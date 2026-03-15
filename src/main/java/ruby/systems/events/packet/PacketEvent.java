package ruby.systems.events.packet;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import ruby.systems.events.Event;

public class PacketEvent extends Event {
    private final ClientConnection connection;
    private Packet<?> packet;

    public PacketEvent(ClientConnection connection, Packet<?> packet) {
        this.connection = connection;
        this.packet = packet;
    }

    public ClientConnection connection() {
        return this.connection;
    }
    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
    public Packet<?> packet() {
        return this.packet;
    }
}
