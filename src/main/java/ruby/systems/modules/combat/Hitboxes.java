package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.config.EnumValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class Hitboxes extends Module {
    public enum AimPoint { Closest, Center, Head }

    public static Hitboxes INSTANCE;

    // ── Config ────────────────────────────────────────────────────────────────
    private final EntityTypeListValue entities;
    private final DoubleValue expand;
    private final BooleanValue onlyOnWeapon;
    private final BooleanValue sword, axe, pickaxe, shovel, hoe, mace, spear, trident;
    private final BooleanValue silentAim;
    private final EnumValue<AimPoint> aimPoint;
    private final DoubleValue maxRotateDeg;

    // ── Silent aim state ──────────────────────────────────────────────────────
    //
    // NEW ARCHITECTURE — fabricate packets, never intercept:
    //
    // The mixin completely replaces sendMovementPackets via @Inject+cancel.
    // It reads the same vanilla state (lastX/Y/Z, lastYaw, lastPitch, etc.),
    // decides what packet(s) vanilla would have sent, then calls back here
    // so we can swap in our spoofed yaw/pitch before the packet is built and
    // handed to networkHandler.sendPacket().
    //
    // Because WE build the packet from scratch using real position values and
    // only a different yaw/pitch, there is:
    //   - No double packet (no Timer flag)
    //   - No duplicate look (AimDuplicateLook only gets one packet with one rotation)
    //   - No simulation mismatch when player is still (LookAndOnGround has no pos)
    //   - No GroundSpoof (we take onGround from the real player state)
    //
    // When the player is MOVING we emit the real yaw, not the spoof, to keep
    // Grim's simulation consistent. The spoof is held and re-applied the next
    // tick the player sends a LookAndOnGround (i.e. stops moving).
    //
    // Attack latch: onBeforeAttack() stores desired aim. The mixin calls
    // buildOutputYawPitch() which promotes the latch to the active spoof that
    // same tick, so the packet carrying the attack's tick also carries the aim.

    // Active spoof rotation — what the server currently has / should get.
    private boolean spoofing;
    private float   spoofYaw;
    private float   spoofPitch;

    // Per-tick latch set by onBeforeAttack().
    private boolean attackedThisTick;
    private float   attackSpoofYaw;
    private float   attackSpoofPitch;

    // Mouse-release detection.
    private float prevClientYaw;
    private float prevClientPitch;
    private static final float MOUSE_RELEASE_DEG = 4.0f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Hitboxes() {
        super("Hitboxes", "Expands target hitboxes for selected entity types.", ModuleType.COMBAT);
        INSTANCE = this;

        entities = config.create(new EntityTypeListValue.Builder("Entities")
                .description("Which entity types to expand.")
                .defaultValue(EntityType.PLAYER)
                .build());
        expand = config.create(new DoubleValue.Builder("Expand")
                .description("How much to expand each selected entity hitbox.")
                .defaultValue(0.5).min(0.0).max(2.0).step(0.05)
                .build());
        onlyOnWeapon = config.create(new BooleanValue.Builder("Only On Weapon")
                .defaultValue(false).build());
        sword   = config.create(new BooleanValue.Builder("Sword")   .defaultValue(true).visible(onlyOnWeapon::value).build());
        axe     = config.create(new BooleanValue.Builder("Axe")     .defaultValue(true).visible(onlyOnWeapon::value).build());
        pickaxe = config.create(new BooleanValue.Builder("Pickaxe") .defaultValue(true).visible(onlyOnWeapon::value).build());
        shovel  = config.create(new BooleanValue.Builder("Shovel")  .defaultValue(true).visible(onlyOnWeapon::value).build());
        hoe     = config.create(new BooleanValue.Builder("Hoe")     .defaultValue(true).visible(onlyOnWeapon::value).build());
        mace    = config.create(new BooleanValue.Builder("Mace")    .defaultValue(true).visible(onlyOnWeapon::value).build());
        spear   = config.create(new BooleanValue.Builder("Spear")   .defaultValue(true).visible(onlyOnWeapon::value).build());
        trident = config.create(new BooleanValue.Builder("Trident") .defaultValue(true).visible(onlyOnWeapon::value).build());
        silentAim = config.create(new BooleanValue.Builder("Silent Aim")
                .defaultValue(false).build());
        aimPoint = config.create(new EnumValue.Builder<AimPoint>("Aim Point")
                .defaultValue(AimPoint.Closest).visible(silentAim::value).build());
        maxRotateDeg = config.create(new DoubleValue.Builder("Max Rotate Delta")
                .defaultValue(35.0).min(1.0).max(180.0).step(1.0)
                .visible(silentAim::value).build());

        Events.ENTITY.register(EntityEvents.ATTACK, e ->
                this.latchAttackRotation(RubyClient.client.player, e.entity())
        );
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        spoofing         = false;
        attackedThisTick = false;
        syncPrevFromPlayer();
    }

    @Override
    public void onDisable() {
        spoofing         = false;
        attackedThisTick = false;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static double getEntityValue(Entity entity) {
        if (INSTANCE == null) return 0;
        return INSTANCE.getEntityValueInternal(entity);
    }

    /**
     * Called before the attack packet is sent.
     * Latches the desired rotation — no packet is sent here.
     * The latch is consumed inside buildOutputYawPitch() when the mixin
     * fabricates the movement packet for this tick.
     */
    public static void onBeforeAttack(PlayerEntity player, Entity target) {
        if (INSTANCE == null) return;
        INSTANCE.latchAttackRotation(player, target);
    }

    /**
     * Called by ClientPlayerEntityMixin from inside the fabricated
     * sendMovementPackets replacement.
     * <p>
     * Given the vanilla-computed yaw and pitch that WOULD go out, returns the
     * yaw/pitch that SHOULD go out (either spoofed or unchanged).
     *
     * @param vanillaYaw   the yaw vanilla would have put in the packet
     * @param vanillaPitch the pitch vanilla would have put in the packet
     * @param isMoving     true when a Full (pos+look) packet is being built
     * @return float[2] = { outputYaw, outputPitch }
     */
    public static float[] buildOutputYawPitch(float vanillaYaw, float vanillaPitch, boolean isMoving) {
        if (INSTANCE == null) return new float[]{ vanillaYaw, vanillaPitch };
        return INSTANCE.resolveOutputRotation(vanillaYaw, vanillaPitch, isMoving);
    }

    /** True only when silent aim needs to rewrite outgoing movement packets. */
    public static boolean shouldOverrideMovementPackets() {
        return INSTANCE != null && INSTANCE.enabled() && INSTANCE.silentAim.value();
    }

    // ── Internal logic ────────────────────────────────────────────────────────

    private double getEntityValueInternal(Entity entity) {
        if (!enabled()) return 0;
        if (!entities.value().contains(entity.getType())) return 0;
        if (!passesWeaponCheck()) return 0;
        return expand.value();
    }

    private boolean passesWeaponCheck() {
        if (!onlyOnWeapon.value()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        if (sword.value()   && stack.isIn(ItemTags.SWORDS))            return true;
        if (axe.value()     && stack.isIn(ItemTags.AXES))              return true;
        if (pickaxe.value() && stack.isIn(ItemTags.PICKAXES))          return true;
        if (shovel.value()  && stack.isIn(ItemTags.SHOVELS))           return true;
        if (hoe.value()     && stack.isIn(ItemTags.HOES))              return true;
        if (mace.value()    && stack.getItem() instanceof MaceItem)    return true;
        if (spear.value()   && stack.isIn(ItemTags.SPEARS))            return true;
        return trident.value() && stack.getItem() instanceof TridentItem;
    }

    private void latchAttackRotation(PlayerEntity player, Entity target) {
        if (!enabled() || !silentAim.value()) return;
        if (!(target instanceof LivingEntity living) || living.isDead()) return;

        float baseYaw   = spoofing ? spoofYaw   : player.getYaw();
        float basePitch = spoofing ? spoofPitch : player.getPitch();

        float[] ideal = computeIdealRotation(player.getEyePos(), target);
        float   max   = maxRotateDeg.value().floatValue();

        float dYaw   = MathHelper.clamp(MathHelper.wrapDegrees(ideal[0] - baseYaw),   -max, max);
        float dPitch = MathHelper.clamp(ideal[1] - basePitch, -max, max);

        attackSpoofYaw   = baseYaw + dYaw;
        attackSpoofPitch = MathHelper.clamp(basePitch + dPitch, -90f, 90f);
        attackedThisTick = true;
    }

    /**
     * Core rotation resolver called by the mixin during packet fabrication.
     *
     * @param vanillaYaw   yaw vanilla computed for this tick
     * @param vanillaPitch pitch vanilla computed for this tick
     * @param isMoving     true if the packet being built is a Full (pos+look)
     */
    private float[] resolveOutputRotation(float vanillaYaw, float vanillaPitch, boolean isMoving) {
        if (!enabled() || !silentAim.value()) {
            spoofing         = false;
            attackedThisTick = false;
            return new float[]{ vanillaYaw, vanillaPitch };
        }

        // Promote attack latch to active spoof.
        if (attackedThisTick) {
            attackedThisTick = false;
            spoofing         = true;
            spoofYaw         = attackSpoofYaw;
            spoofPitch       = attackSpoofPitch;
            prevClientYaw    = vanillaYaw;
            prevClientPitch  = vanillaPitch;
        }

        if (!spoofing) {
            return new float[]{ vanillaYaw, vanillaPitch };
        }

        if (isMoving) {
            // Full packet — player is changing position this tick.
            // We MUST send the real yaw here or Grim's simulation will flag us.
            // Hold the spoof; it will be applied next tick when player is still.
            return new float[]{ vanillaYaw, vanillaPitch };
        }

        // LookAndOnGround — no position delta. Safe to spoof freely.
        return new float[]{ spoofYaw, spoofPitch };
    }

    @Override
    public void tick() {
        if (!enabled() || !silentAim.value()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float clientYaw   = mc.player.getYaw();
        float clientPitch = mc.player.getPitch();

        if (spoofing) {
            float dYaw   = Math.abs(MathHelper.wrapDegrees(clientYaw - prevClientYaw));
            float dPitch = Math.abs(clientPitch - prevClientPitch);
            if (dYaw > MOUSE_RELEASE_DEG || dPitch > MOUSE_RELEASE_DEG) {
                spoofing = false;
            }
        }

        prevClientYaw   = clientYaw;
        prevClientPitch = clientPitch;

        // Defensive clear — if no packet was sent this tick, drop the latch.
        attackedThisTick = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float[] computeIdealRotation(Vec3d eyes, Entity target) {
        Box box = target.getBoundingBox();
        Vec3d aim = switch (aimPoint.value()) {
            case Center  -> box.getCenter();
            case Head    -> new Vec3d(box.getCenter().x, box.maxY - 0.1, box.getCenter().z);
            case Closest -> closestPoint(eyes, box);
        };
        double dx = aim.x - eyes.x;
        double dy = aim.y - eyes.y;
        double dz = aim.z - eyes.z;
        double h  = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = MathHelper.clamp((float) -Math.toDegrees(Math.atan2(dy, h)), -90f, 90f);
        return new float[]{ yaw, pitch };
    }

    private static Vec3d closestPoint(Vec3d from, Box box) {
        return new Vec3d(
                MathHelper.clamp(from.x, box.minX, box.maxX),
                MathHelper.clamp(from.y, box.minY, box.maxY),
                MathHelper.clamp(from.z, box.minZ, box.maxZ));
    }

    private void syncPrevFromPlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        prevClientYaw   = mc.player.getYaw();
        prevClientPitch = mc.player.getPitch();
    }

    private static String f(float v) { return String.format("%.2f", v); }
}