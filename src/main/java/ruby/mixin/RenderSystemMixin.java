package ruby.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.helpers.render.MeshUniformBuilder;

@Mixin(GameRenderer.class)
public class RenderSystemMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void ruby$flipFrame(CallbackInfo info) {
        MeshUniformBuilder.flipFrame();
    }
}
