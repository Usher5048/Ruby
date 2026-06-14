package ruby.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

import java.util.List;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {
    @Unique
    private final ThreadLocal<Integer> ruby$xrayAlpha = new ThreadLocal<>();

    @ModifyReturnValue(method = "shouldDrawFace", at = @At("RETURN"))
    private static boolean ruby$modifyDrawFace(
            boolean original,
            BlockRenderView world,
            BlockState state,
            boolean solid,
            Direction direction,
            BlockPos pos
    ) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return original;
        return xray.modifyDrawSide(state, world, pos.offset(direction.getOpposite()), direction, original);
    }

    @Inject(method = {"renderSmooth", "renderFlat"}, at = @At("HEAD"), cancellable = true)
    private void ruby$xrayHead(
            BlockRenderView world,
            List<BlockModelPart> parts,
            BlockState state,
            BlockPos pos,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            boolean cull,
            int overlay,
            CallbackInfo ci
    ) {
        int alpha = Xray.getAlpha(state, pos);
        if (alpha == 0) {
            ci.cancel();
            return;
        }
        ruby$xrayAlpha.set(alpha);
    }

    @Inject(method = {"renderSmooth", "renderFlat"}, at = @At("RETURN"))
    private void ruby$xrayTail(CallbackInfo ci) {
        ruby$xrayAlpha.remove();
    }

    @ModifyArgs(
            method = "renderQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;[FFFFF[II)V"
            )
    )
    private void ruby$modifyAlpha(Args args) {
        Integer alpha = ruby$xrayAlpha.get();
        args.set(6, alpha == null || alpha == -1 ? args.get(6) : alpha / 255f);
    }
}
