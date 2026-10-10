package ruby.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.QuadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockModelRendererMixin {
    @Shadow @Final private QuadInstance quadInstance;

    @Unique
    private final ThreadLocal<Integer> ruby$xrayAlpha = new ThreadLocal<>();

    @ModifyReturnValue(method = "shouldRenderFace", at = @At("RETURN"))
    private boolean ruby$modifyDrawFace(
            boolean original,
            BlockAndTintGetter world,
            BlockState state,
            Direction direction,
            BlockPos pos
    ) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return original;
        return xray.modifyDrawSide(state, world, pos.relative(direction.getOpposite()), direction, original);
    }

    @Inject(method = {"tesselateAmbientOcclusion", "tesselateFlat"}, at = @At("HEAD"), cancellable = true)
    private void ruby$xrayHead(
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            List<BlockStateModelPart> parts,
            BlockAndTintGetter world,
            BlockState state,
            BlockPos pos,
            CallbackInfo ci
    ) {
        int alpha = Xray.getAlpha(state, pos);
        if (alpha == 0) {
            ci.cancel();
            return;
        }
        ruby$xrayAlpha.set(alpha);
    }

    @Inject(method = {"tesselateAmbientOcclusion", "tesselateFlat"}, at = @At("RETURN"))
    private void ruby$xrayTail(CallbackInfo ci) {
        ruby$xrayAlpha.remove();
    }

    @Inject(
            method = "putQuadWithTint",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/QuadInstance;multiplyColor(I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ruby$modifyAlpha(CallbackInfo ci) {
        Integer alpha = ruby$xrayAlpha.get();
        if (alpha == null || alpha == -1) return;
        this.quadInstance.multiplyColor((alpha << 24) | 0x00FFFFFF);
    }
}
