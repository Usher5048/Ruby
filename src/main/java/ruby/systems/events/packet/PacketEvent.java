package ruby.systems.events.packet;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import ruby.systems.events.Event;

public class PacketEvent extends Event {
    private final Connection connection;
    private Packet<?> packet;

    public PacketEvent(Connection connection, Packet<?> packet) {
        this.connection = connection;
        this.packet = packet;
    }

    public Connection connection() {
        return this.connection;
    }
    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
    public Packet<?> packet() {
        return this.packet;
    }
}
