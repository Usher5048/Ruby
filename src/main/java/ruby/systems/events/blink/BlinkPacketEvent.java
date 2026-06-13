package ruby.systems.events.blink;

import net.minecraft.network.packet.Packet;
import ruby.systems.events.Event;
import ruby.helpers.blink.BlinkManager;

public class BlinkPacketEvent extends Event {
    private final Packet<?> packet;
    private final BlinkManager.Origin origin;
    private BlinkManager.Action action = BlinkManager.Action.PASS;

    public BlinkPacketEvent(Packet<?> packet, BlinkManager.Origin origin) {
        this.packet = packet;
        this.origin = origin;
    }

    public Packet<?> packet() {
        return this.packet;
    }

    public BlinkManager.Origin origin() {
        return this.origin;
    }

    public BlinkManager.Action action() {
        return this.action;
    }

    public void setAction(BlinkManager.Action action) {
        this.action = action;
    }
}
