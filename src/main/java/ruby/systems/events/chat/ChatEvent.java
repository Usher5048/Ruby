package ruby.systems.events.chat;

import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import ruby.systems.events.Event;

public class ChatEvent extends Event {
    private Text message;
    private final MessageSignatureData signatureData;
    private final MessageIndicator indicator;

    public ChatEvent(Text message, MessageSignatureData signatureData, MessageIndicator indicator) {
        this.message = message;
        this.signatureData = signatureData;
        this.indicator = indicator;
    }

    public void setMessage(Text message) {
        this.message = message;
    }
    public Text message() {
        return this.message;
    }
    public MessageSignatureData signatureData() {
        return this.signatureData;
    }
    public MessageIndicator indicator() {
        return this.indicator;
    }
}
