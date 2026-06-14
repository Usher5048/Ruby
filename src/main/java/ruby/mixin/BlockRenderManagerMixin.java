package ruby.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.render.Xray;

import java.util.List;

@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void ruby$xrayHead(
            BlockState state,
            BlockPos pos,
            BlockRenderView world,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            boolean cull,
            List<BlockModelPart> parts,
            CallbackInfo ci
    ) {
        if (Xray.getAlpha(state, pos) == 0) ci.cancel();
    }
}
