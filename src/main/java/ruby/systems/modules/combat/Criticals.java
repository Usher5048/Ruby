package ruby.systems.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import ruby.RubyClient;
import ruby.helpers.RotationRaycast;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

public class Criticals extends Module {
    public enum Mode { None, Jump, Packet }
    public enum StopSprintMode { None, Legit, OnNetwork, OnAttack }

    private boolean adjustNextJump;

    public final EnumValue<Mode> mode = this.config.create(new EnumValue.Builder<Mode>("Mode")
            .description("Critical hit mode.")
            .defaultValue(Mode.Jump)
            .build());

    public final DoubleValue jumpHeight = this.config.create(new DoubleValue.Builder("Jump Height")
            .description("Upward velocity used for jump crits.")
            .range(0.1, 0.42, 0.01)
            .defaultValue(0.42)
            .build());

    public final DoubleValue range = this.config.create(new DoubleValue.Builder("Range")
            .description("Range to begin jump crit setup.")
            .range(1, 6, 0.1)
            .defaultValue(4.5)
            .build());

    public final BooleanValue optimizeForCooldown = this.config.create(new BooleanValue.Builder("Optimize Cooldown")
            .description("Wait for attack cooldown before jumping.")
            .defaultValue(true)
            .build());

    public final BooleanValue checkKillAura = this.config.create(new BooleanValue.Builder("Check KillAura")
            .description("Only run when KillAura is enabled.")
            .defaultValue(true)
            .build());

    public final BooleanValue canBeSeen = this.config.create(new BooleanValue.Builder("Can Be Seen")
            .description("Only jump when the target is visible.")
            .defaultValue(true)
            .build());

    public final BooleanValue whenSprinting = this.config.create(new BooleanValue.Builder("When Sprinting")
            .description("Apply extra sprint rules while attempting crits.")
            .defaultValue(true)
            .build());

    public final EnumValue<StopSprintMode> stopSprinting = this.config.create(new EnumValue.Builder<StopSprintMode>("Stop Sprinting")
            .description("How to stop sprint for crits.")
            .defaultValue(StopSprintMode.Legit)
            .build());

    public final DoubleValue sprintRange = this.config.create(new DoubleValue.Builder("Sprint Range")
            .description("Range used by When Sprinting.")
            .range(0, 10, 0.5)
            .defaultValue(4.0)
            .build());

    public Criticals() {
        super("Criticals", "Automatically performs critical hits.", ModuleType.COMBAT);
    }

    @Override
    public void onDisable() {
        this.adjustNextJump = false;
    }

    public static boolean shouldForceJump() {
        Criticals criticals = Modules.getByClass(Criticals.class);
        if(criticals == null || !criticals.enabled() || criticals.mode.value() != Mode.Jump) return false;
        if(RubyClient.client.player == null || RubyClient.client.world == null) return false;
        if(RubyClient.client.currentScreen != null) return false;
        if(!criticals.isJumpModeActive()) return false;
        if(!allowsCriticalHit(true)) return false;
        if(criticals.optimizeForCooldown.value() && criticals.shouldWaitForJump()) return false;

        ClientPlayerEntity player = RubyClient.client.player;
        if(!player.isOnGround()) return false;
        if(!criticals.hasEnemyInRange(criticals.range.value())) return false;

        criticals.adjustNextJump = true;
        return true;
    }

    public void onPlayerJump(float motionY) {
        if(!this.adjustNextJump || Math.abs(motionY - 0.42f) > 0.001f) return;
        if(RubyClient.client.player == null) return;

        RubyClient.client.player.setVelocity(
                RubyClient.client.player.getVelocity().x,
                this.jumpHeight.value(),
                RubyClient.client.player.getVelocity().z
        );
        this.adjustNextJump = false;
    }

    public static boolean allowsCriticalHit(boolean ignoreOnGround) {
        if(RubyClient.client.player == null) return false;
        ClientPlayerEntity player = RubyClient.client.player;

        if(player.isTouchingWater() || player.isInLava() || player.hasVehicle()) return false;
        if(player.isClimbing() || player.hasNoGravity()) return false;
        if(player.getAbilities().flying) return false;
        if(player.isGliding()) return false;
        if(player.getBlockStateAtPos().isOf(Blocks.COBWEB)) return false;
        if(!ignoreOnGround && player.isOnGround()) return false;
        return true;
    }

    public static boolean wouldDoCriticalHit() {
        return wouldDoCriticalHit(false);
    }

    public static boolean wouldDoCriticalHit(boolean ignoreSprint) {
        if(RubyClient.client.player == null) return false;
        ClientPlayerEntity player = RubyClient.client.player;
        return allowsCriticalHit(false)
                && player.getAttackCooldownProgress(0.5f) > 0.9f
                && (ignoreSprint || !player.isSprinting())
                && player.fallDistance > 0.0f;
    }

    public static boolean shouldWaitForCrit(Entity target, boolean ignoreState) {
        Criticals criticals = Modules.getByClass(Criticals.class);
        if(criticals == null || criticals.mode.value() != Mode.Jump) return false;
        if(!ignoreState && (!criticals.enabled() || !criticals.isJumpModeActive())) return false;
        if(RubyClient.client.player == null || target == null) return false;

        ClientPlayerEntity player = RubyClient.client.player;
        if(player.isGliding()) return false;
        if(!allowsCriticalHit(false)) return false;
        if(player.getVelocity().y < -0.08) return false;
        if(wouldDoCriticalHit(false)) return false;

        float nextPossibleCrit = calculateTicksUntilNextCrit(player);
        float ticksTillFall = Math.max(0.0f, (float) (player.getVelocity().y / 0.08));
        float ticksTillCrit = Math.max(nextPossibleCrit, ticksTillFall);

        float damageOnCrit = 0.5f * 0.75f;
        if(damageOnCrit <= getCooldownDamageFactor(player, ticksTillCrit)) return false;

        if(player.isOnGround() && criticals.hasEnemyInRange(criticals.range.value())) return true;
        return !player.isOnGround() && player.fallDistance <= 0.0f && player.getVelocity().y >= -0.08;
    }

    public static boolean isCriticalHitAllowed(KillAura.CriticalsMode mode, Entity target) {
        Criticals criticals = Modules.getByClass(Criticals.class);
        return switch(mode) {
            case Ignore -> true;
            case Always -> wouldDoCriticalHit();
            case Smart -> criticals == null || !criticals.enabled() || criticals.mode.value() != Mode.Jump
                    || !shouldWaitForCrit(target, true);
        };
    }

    public static boolean blocksSprintInput() {
        Criticals criticals = Modules.getByClass(Criticals.class);
        if(criticals == null || !criticals.whenSprintingActive()) return false;
        return criticals.stopSprinting.value() == StopSprintMode.Legit
                || criticals.stopSprinting.value() == StopSprintMode.OnNetwork;
    }

    public static boolean blocksNetworkSprint() {
        return blocksSprintInput();
    }

    public static void onAttack() {
        Criticals criticals = Modules.getByClass(Criticals.class);
        if(criticals == null || !criticals.enabled() || !criticals.whenSprinting.value()) return;
        if(criticals.stopSprinting.value() != StopSprintMode.OnAttack) return;
        if(RubyClient.client.player != null) RubyClient.client.player.setSprinting(false);
    }

    private boolean whenSprintingActive() {
        return this.enabled()
                && this.whenSprinting.value()
                && wouldDoCriticalHit(true)
                && this.hasEnemyInRange(this.sprintRange.value());
    }

    private boolean isJumpModeActive() {
        if(!this.checkKillAura.value()) return true;
        KillAura killAura = Modules.getByClass(KillAura.class);
        return killAura != null && killAura.enabled();
    }

    private boolean hasEnemyInRange(double range) {
        if(RubyClient.client.player == null || RubyClient.client.world == null) return false;

        KillAura killAura = Modules.getByClass(KillAura.class);
        Entity target = killAura != null ? killAura.currentTarget() : null;
        if(target != null && target.isAlive()) {
            if(RotationRaycast.squaredBoxedDistanceTo(target) <= range * range) {
                if(!this.canBeSeen.value() || RubyClient.client.player.canSee(target)) return true;
            }
        }

        Box box = RubyClient.client.player.getBoundingBox().expand(range);
        for(Entity entity : RubyClient.client.world.getOtherEntities(RubyClient.client.player, box, e -> e instanceof LivingEntity living && living.isAlive())) {
            if(RotationRaycast.squaredBoxedDistanceTo(entity) > range * range) continue;
            if(this.canBeSeen.value() && !RubyClient.client.player.canSee(entity)) continue;
            return true;
        }
        return false;
    }

    private boolean shouldWaitForJump() {
        if(RubyClient.client.player == null || !allowsCriticalHit(true) || !this.enabled()) return false;

        float initialMotion = this.jumpHeight.value().floatValue();
        float ticksTillFall = initialMotion / 0.08f;
        float nextPossibleCrit = calculateTicksUntilNextCrit(RubyClient.client.player);
        float ticksTillNextOnGround = ticksTillFall * 2.0f;

        if(ticksTillNextOnGround + ticksTillFall < nextPossibleCrit) return false;
        return ticksTillFall + 1.0f < nextPossibleCrit;
    }

    private static float calculateTicksUntilNextCrit(ClientPlayerEntity player) {
        float progress = player.getAttackCooldownProgress(0.0f);
        if(progress >= 1.0f) return 0.0f;
        return (1.0f - progress) * attackCooldownTicks(player);
    }

    private static float attackCooldownTicks(ClientPlayerEntity player) {
        float progress = player.getAttackCooldownProgress(0.0f);
        if(progress <= 0.0f) return 20.0f;
        return Math.max(1.0f, 1.0f / progress);
    }

    private static float getCooldownDamageFactor(ClientPlayerEntity player, float tickDelta) {
        float delay = attackCooldownTicks(player);
        float base = (tickDelta + 0.5f) / delay;
        return Math.min(1.0f, 0.2f + base * base * 0.8f);
    }

    @Override
    public String getInfoString() {
        return this.mode.value().name();
    }
}
