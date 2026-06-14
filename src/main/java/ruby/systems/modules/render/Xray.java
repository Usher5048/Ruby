package ruby.systems.modules.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import ruby.RubyClient;
import ruby.helpers.world.BlockUtils;
import ruby.helpers.world.ChunkReloadHelper;
import ruby.systems.config.BlockListValue;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.List;

/**
 * Xray — based on Meteor Client 1.21.11.
 */
public class Xray extends Module {

    public static final List<Block> DEFAULT_ORES = List.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    private final BlockListValue blocks;
    private final IntegerValue opacity;
    private final BooleanValue exposedOnly;

    public Xray() {
        super("Xray", "Only renders specified blocks. Good for mining.", ModuleType.RENDER);

        blocks = config.create(new BlockListValue.Builder("Whitelist")
                .description("Which blocks to show x-rayed.")
                .defaultValue(DEFAULT_ORES)
                .changed(v -> { if (this.enabled()) reload(); })
                .build());
        opacity = config.create(new IntegerValue.Builder("Opacity")
                .description("Opacity for all other blocks.")
                .range(0, 255).defaultValue(25)
                .changed(v -> { if (this.enabled()) reload(); })
                .build());
        exposedOnly = config.create(new BooleanValue.Builder("Exposed Only")
                .description("Show only exposed ores.")
                .defaultValue(false)
                .changed(v -> { if (this.enabled()) reload(); })
                .build());
    }

    @Override
    public void onEnable() {
        reload();
    }

    @Override
    public void onDisable() {
        reload();
    }

    private void reload() {
        ChunkReloadHelper.schedule();
    }

    public boolean isBlocked(Block block, BlockPos pos) {
        if (!blocks.value().contains(block)) return true;
        if (!exposedOnly.value()) return false;
        if (pos == null || RubyClient.client.world == null) return false;
        return !BlockUtils.isExposed(pos, RubyClient.client.world);
    }

    public boolean isBlocked(Block block, BlockPos pos, BlockView view) {
        if (!blocks.value().contains(block)) return true;
        if (!exposedOnly.value()) return false;
        if (pos == null || view == null) return false;
        return !BlockUtils.isExposed(pos, view);
    }

    public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean original) {
        if (original || isBlocked(state.getBlock(), pos, view)) return original;

        BlockPos adjPos = pos.offset(facing);
        BlockState adjState = view.getBlockState(adjPos);
        return adjState.getCullingFace(facing.getOpposite()) != VoxelShapes.fullCube()
                || adjState.getBlock() != state.getBlock()
                || !adjState.isOpaqueFullCube()
                || isBlocked(adjState.getBlock(), adjPos, view);
    }

    public static int getAlpha(BlockState state, BlockPos pos) {
        return getAlpha(state, pos, null);
    }

    public static int getAlpha(BlockState state, BlockPos pos, BlockView view) {
        Xray xray = ruby.systems.modules.Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return -1;

        if (!xray.isBlocked(state.getBlock(), pos, view)) return -1;
        return xray.opacity.value();
    }
}
