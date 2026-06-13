package ruby.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.helpers.render.MeshUniformBuilder;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method = "flipFrame", at = @At("TAIL"))
    private static void ruby$flipFrame(CallbackInfo info) {
        MeshUniformBuilder.flipFrame();
    }
}
