package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Speed module with Vanilla and Strafe modes.
 * - Vanilla: Overrides horizontal velocity based on configurable blocks/sec.
 * - Strafe: NCP-style stage-based speed with jump boost.
 */
public class Speed extends Module {

    public enum SpeedMode { Vanilla, Strafe }

    private final EnumValue<SpeedMode> mode;
    private final DoubleValue vanillaSpeed;
    private final DoubleValue strafeSpeed;
    private final BooleanValue strafeSpeedLimit;
    private final BooleanValue inLiquids;
    private final BooleanValue whenSneaking;
    private final BooleanValue vanillaOnGround;

    // Strafe state
    private int stage;
    private double distance, speed;

    public Speed() {
        super("Speed", "Modifies your movement speed.", ModuleType.MOVEMENT);

        mode = config.create(new EnumValue.Builder<SpeedMode>("Mode")
                .description("The method of applying speed.")
                .defaultValue(SpeedMode.Vanilla)
                .build());

        vanillaSpeed = config.create(new DoubleValue.Builder("Vanilla Speed")
                .description("The speed in blocks per second.")
                .defaultValue(5.6).min(0).max(20).step(0.1)
                .build());

        strafeSpeed = config.create(new DoubleValue.Builder("Strafe Speed")
                .description("The strafe speed multiplier.")
                .defaultValue(1.6).min(0).max(3).step(0.1)
                .build());

        strafeSpeedLimit = config.create(new BooleanValue.Builder("Speed Limit")
                .description("Limits your speed on servers with strict anticheats.")
                .defaultValue(false)
                .build());

        inLiquids = config.create(new BooleanValue.Builder("In Liquids")
                .description("Uses speed when in lava or water.")
                .defaultValue(false)
                .build());

        whenSneaking = config.create(new BooleanValue.Builder("When Sneaking")
                .description("Uses speed when sneaking.")
                .defaultValue(false)
                .build());

        vanillaOnGround = config.create(new BooleanValue.Builder("Only On Ground")
                .description("Uses vanilla speed only when on the ground.")
                .defaultValue(false)
                .build());
    }

    @Override
    public void onEnable() {
        stage = 0;
        distance = 0;
        speed = 0.2873;
    }

    @Override
    public void onDisable() {
        stage = 0;
        distance = 0;
        speed = 0.2873;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (shouldStop(mc, player)) return;

        switch (mode.value()) {
            case Vanilla -> tickVanilla(mc, player);
            case Strafe -> tickStrafe(mc, player);
        }
    }

    private void tickVanilla(Minecraft mc, LocalPlayer player) {
        if (vanillaOnGround.value() && !player.onGround()) return;
        if (!isMoving(player)) return;

        double bps = vanillaSpeed.value();

        float yaw = player.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
        double velX = 0, velZ = 0;

        boolean fwd = false;
        if (player.input.keyPresses.forward()) {
            velX += forward.x / 20 * bps;
            velZ += forward.z / 20 * bps;
            fwd = true;
        }
        if (player.input.keyPresses.backward()) {
            velX -= forward.x / 20 * bps;
            velZ -= forward.z / 20 * bps;
            fwd = true;
        }

        boolean side = false;
        if (player.input.keyPresses.right()) {
            velX += right.x / 20 * bps;
            velZ += right.z / 20 * bps;
            side = true;
        }
        if (player.input.keyPresses.left()) {
            velX -= right.x / 20 * bps;
            velZ -= right.z / 20 * bps;
            side = true;
        }

        // Diagonal normalization
        if (fwd && side) {
            double diagonal = 1.0 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }

        // Apply speed effect boost
        if (player.hasEffect(MobEffects.SPEED)) {
            double value = (player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.205;
            velX += velX * value;
            velZ += velZ * value;
        }

        player.setDeltaMovement(velX, player.getDeltaMovement().y, velZ);
    }

    private void tickStrafe(Minecraft mc, LocalPlayer player) {
        // Update distance traveled
        distance = Math.sqrt(
                (player.getX() - player.xo) * (player.getX() - player.xo) +
                (player.getZ() - player.zo) * (player.getZ() - player.zo)
        );

        if (!isMoving(player)) {
            stage = 0;
            return;
        }

        double defaultSpeed = getDefaultSpeed(player);

        switch (stage) {
            case 0 -> { // Reset
                speed = 1.18 * defaultSpeed - 0.01;
                stage++;
            }
            case 1 -> { // Jump
                if (!player.onGround()) break;
                player.setDeltaMovement(player.getDeltaMovement().x, getHop(player, 0.40123128), player.getDeltaMovement().z);
                speed *= strafeSpeed.value();
                stage++;
            }
            case 2 -> { // Slowdown after jump
                speed = distance - 0.76 * (distance - defaultSpeed);
                stage++;
            }
            default -> { // Predict and update
                if (player.verticalCollision && stage > 0) {
                    stage = 0;
                }
                speed = distance - (distance / 159.0);
            }
        }

        speed = Math.max(speed, defaultSpeed);

        if (strafeSpeedLimit.value()) {
            speed = Math.min(speed, 0.44);
        }

        // Apply strafe movement
        float forward = Math.signum(player.zza);
        float sideways = Math.signum(player.xxa);
        float yaw = player.getYRot();

        if (forward == 0 && sideways == 0) return;

        float strafe = 90 * sideways;
        if (forward != 0) strafe *= forward * 0.5f;

        yaw = yaw - strafe;
        if (forward < 0) yaw -= 180;

        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad) * speed;
        double moveZ = Math.cos(yawRad) * speed;

        player.setDeltaMovement(moveX, player.getDeltaMovement().y, moveZ);
    }

    private double getDefaultSpeed(LocalPlayer player) {
        double defaultSpeed = 0.2873;
        if (player.hasEffect(MobEffects.SPEED)) {
            int amp = player.getEffect(MobEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amp + 1);
        }
        if (player.hasEffect(MobEffects.SLOWNESS)) {
            int amp = player.getEffect(MobEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amp + 1);
        }
        return defaultSpeed;
    }

    private double getHop(LocalPlayer player, double height) {
        if (player.hasEffect(MobEffects.JUMP_BOOST)) {
            height += (player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1;
        }
        return height;
    }

    private boolean isMoving(LocalPlayer player) {
        return player.zza != 0 || player.xxa != 0;
    }

    private boolean shouldStop(Minecraft mc, LocalPlayer player) {
        if (player.isFallFlying() || player.onClimbable() || player.getVehicle() != null) return true;
        if (!whenSneaking.value() && player.isShiftKeyDown()) return true;
        return !inLiquids.value() && (player.isInWater() || player.isInLava());
    }

    @Override
    public String getInfoString() {
        return this.mode.value().name();
    }
}
