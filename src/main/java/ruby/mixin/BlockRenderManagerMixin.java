package ruby.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.helpers.render.XrayAlphaVertexConsumer;
import ruby.systems.modules.render.Xray;

import java.util.List;

@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {
    @Unique
    private static final ThreadLocal<Integer> RUBY_XRAY_ALPHA = ThreadLocal.withInitial(() -> -1);

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void ruby$xrayCancelBlock(
            BlockState state,
            BlockPos pos,
            BlockRenderView world,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            boolean cull,
            List<BlockModelPart> parts,
            CallbackInfo ci
    ) {
        int alpha = Xray.getAlpha(state, pos);
        RUBY_XRAY_ALPHA.set(alpha);
        if (alpha == 0) ci.cancel();
    }

    @ModifyVariable(method = "renderBlock", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private VertexConsumer ruby$xrayAlphaConsumer(VertexConsumer consumer) {
        int alpha = RUBY_XRAY_ALPHA.get();
        if (alpha <= 0 || alpha >= 255) return consumer;
        return new XrayAlphaVertexConsumer(consumer, alpha);
    }
}
