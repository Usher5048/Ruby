package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Automatically jumps when you reach the edge of a block.
 * Uses bounding box collision detection: shrinks the player's box slightly
 * and checks if there would be no ground below. If so, jumps.
 */
public class Parkour extends Module {

    private final DoubleValue edgeDistance;

    public Parkour() {
        super("Parkour", "Automatically jumps at the edges of blocks.", ModuleCategory.MOVEMENT);

        edgeDistance = config.create(new DoubleValue.Builder("Edge Distance")
                .description("How far from the edge should you jump.")
                .defaultValue(0.01).min(0.001).max(0.1).step(0.001)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        if (!player.isOnGround() || mc.options.jumpKey.isPressed()) return;
        if (player.isSneaking() || mc.options.sneakKey.isPressed()) return;

        Box box = player.getBoundingBox();
        Box adjustedBox = box.offset(0, -0.5, 0).expand(-edgeDistance.value(), 0, -edgeDistance.value());

        // If no block collisions exist in the shrunken box below, we're at an edge
        if (!mc.world.getBlockCollisions(player, adjustedBox).iterator().hasNext()) {
            player.jump();
        }
    }
}
