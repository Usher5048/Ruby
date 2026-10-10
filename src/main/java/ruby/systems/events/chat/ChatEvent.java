package ruby.systems.events.chat;

import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import ruby.systems.events.Event;

public class ChatEvent extends Event {
    private Component message;
    private final MessageSignature signatureData;
    private final GuiMessageTag indicator;

    public ChatEvent(Component message, MessageSignature signatureData, GuiMessageTag indicator) {
        this.message = message;
        this.signatureData = signatureData;
        this.indicator = indicator;
    }

    public void setMessage(Component message) {
        this.message = message;
    }
    public Component message() {
        return this.message;
    }
    public MessageSignature signatureData() {
        return this.signatureData;
    }
    public GuiMessageTag indicator() {
        return this.indicator;
    }
}
