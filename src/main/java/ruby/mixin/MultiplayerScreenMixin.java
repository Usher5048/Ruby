package ruby.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.systems.gui.AccountsScreen;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void ruby$addAccountsButton(CallbackInfo ci) {
        int btnW = 100;
        int btnH = 20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Accounts"), button ->
                RubyClient.client.setScreen(new AccountsScreen(this))
        ).dimensions(this.width - btnW - 5, 5, btnW, btnH).build());
    }
}
