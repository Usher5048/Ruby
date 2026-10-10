package ruby.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.helpers.input.InputUtils;
import ruby.systems.gui.LoadingOverlay;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Freecam;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Redirect(
            method = "onButton",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;",
                ordinal = 0
            )
    )
    private Overlay skipIntroOverlay(Gui gui) {
        Overlay overlay = gui.overlay();
        if(!(gui.screen() instanceof TitleScreen)) return overlay;
        if(!(overlay instanceof LoadingOverlay loading)) return overlay;
        loading.exit();

        gui.setOverlay(null);
        return overlay;
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    private void ruby$trackMouse(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        InputUtils.setMouseState(info.button(), action != InputConstants.RELEASE);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ruby$freecamScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam == null || !freecam.enabled()) return;
        if (freecam.onScroll(vertical)) ci.cancel();
    }
}
