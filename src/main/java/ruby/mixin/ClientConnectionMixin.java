package ruby.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;

import java.util.Iterator;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(
            method = "channelRead0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V"
            ),
            cancellable = true
    )
    private void receivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo info) {
        ClientConnection con = (ClientConnection) (Object) this;
        Bypasses.get().resetConnection(con);

        if(packet instanceof BundleS2CPacket packetBundle) {
            Iterator<Packet<? super ClientPlayPacketListener>> iterator = packetBundle.getPackets().iterator();
            while(iterator.hasNext()) {
                Packet<?> p = iterator.next();
                Bypasses.get().updateServer(p);
                if(Events.PACKET.fire(PacketEvents.RECEIVE, new PacketEvent(con, p)))
                    iterator.remove();
            }

            return;
        }

        Bypasses.get().updateServer(packet);
        if(Events.PACKET.fire(PacketEvents.RECEIVE, new PacketEvent(con, packet)))
            info.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void updateBypass(Packet<?> packet, CallbackInfo info) {
        Bypasses.get().updateServer(packet);
    }
}
