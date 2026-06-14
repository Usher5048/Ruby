package ruby.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, KeyInput input, CallbackInfo info) {
        if(input.key() == GLFW.GLFW_KEY_UNKNOWN) return;
        InputUtils.setKeyState(input.key(), action != GLFW.GLFW_RELEASE);
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void ruby$openCommandChat(long window, CharInput input, CallbackInfo ci) {
        if(!input.isValidChar()) return;
        if(this.client.currentScreen != null) return;
        if(this.client.player == null) return;

        String prefix = RubyClient.chatPrefix.value();
        if(prefix.isEmpty()) return;

        String typed = input.asString();
        if(!typed.equals(prefix)) return;

        this.client.setScreen(new ChatScreen(prefix, false));
        ci.cancel();
    }
}