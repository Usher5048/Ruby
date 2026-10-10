package ruby.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.systems.gui.AccountsScreen;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void ruby$addAccountsButton(CallbackInfo ci) {
        int btnW = 100;
        int btnH = 20;
        this.addRenderableWidget(Button.builder(Component.literal("Accounts"), button ->
                RubyClient.client.setScreenAndShow(new AccountsScreen(this))
        ).bounds(this.width - btnW - 5, 5, btnW, btnH).build());
    }
}
