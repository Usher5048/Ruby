package ruby.systems.modules.world;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Automatically places blocks below the player as they walk.
 * Only works when holding a block in the main hand.
 */
public class Scaffold extends Module {

    private final BooleanValue onlyOnMove;

    public Scaffold() {
        super("Scaffold", "Automatically places blocks below you.", ModuleCategory.WORLD);

        onlyOnMove = config.create(new BooleanValue.Builder("Only On Move")
                .description("Only place blocks when you are moving.")
                .defaultValue(true)
                .build());

        config.create(new BooleanValue.Builder("Tower")
                .description("Place blocks below when jumping upward.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) return;

        // Check if holding a block
        ItemStack mainHand = player.getMainHandStack();
        if (!(mainHand.getItem() instanceof BlockItem)) return;

        // Check if moving (if setting is enabled)
        if (onlyOnMove.value() && player.forwardSpeed == 0 && player.sidewaysSpeed == 0
                && !mc.options.jumpKey.isPressed()) return;

        BlockPos below = player.getBlockPos().down();

        // Check if there's air below
        BlockState belowState = mc.world.getBlockState(below);
        if (!belowState.isReplaceable()) return;

        // Find a face to place against
        Direction placeDir = findPlaceFace(mc, below);
        if (placeDir == null) return;

        BlockPos neighbor = below.offset(placeDir);
        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(neighbor),
                placeDir.getOpposite(),
                neighbor,
                false
        );

        mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        player.swingHand(Hand.MAIN_HAND);
    }

    private Direction findPlaceFace(MinecraftClient mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState state = mc.world.getBlockState(neighbor);
            if (!state.isReplaceable() && !state.isAir()) {
                return dir;
            }
        }
        return null;
    }
}
