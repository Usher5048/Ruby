package ruby.systems.events.packet;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import ruby.systems.events.Event;

public class PacketEvent extends Event {
    public static class WrappedConnection {
        private final Connection connection;
        public WrappedConnection(Connection connection) {
            this.connection = connection;
        }

        public void send(Packet<?> packet) {
            this.connection.send(packet, null);
        }
        public void resend(Packet<?> packet) {
            this.connection.send(packet);
        }
    }

    private final WrappedConnection connection;
    private Packet<?> packet;

    public PacketEvent(Connection connection, Packet<?> packet) {
        this.connection = new WrappedConnection(connection);
        this.packet = packet;
    }

    public WrappedConnection connection() {
        return this.connection;
    }
    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
    public Packet<?> packet() {
        return this.packet;
    }
}
