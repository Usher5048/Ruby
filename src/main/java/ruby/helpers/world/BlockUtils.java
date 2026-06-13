package ruby.helpers.world;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import ruby.RubyClient;

public final class BlockUtils {

    private BlockUtils() {}

    public static boolean isExposed(BlockPos pos) {
        World world = RubyClient.client.world;
        if (world == null) return true;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            BlockState state = world.getBlockState(neighbor);
            if (state.isAir() || !state.isOpaqueFullCube()) return true;
        }

        return false;
    }
}
