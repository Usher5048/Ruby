package ruby.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.gui.Toasts;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void ruby$renderToasts(GuiGraphicsExtractor context, CallbackInfo ci) {
        Toasts.render(context);
    }
}
