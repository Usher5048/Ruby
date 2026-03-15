package ruby.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.systems.gui.LoadingOverlay;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    private static boolean showedLoading = false;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    private void showOverlay(CallbackInfo info) {
        if(TitleScreenMixin.showedLoading) return;

        RubyClient.client.setOverlay(new LoadingOverlay(this, RubyClient::loadClient));
        TitleScreenMixin.showedLoading = true;
    }
}
