package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Prevents walking off blocks by sneaking at edges.
 * Uses collision detection to determine if the player is near a ledge,
 * then forces sneak input to prevent falling.
 */
public class SafeWalk extends Module {

    private final BooleanValue sneak;

    public SafeWalk() {
        super("Safe Walk", "Prevents you from walking off blocks.", ModuleCategory.MOVEMENT);

        config.create(new IntegerValue.Builder("Fall Distance")
                .description("Minimum fall distance before the module activates.")
                .defaultValue(1).min(1).max(20)
                .build());

        sneak = config.create(new BooleanValue.Builder("Sneak At Edge")
                .description("Sneak when approaching the edge of a block.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        if (!player.isOnGround() || player.isSneaking()) return;

        Box box = player.getBoundingBox();
        Box belowBox = box.offset(0, -player.getStepHeight() - 0.01, 0).expand(-0.3, 0, -0.3);

        boolean atEdge = !mc.world.getBlockCollisions(player, belowBox).iterator().hasNext();

        if (atEdge && sneak.value()) {
            mc.options.sneakKey.setPressed(true);
        }
    }
}
