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
 * Increases climbing speed on ladders and vines by replacing
 * the player's Y velocity while climbing.
 */
public class FastClimb extends Module {

    private final DoubleValue climbSpeed;

    public FastClimb() {
        super("FastClimb", "Allows you to climb ladders and vines faster.", ModuleCategory.MOVEMENT);

        climbSpeed = config.create(new DoubleValue.Builder("Climb Speed")
                .description("Your climb speed.")
                .defaultValue(0.2872).min(0.0).max(1.0).step(0.01)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!player.isClimbing()) return;

        // Only boost if the player is trying to go up (holding forward or jump)
        if (player.horizontalCollision || mc.options.jumpKey.isPressed()) {
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, climbSpeed.value(), velocity.z);
        }
    }
}
