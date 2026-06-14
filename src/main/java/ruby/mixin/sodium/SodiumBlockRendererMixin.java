package ruby.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.render.Xray;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererMixin {
    @Unique
    private static final ThreadLocal<Integer> ruby$xrayAlpha = new ThreadLocal<>();

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void ruby$onRenderModelHead(
            BlockStateModel model,
            BlockState state,
            BlockPos pos,
            BlockPos origin,
            CallbackInfo ci
    ) {
        int alpha = Xray.getAlpha(state, pos);
        ruby$xrayAlpha.set(alpha);
        if (alpha == 0) ci.cancel();
    }

    @Inject(method = "renderModel", at = @At("RETURN"), remap = false)
    private void ruby$onRenderModelReturn(CallbackInfo ci) {
        ruby$xrayAlpha.remove();
    }

    @Inject(method = "bufferQuad", at = @At("HEAD"), remap = false)
    private void ruby$onBufferQuad(
            MutableQuadViewImpl quad,
            float[] brightnesses,
            Material material,
            CallbackInfo ci
    ) {
        Integer alpha = ruby$xrayAlpha.get();
        if (alpha == null || alpha == -1) return;

        for (int i = 0; i < 4; i++) {
            int color = quad.baseColor(i);
            quad.setColor(i, ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF));
        }
    }

    @ModifyArg(
            method = "processQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V"
            ),
            index = 2,
            remap = false
    )
    private Material ruby$translucentMaterial(Material material) {
        Integer alpha = ruby$xrayAlpha.get();
        if (alpha != null && alpha != -1) return DefaultMaterials.TRANSLUCENT;
        return material;
    }
}
