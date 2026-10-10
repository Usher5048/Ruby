package ruby.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.render.Xray;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockRenderManagerMixin {
    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void ruby$xrayHead(
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            BlockStateModel model,
            long seed,
            CallbackInfo ci
    ) {
        if (Xray.getAlpha(state, pos) == 0) ci.cancel();
    }
}
