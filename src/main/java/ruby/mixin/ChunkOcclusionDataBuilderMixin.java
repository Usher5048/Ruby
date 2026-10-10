package ruby.mixin;

import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Freecam;
import ruby.systems.modules.render.Xray;

@Mixin(VisGraph.class)
public abstract class ChunkOcclusionDataBuilderMixin {
    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
    private void ruby$disableOcclusion(BlockPos pos, CallbackInfo ci) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray != null && xray.enabled()) {
            ci.cancel();
            return;
        }

        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.reloadChunks()) {
            ci.cancel();
        }
    }
}
