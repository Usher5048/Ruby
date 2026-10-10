package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
 * Meteor's Flight modes:
 * - Abilities: Sets creative-style flight (allowFlying + flying)
 * - Velocity: Manually controls Y velocity based on input keys
 * Anti-kick: Periodically sends small downward movement to avoid server kick.
 */
public class Flight extends Module {
    public enum Mode { Abilities, Velocity }

    private final EnumValue<Mode> mode;
    private final DoubleValue speed;
    private final BooleanValue antiKick;

    private int antiKickTimer = 0;

    public Flight() {
        super("Flight", "Lets you fly.", ModuleType.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The flight mode.")
                .defaultValue(Mode.Abilities)
                .build());

        speed = config.create(new DoubleValue.Builder("Speed")
                .description("Flight speed.")
                .defaultValue(1.0).min(0.1).max(10.0).step(0.1)
                .build());

        antiKick = config.create(new BooleanValue.Builder("Anti Kick")
                .description("Attempts to prevent being kicked for flying.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mode.value() == Mode.Abilities) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed(speed.value().floatValue() / 10f);
            mc.player.onUpdateAbilities();
        }
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        switch (mode.value()) {
            case Abilities -> {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.getAbilities().setFlyingSpeed(speed.value().floatValue() / 10f);
            }
            case Velocity -> {
                Vec3 velocity = player.getDeltaMovement();
                double y = 0;

                if (mc.options.keyJump.isDown()) y += speed.value() * 0.05;
                if (mc.options.keyShift.isDown()) y -= speed.value() * 0.05;

                // Horizontal speed boost
                double forward = player.zza;
                double strafe = player.xxa;
                float yaw = player.getYRot();

                double mx = 0, mz = 0;
                if (forward != 0 || strafe != 0) {
                    double angle = Math.toRadians(yaw);
                    mx = (-Math.sin(angle) * forward + Math.cos(angle) * strafe) * speed.value() * 0.05;
                    mz = (Math.cos(angle) * forward + Math.sin(angle) * strafe) * speed.value() * 0.05;
                }

                player.setDeltaMovement(mx, y, mz);
            }
        }

        // Anti-kick: send small downward position every 40 ticks
        if (antiKick.value() && !player.onGround()) {
            antiKickTimer++;
            if (antiKickTimer >= 40) {
                antiKickTimer = 0;
                player.setDeltaMovement(player.getDeltaMovement().x, -0.04, player.getDeltaMovement().z);
            }
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getAbilities().mayfly = false;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlyingSpeed(0.05f); // Default fly speed
        mc.player.onUpdateAbilities();
        antiKickTimer = 0;
    }

    @Override
    public String getInfoString() {
        return this.mode.value().name();
    }
}
