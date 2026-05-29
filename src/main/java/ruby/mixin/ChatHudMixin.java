package ruby.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.events.Events;
import ruby.systems.events.chat.ChatEvent;
import ruby.systems.events.chat.ChatEvents;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(
            at = @At("HEAD"),
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            cancellable = true
    )
    private void fireChatEvent(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo info) {
        boolean isCancelled = Events.CHAT.fire(ChatEvents.RECEIVE, new ChatEvent(
                message,
                signatureData,
                indicator
        ));

        if(!isCancelled) return;
        info.cancel();
    }
}
