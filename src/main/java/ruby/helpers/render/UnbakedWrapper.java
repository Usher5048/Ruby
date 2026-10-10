package ruby.helpers.render;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperUnbakedRootBlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;

public class UnbakedWrapper extends WrapperUnbakedRootBlockStateModel implements BlockStateModel.UnbakedRoot {
    private final BlockStateModel.UnbakedRoot wrapped;
    private final Function<BlockStateModel, BlockStateModel> modelWrapper;

    public UnbakedWrapper(
            BlockStateModel.UnbakedRoot wrapped,
            Function<BlockStateModel, BlockStateModel> modelWrapper
    ) {
        super(wrapped);
        this.wrapped = wrapped;
        this.modelWrapper = modelWrapper;
    }

    @Override
    public BlockStateModel bake(BlockState state, ModelBaker baker) {
        return this.modelWrapper.apply(this.wrapped.bake(state, baker));
    }

    @Override public Object visualEqualityGroup(BlockState state) {
        return this.wrapped.visualEqualityGroup(state);
    }
    @Override public void resolveDependencies(Resolver resolver) {
        this.wrapped.resolveDependencies(resolver);
    }
}
