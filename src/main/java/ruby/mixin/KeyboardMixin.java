package ruby.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Freecam;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, KeyEvent input, CallbackInfo info) {
        if(input.key() == InputConstants.UNKNOWN.getValue()) return;
        InputUtils.setKeyState(input.key(), action != InputConstants.RELEASE);

        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled() && freecam.onKey(input.key(), action)) {
            info.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void ruby$openCommandChat(long window, CharacterEvent input, CallbackInfo ci) {
        if(!input.isAllowedChatCharacter()) return;
        if(this.minecraft.gui.screen() != null) return;
        if(this.minecraft.player == null) return;

        String prefix = RubyClient.chatPrefix.value();
        if(prefix.isEmpty()) return;

        String typed = input.codepointAsString();
        if(!typed.equals(prefix)) return;

        this.minecraft.setScreenAndShow(new ChatScreen(prefix, false));
        ci.cancel();
    }
}
