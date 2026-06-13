package ruby.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Shadow
    private ClientConnection connection;

    @Overwrite
    public void sendPacket(Packet<?> packet) {
        PacketEvent event = new PacketEvent(this.connection, packet);
        Bypasses.get().resetConnection(this.connection);

        if(Events.PACKET.fire(PacketEvents.SEND, event)) return;

        Packet<?> bypassedPacket = Bypasses.get().modifyPacket(event.packet());
        if(bypassedPacket == null) return;

        Bypasses.get().updateServer(bypassedPacket);
        this.connection.send(bypassedPacket);
    }
}
