package ruby.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.gui.LoadingOverlay;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Freecam;

@Mixin(Mouse.class)
public class MouseMixin {
    @Redirect(
            method = "onMouseButton",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;",
                ordinal = 0
            )
    )
    private Overlay skipIntroOverlay(MinecraftClient client) {
        Overlay overlay = client.getOverlay();
        if(!(client.currentScreen instanceof TitleScreen)) return overlay;
        if(!(overlay instanceof LoadingOverlay loading)) return overlay;
        loading.exit();

        client.setOverlay(null);
        return overlay;
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void ruby$freecamScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam == null || !freecam.enabled()) return;
        if (freecam.onScroll(vertical)) ci.cancel();
    }
}
