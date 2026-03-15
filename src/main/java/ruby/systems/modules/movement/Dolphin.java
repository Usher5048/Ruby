package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Allows the player to swim faster in water, like a dolphin.
 * Boosts horizontal velocity when submerged and moving.
 */
public class Dolphin extends Module {

    private final DoubleValue speed;

    public Dolphin() {
        super("Dolphin", "Allows you to swim faster in water.", ModuleCategory.MOVEMENT);

        speed = config.create(new DoubleValue.Builder("Speed")
                .description("Swim speed multiplier.")
                .defaultValue(3.0).min(1.0).max(10.0).step(0.1)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!player.isTouchingWater() && !player.isSubmergedInWater()) return;

        Vec3d velocity = player.getVelocity();

        // Boost horizontal velocity while in water
        double factor = speed.value() / 20.0;
        float yaw = player.getYaw();
        double forward = player.forwardSpeed;
        double sideways = player.sidewaysSpeed;

        if (forward == 0 && sideways == 0) return;

        double angle = Math.toRadians(yaw);
        double mx = (-Math.sin(angle) * forward + Math.cos(angle) * sideways) * factor;
        double mz = (Math.cos(angle) * forward + Math.sin(angle) * sideways) * factor;

        // Vertical control
        double my = velocity.y;
        if (mc.options.jumpKey.isPressed()) my = factor;
        else if (mc.options.sneakKey.isPressed()) my = -factor;

        player.setVelocity(mx, my, mz);
    }
}
