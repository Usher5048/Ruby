package ruby.mixin.sodium;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.render.Xray;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class SodiumDefaultFluidRendererMixin {
    @Shadow @Final private int[] quadColors;

    @Unique
    private static final ThreadLocal<Integer> ruby$xrayAlpha = new ThreadLocal<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void ruby$onRenderHead(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkModelBuilder meshBuilder,
            Material material,
            ColorProvider<FluidState> colorProvider,
            FluidModel fluidModel,
            CallbackInfo ci
    ) {
        int alpha = Xray.getAlpha(fluidState.createLegacyBlock(), blockPos);
        ruby$xrayAlpha.set(alpha);
        if (alpha == 0) ci.cancel();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void ruby$onRenderReturn(CallbackInfo ci) {
        ruby$xrayAlpha.remove();
    }

    @Inject(method = "updateQuad", at = @At("TAIL"), remap = false)
    private void ruby$onUpdateQuad(
            ModelQuadViewMutable quad,
            LevelSlice level,
            BlockPos pos,
            LightPipeline lighter,
            Direction dir,
            ModelQuadFacing facing,
            float brightness,
            ColorProvider<FluidState> colorProvider,
            FluidState fluidState,
            CallbackInfo ci
    ) {
        Integer alpha = ruby$xrayAlpha.get();
        if (alpha == null || alpha == -1) return;

        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = (this.quadColors[i] & 0x00FFFFFF) | (alpha << 24);
        }
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;writeQuad(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;Z)V"
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
