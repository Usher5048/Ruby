package ruby.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Shadow
    private ClientConnection connection;

    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo info) {
        PacketEvent event = new PacketEvent(this.connection, packet);
        Bypasses.get().resetConnection(this.connection);

        info.cancel();
        if(Events.PACKET.fire(PacketEvents.SEND, event)) return;

        Packet<?> bypassedPacket = Bypasses.get().modifyPacket(event.packet());
        if(bypassedPacket == null) return;

        // updateServer runs once from ClientConnectionMixin.send HEAD
        this.connection.send(bypassedPacket);
    }
}
