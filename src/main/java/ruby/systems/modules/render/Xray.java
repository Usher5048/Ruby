package ruby.systems.modules.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import ruby.RubyClient;
import ruby.helpers.world.BlockUtils;
import ruby.systems.config.BlockListValue;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.List;

/**
 * Ported from Meteor Client xray.
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
                .build());
        opacity = config.create(new IntegerValue.Builder("Opacity")
                .description("Opacity for all other blocks.")
                .range(0, 255).defaultValue(25)
                .changed(v -> reload())
                .build());
        exposedOnly = config.create(new BooleanValue.Builder("Exposed Only")
                .description("Show only exposed ores.")
                .defaultValue(false)
                .changed(v -> reload())
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
        if (RubyClient.client.worldRenderer != null) {
            RubyClient.client.worldRenderer.reload();
        }
    }

    public boolean isBlocked(Block block, BlockPos pos) {
        if (!blocks.value().contains(block)) return true;
        if (!exposedOnly.value()) return false;
        return pos == null || !BlockUtils.isExposed(pos);
    }

    public boolean modifyDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, boolean original) {
        if (original || isBlocked(state.getBlock(), pos)) return original;

        BlockPos adjPos = pos.offset(facing);
        BlockState adjState = view.getBlockState(adjPos);
        return !adjState.isOpaqueFullCube()
                || adjState.getBlock() != state.getBlock()
                || isBlocked(adjState.getBlock(), adjPos);
    }

    public static int getAlpha(BlockState state, BlockPos pos) {
        Xray xray = ruby.systems.modules.Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return -1;

        if (!xray.isBlocked(state.getBlock(), pos)) return -1;
        return xray.opacity.value();
    }
}
