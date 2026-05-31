package ruby.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ruby.systems.gui.LoadingOverlay;

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
}
