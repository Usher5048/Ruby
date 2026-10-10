package ruby.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.events.Events;
import ruby.systems.events.chat.ChatEvent;
import ruby.systems.events.chat.ChatEvents;

@Mixin(ChatComponent.class)
public class ChatHudMixin {
    @Inject(
            at = @At("HEAD"),
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            cancellable = true
    )
    private void fireChatEvent(Component message, MessageSignature signatureData, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo info) {
        boolean isCancelled = Events.CHAT.fire(ChatEvents.RECEIVE, new ChatEvent(
                message,
                signatureData,
                indicator
        ));

        if(!isCancelled) return;
        info.cancel();
    }
}
