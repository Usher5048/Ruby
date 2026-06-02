package ruby.helpers.render;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperUnbakedGroupedBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.BlockStateModel;

import java.util.function.Function;

public class UnbakedWrapper extends WrapperUnbakedGroupedBlockStateModel implements BlockStateModel.UnbakedGrouped {
    private final BlockStateModel.UnbakedGrouped wrapped;
    private final Function<BlockStateModel, BlockStateModel> modelWrapper;

    public UnbakedWrapper(
            BlockStateModel.UnbakedGrouped wrapped,
            Function<BlockStateModel, BlockStateModel> modelWrapper
    ) {
        this.wrapped = wrapped;
        this.modelWrapper = modelWrapper;
    }

    @Override
    public BlockStateModel bake(BlockState state, Baker baker) {
        return this.modelWrapper.apply(this.wrapped.bake(state, baker));
    }

    @Override public Object getEqualityGroup(BlockState state) {
        return this.wrapped.getEqualityGroup(state);
    }
    @Override public void resolve(Resolver resolver) {
        this.wrapped.resolve(resolver);
    }
}
