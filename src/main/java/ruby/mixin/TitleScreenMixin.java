package ruby.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.systems.gui.LoadingOverlay;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    private static boolean showedLoading = false;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("HEAD"), method = "extractRenderState", cancellable = true)
    private void showOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        if(TitleScreenMixin.showedLoading) return;

        RubyClient.client.gui.setOverlay(new LoadingOverlay(this, RubyClient::loadClient));
        TitleScreenMixin.showedLoading = true;
    }
}
