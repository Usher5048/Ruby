package ruby.systems.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import ruby.RubyClient;
import ruby.helpers.ClickScheduler;
import ruby.helpers.RotationManager;
import ruby.helpers.RotationRaycast;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

public class KillAura extends Module {
    public enum Priority { Health, Distance, Angle }
    public enum CriticalsMode { Smart, Ignore, Always }
    public enum RaycastMode { None, All, OnlyEnemy }

    private Entity currentTarget;
    private final ClickScheduler clicker = new ClickScheduler(15, 16);

    public final DoubleValue range = this.config.create(new DoubleValue.Builder("Range")
            .description("Maximum attack distance.")
            .range(2.5, 6.0, 0.05)
            .defaultValue(3.0)
            .build());

    public final DoubleValue scanRange = this.config.create(new DoubleValue.Builder("Scan Range")
            .description("Extra distance used to acquire targets.")
            .range(0, 2, 0.1)
            .defaultValue(1.0)
            .build());

    public final DoubleValue wallRange = this.config.create(new DoubleValue.Builder("Wall Range")
            .description("Attack range through walls.")
            .range(0, 3, 0.1)
            .defaultValue(0.0)
            .build());

    public final DoubleValue fov = this.config.create(new DoubleValue.Builder("FOV")
            .description("Field of view used to pick targets.")
            .range(30, 180, 5)
            .defaultValue(180.0)
            .build());

    public final IntegerValue hurtTime = this.config.create(new IntegerValue.Builder("Hurt Time")
            .description("Maximum hurt time a target may have.")
            .range(0, 10)
            .defaultValue(10)
            .build());

    public final EnumValue<Priority> priority = this.config.create(new EnumValue.Builder<Priority>("Priority")
            .description("Which target to prefer.")
            .defaultValue(Priority.Health)
            .build());

    public final BooleanValue ignoreShield = this.config.create(new BooleanValue.Builder("Ignore Shield")
            .description("Attack players who are blocking.")
            .defaultValue(true)
            .build());

    public final BooleanValue attackCooldown = this.config.create(new BooleanValue.Builder("Attack Cooldown")
            .description("Wait for the weapon attack cooldown.")
            .defaultValue(true)
            .build());

    public final EnumValue<CriticalsMode> criticals = this.config.create(new EnumValue.Builder<CriticalsMode>("Criticals")
            .description("How KillAura coordinates with the Criticals module.")
            .defaultValue(CriticalsMode.Smart)
            .build());

    public final BooleanValue keepSprint = this.config.create(new BooleanValue.Builder("Keep Sprint")
            .description("Keep sprinting after hitting.")
            .defaultValue(false)
            .build());

    public final EnumValue<RaycastMode> raycast = this.config.create(new EnumValue.Builder<RaycastMode>("Raycast")
            .description("Which entity raycast validation to use.")
            .defaultValue(RaycastMode.All)
            .build());

    public final EntityTypeListValue targets = this.config.create(new EntityTypeListValue.Builder("Targets")
            .description("Which types of entities to target.")
            .defaultValue(EntityType.PLAYER)
            .build());

    public KillAura() {
        super("KillAura", "Automatically attacks entities.", ModuleType.COMBAT);
    }

    public Entity currentTarget() {
        return this.currentTarget;
    }

    public boolean shouldStopSprinting(Entity target) {
        if(this.criticals.value() == CriticalsMode.Ignore) return false;
        if(RubyClient.client.player == null || RubyClient.client.player.isOnGround()) return false;
        return target != null && this.clicker.willClickAt(1);
    }

    @Override
    public void onDisable() {
        this.currentTarget = null;
        this.clicker.reset();
        RotationManager.reset();
    }

    public static void tryAttack() {
        KillAura killAura = Modules.getByClass(KillAura.class);
        if(killAura == null || !killAura.enabled()) return;
        killAura.attackTarget();
    }

    private void attackTarget() {
        if(RubyClient.client.player == null || RubyClient.client.world == null) return;
        if(RubyClient.client.currentScreen != null) return;

        Entity target = this.currentTarget;
        if(target == null) target = RotationManager.targetEntity();
        if(target == null || !this.isValidTarget(target)) return;

        if(this.attackCooldown.value()
                && RubyClient.client.player.getAttackCooldownProgress(0.5f) < 1.0f)
            return;

        if(!this.clicker.shouldClick()) return;
        if(!RotationManager.hasRotation()) return;
        if(!RotationRaycast.canHit(
                target,
                RotationManager.rotationYaw(),
                RotationManager.rotationPitch(),
                this.range.value()
        )) return;

        if(!Criticals.isCriticalHitAllowed(this.criticals.value(), target)) return;

        if(!this.keepSprint.value() || this.shouldStopSprinting(target))
            RubyClient.client.player.setSprinting(false);
        Criticals.onAttack();

        RubyClient.client.interactionManager.attackEntity(RubyClient.client.player, target);
        RubyClient.client.player.swingHand(Hand.MAIN_HAND);
    }

    public static void updateRotations() {
        KillAura killAura = Modules.getByClass(KillAura.class);
        if(killAura == null || !killAura.enabled()) {
            RotationManager.reset();
            return;
        }
        if(RubyClient.client.player == null || RubyClient.client.world == null) {
            killAura.currentTarget = null;
            RotationManager.reset();
            return;
        }
        if(RubyClient.client.currentScreen != null) {
            killAura.currentTarget = null;
            RotationManager.clearTarget();
            return;
        }

        killAura.currentTarget = killAura.findTarget();
        if(killAura.currentTarget != null && killAura.processTarget(killAura.currentTarget)) return;
        RotationManager.clearTarget();
    }

    private boolean processTarget(Entity entity) {
        if(!(entity instanceof LivingEntity living)) return false;
        if(!this.isValidTarget(living)) return false;

        double maxRange = this.range.value() + this.scanRange.value();
        if(RotationRaycast.squaredBoxedDistanceTo(entity) > maxRange * maxRange) return false;

        RotationManager.setTarget(entity);
        return true;
    }

    private Entity findTarget() {
        var player = RubyClient.client.player;
        double maxRange = this.range.value() + this.scanRange.value();
        Box box = player.getBoundingBox().expand(maxRange);

        Entity best = null;
        double bestScore = Double.MAX_VALUE;

        for(Entity entity : RubyClient.client.world.getOtherEntities(player, box, this::isValidTarget)) {
            double distSq = RotationRaycast.squaredBoxedDistanceTo(entity);
            if(distSq > maxRange * maxRange) continue;
            if(!RotationRaycast.inFov(entity, this.fov.value().floatValue())) continue;

            double inRangePenalty = distSq <= this.range.value() * this.range.value() ? 0 : 1000;
            double score = inRangePenalty + this.targetPriority(entity);
            if(score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }

        return best;
    }

    private double targetPriority(Entity entity) {
        return switch(this.priority.value()) {
            case Health -> entity instanceof LivingEntity living ? living.getHealth() : 0;
            case Distance -> RotationRaycast.squaredBoxedDistanceTo(entity);
            case Angle -> this.angleToCrosshair(entity);
        };
    }

    private boolean isValidTarget(Entity entity) {
        if(!(entity instanceof LivingEntity living)) return false;
        if(living.isDead()) return false;
        if(living.hurtTime > this.hurtTime.value()) return false;
        if(!this.targets.value().contains(entity.getType())) return false;

        if(!this.ignoreShield.value() && entity instanceof PlayerEntity player)
            return !player.isUsingItem() || !player.getActiveItem().isOf(Items.SHIELD);

        return true;
    }

    private double angleToCrosshair(Entity entity) {
        var player = RubyClient.client.player;
        var look = player.getRotationVector();
        var toTarget = entity.getBoundingBox().getCenter().subtract(player.getEyePos());
        if(toTarget.lengthSquared() < 1.0E-7) return 0;

        double dot = look.normalize().dotProduct(toTarget.normalize());
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }
}
