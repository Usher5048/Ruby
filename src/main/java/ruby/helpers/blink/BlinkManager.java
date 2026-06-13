package ruby.helpers.blink;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.systems.events.Events;
import ruby.systems.events.blink.BlinkPacketEvent;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.events.TickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queues packets and flushes them on demand. LiquidBounce {@code BlinkManager} port.
 */
public final class BlinkManager {
    public enum Origin {
        OUTGOING,
        INCOMING
    }

    public enum Action {
        FLUSH(0),
        PASS(1),
        QUEUE(2);

        private final int priority;

        Action(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return this.priority;
        }
    }

    public record PacketSnapshot(Packet<?> packet, Origin origin, long timestamp) {}

    private static final ConcurrentLinkedQueue<PacketSnapshot> PACKET_QUEUE = new ConcurrentLinkedQueue<>();

    static {
        Events.PACKET.register(PacketEvents.SEND, BlinkManager::onSend);
        Events.PACKET.register(PacketEvents.RECEIVE, BlinkManager::onReceive);
        Events.TICK.register(TickEvents.END, event -> processTickFlush());
    }

    private BlinkManager() {
    }

    public static List<Vec3d> positions() {
        List<Vec3d> result = new ArrayList<>();
        for (PacketSnapshot snapshot : PACKET_QUEUE) {
            if (snapshot.origin() != Origin.OUTGOING) continue;
            if (!(snapshot.packet() instanceof PlayerMoveC2SPacket move) || !move.changesPosition()) continue;
            result.add(new Vec3d(move.getX(0), move.getY(0), move.getZ(0)));
        }
        return result;
    }

    public static boolean isLagging() {
        return !PACKET_QUEUE.isEmpty();
    }

    public static boolean isAboveTime(long delayMs) {
        PacketSnapshot first = PACKET_QUEUE.peek();
        if (first == null) return false;
        return System.currentTimeMillis() - first.timestamp() >= delayMs;
    }

    public static void flush(Origin origin) {
        flush(snapshot -> snapshot.origin() == origin);
    }

    public static void flush(java.util.function.Predicate<PacketSnapshot> flushWhen) {
        Iterator<PacketSnapshot> iterator = PACKET_QUEUE.iterator();
        while (iterator.hasNext()) {
            PacketSnapshot snapshot = iterator.next();
            if (!flushWhen.test(snapshot)) continue;

            flushSnapshot(snapshot);
            iterator.remove();
        }
    }

    public static void flush(int movePacketCount) {
        int counter = 0;
        Iterator<PacketSnapshot> iterator = PACKET_QUEUE.iterator();
        while (iterator.hasNext()) {
            PacketSnapshot snapshot = iterator.next();
            Packet<?> packet = snapshot.packet();

            if (packet instanceof PlayerMoveC2SPacket move && move.changesPosition()) {
                counter++;
            }

            flushSnapshot(snapshot);
            iterator.remove();

            if (counter >= movePacketCount) break;
        }
    }

    public static void cancel() {
        Vec3d firstPos = positions().isEmpty() ? null : positions().getFirst();
        if (firstPos != null && RubyClient.client.player != null) {
            RubyClient.client.player.setPosition(firstPos);
        }

        for (PacketSnapshot snapshot : PACKET_QUEUE) {
            if (snapshot.packet() instanceof PlayerMoveC2SPacket) continue;
            flushSnapshot(snapshot);
        }
        PACKET_QUEUE.clear();
    }

    public static void clear() {
        PACKET_QUEUE.clear();
    }

    public static Action fireEvent(Packet<?> packet, Origin origin) {
        BlinkPacketEvent event = new BlinkPacketEvent(packet, origin);
        Events.BLINK.fire(event);
        return event.action();
    }

    private static void onSend(PacketEvent event) {
        if (!isConnected()) {
            PACKET_QUEUE.clear();
            return;
        }

        Packet<?> packet = event.packet();
        Action lagResult = fireEvent(packet, Origin.OUTGOING);
        if (lagResult == Action.FLUSH) {
            flush(Origin.OUTGOING);
            return;
        }
        if (lagResult == Action.PASS) return;

        if (shouldIgnoreOutgoing(packet)) return;

        event.setCancelled(true);
        PACKET_QUEUE.add(new PacketSnapshot(packet, Origin.OUTGOING, System.currentTimeMillis()));
    }

    private static void onReceive(PacketEvent event) {
        if (!isConnected()) {
            PACKET_QUEUE.clear();
            return;
        }

        Packet<?> packet = event.packet();
        if (packet instanceof BundleS2CPacket bundle) {
            Iterator<Packet<? super net.minecraft.network.listener.ClientPlayPacketListener>> iterator =
                    bundle.getPackets().iterator();
            while (iterator.hasNext()) {
                Packet<?> bundled = iterator.next();
                if (handleIncoming(bundled, event)) iterator.remove();
            }
            return;
        }

        if (handleIncoming(packet, event)) event.setCancelled(true);
    }

    private static boolean handleIncoming(Packet<?> packet, PacketEvent event) {
        Action lagResult = fireEvent(packet, Origin.INCOMING);
        if (lagResult == Action.FLUSH) {
            flush(Origin.INCOMING);
            return false;
        }
        if (lagResult == Action.PASS) return false;

        if (shouldFlushIncoming(packet)) {
            flush(Origin.INCOMING);
            return false;
        }

        PACKET_QUEUE.add(new PacketSnapshot(packet, Origin.INCOMING, System.currentTimeMillis()));
        return true;
    }

    private static void processTickFlush() {
        if (!isConnected()) {
            PACKET_QUEUE.clear();
            return;
        }

        if (fireEvent(null, Origin.OUTGOING) == Action.FLUSH) {
            flush(Origin.OUTGOING);
        }
        if (fireEvent(null, Origin.INCOMING) == Action.FLUSH) {
            flush(Origin.INCOMING);
        }
    }

    private static boolean shouldIgnoreOutgoing(Packet<?> packet) {
        return switch (packet) {
            case net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket ignored -> true;
            case net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket ignored -> true;
            case net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket ignored -> true;
            case net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket ignored -> true;
            default -> false;
        };
    }

    private static boolean shouldFlushIncoming(Packet<?> packet) {
        return switch (packet) {
            case PlayerPositionLookS2CPacket ignored -> true;
            case net.minecraft.network.packet.s2c.common.DisconnectS2CPacket ignored -> true;
            case net.minecraft.network.packet.s2c.play.GameJoinS2CPacket ignored -> true;
            case HealthUpdateS2CPacket health when health.getHealth() <= 0 -> true;
            default -> false;
        };
    }

    private static void flushSnapshot(PacketSnapshot snapshot) {
        MinecraftClient mc = RubyClient.client;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return;

        switch (snapshot.origin()) {
            case OUTGOING -> handler.getConnection().send(snapshot.packet());
            case INCOMING -> {
                @SuppressWarnings("unchecked")
                Packet<net.minecraft.network.listener.ClientPlayPacketListener> playPacket =
                        (Packet<net.minecraft.network.listener.ClientPlayPacketListener>) snapshot.packet();
                playPacket.apply(handler);
            }
        }
    }

    private static boolean isConnected() {
        MinecraftClient mc = RubyClient.client;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return false;
        ClientConnection connection = handler.getConnection();
        return connection != null && connection.isOpen();
    }
}
