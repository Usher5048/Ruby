package ruby.helpers.world;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import ruby.RubyClient;

public final class BlockUtils {

    private BlockUtils() {}

    public static boolean isExposed(BlockPos pos) {
        if (RubyClient.client.world == null) return true;
        return isExposed(pos, RubyClient.client.world);
    }

    public static boolean isExposed(BlockPos pos, BlockView world) {
        if (world == null) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState state = world.getBlockState(neighbor);
            if (state.isAir() || !state.isOpaqueFullCube()) return true;
        }

        return false;
    }
}
