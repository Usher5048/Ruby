package ruby.helpers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import ruby.RubyClient;

public final class BlockUtils {

    private BlockUtils() {}

    public static boolean isExposed(BlockPos pos) {
        if (RubyClient.client.level == null) return true;
        return isExposed(pos, RubyClient.client.level);
    }

    public static boolean isExposed(BlockPos pos, BlockGetter world) {
        if (world == null) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState state = world.getBlockState(neighbor);
            if (state.isAir() || !state.isSolidRender()) return true;
        }

        return false;
    }
}
