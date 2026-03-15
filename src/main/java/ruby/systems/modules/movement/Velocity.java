package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Velocity: modifies knockback from EntityVelocityUpdateS2CPacket via mixin.
 * Tick-based adaptation: detects the first tick of being hurt (hurtTime == maxHurtTime)
 * and multiplies the player's velocity by the configured horizontal/vertical factors.
 */
public class Velocity extends Module {
    private final DoubleValue horizontal;
    private final DoubleValue vertical;

    public Velocity() {
        super("Velocity", "Reduces the amount of knockback you take.", ModuleType.MOVEMENT);

        horizontal = config.create(new DoubleValue.Builder("Horizontal")
                .description("Horizontal knockback multiplier.")
                .defaultValue(0.0).min(0).max(1.0).step(0.01)
                .build());

        vertical = config.create(new DoubleValue.Builder("Vertical")
                .description("Vertical knockback multiplier.")
                .defaultValue(0.0).min(0).max(1.0).step(0.01)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Detect first tick of being hurt (knockback is applied this tick)
        if (player.hurtTime == player.maxHurtTime && player.maxHurtTime > 0) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(
                    vel.x * horizontal.value(),
                    vel.y * vertical.value(),
                    vel.z * horizontal.value()
            );
        }
    }
}
