package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Automatically jumps when you reach the edge of a block.
 * Uses bounding box collision detection: shrinks the player's box slightly
 * and checks if there would be no ground below. If so, jumps.
 */
public class Parkour extends Module {

    private final DoubleValue edgeDistance;

    public Parkour() {
        super("Parkour", "Automatically jumps at the edges of blocks.", ModuleType.MOVEMENT);

        edgeDistance = config.create(new DoubleValue.Builder("Edge Distance")
                .description("How far from the edge should you jump.")
                .defaultValue(0.01).min(0.001).max(0.1).step(0.001)
                .build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (!player.onGround() || mc.options.keyJump.isDown()) return;
        if (player.isShiftKeyDown() || mc.options.keyShift.isDown()) return;

        AABB box = player.getBoundingBox();
        AABB adjustedBox = box.move(0, -0.5, 0).inflate(-edgeDistance.value(), 0, -edgeDistance.value());

        // If no block collisions exist in the shrunken box below, we're at an edge
        if (!mc.level.getBlockCollisions(player, adjustedBox).iterator().hasNext()) {
            player.jumpFromGround();
        }
    }
}
