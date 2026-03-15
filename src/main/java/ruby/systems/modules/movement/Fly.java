package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Meteor's Flight modes:
 * - Abilities: Sets creative-style flight (allowFlying + flying)
 * - Velocity: Manually controls Y velocity based on input keys
 * Anti-kick: Periodically sends small downward movement to avoid server kick.
 */
public class Fly extends Module {
    public enum Mode { Abilities, Velocity }

    private final EnumValue<Mode> mode;
    private final DoubleValue speed;
    private final BooleanValue antiKick;

    private int antiKickTimer = 0;

    public Fly() {
        super("Fly", "Lets you fly.", ModuleCategory.MOVEMENT);

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mode.value() == Mode.Abilities) {
            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(speed.value().floatValue() / 10f);
            mc.player.sendAbilitiesUpdate();
        }
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        switch (mode.value()) {
            case Abilities -> {
                player.getAbilities().allowFlying = true;
                player.getAbilities().flying = true;
                player.getAbilities().setFlySpeed(speed.value().floatValue() / 10f);
            }
            case Velocity -> {
                Vec3d velocity = player.getVelocity();
                double y = 0;

                if (mc.options.jumpKey.isPressed()) y += speed.value() * 0.05;
                if (mc.options.sneakKey.isPressed()) y -= speed.value() * 0.05;

                // Horizontal speed boost
                double forward = player.forwardSpeed;
                double strafe = player.sidewaysSpeed;
                float yaw = player.getYaw();

                double mx = 0, mz = 0;
                if (forward != 0 || strafe != 0) {
                    double angle = Math.toRadians(yaw);
                    mx = (-Math.sin(angle) * forward + Math.cos(angle) * strafe) * speed.value() * 0.05;
                    mz = (Math.cos(angle) * forward + Math.sin(angle) * strafe) * speed.value() * 0.05;
                }

                player.setVelocity(mx, y, mz);
            }
        }

        // Anti-kick: send small downward position every 40 ticks
        if (antiKick.value() && !player.isOnGround()) {
            antiKickTimer++;
            if (antiKickTimer >= 40) {
                antiKickTimer = 0;
                player.setVelocity(player.getVelocity().x, -0.04, player.getVelocity().z);
            }
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.getAbilities().allowFlying = false;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(0.05f); // Default fly speed
        mc.player.sendAbilitiesUpdate();
        antiKickTimer = 0;
    }
}
