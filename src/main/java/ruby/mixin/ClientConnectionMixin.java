package ruby.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;

import java.util.Iterator;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
    @Shadow
    public abstract void send(Packet<?> packet, @Nullable ChannelFutureListener listener);

    @Inject(
            method = "channelRead0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
            ),
            cancellable = true
    )
    private void receivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo info) {
        Connection con = (Connection) (Object) this;
        Bypasses.get().resetConnection(con);

        if(packet instanceof ClientboundBundlePacket packetBundle) {
            Iterator<Packet<? super ClientGamePacketListener>> iterator = packetBundle.subPackets().iterator();
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

    @Overwrite
    public void send(Packet<?> packet) {
        Connection con = (Connection) (Object) this;

        PacketEvent event = new PacketEvent(con, packet);
        Bypasses.get().resetConnection(con);

        if(Events.PACKET.fire(PacketEvents.SEND, event)) return;

        Packet<?> bypassedPacket = Bypasses.get().modifyPacket(event.packet());
        if(bypassedPacket == null) return;

        Bypasses.get().updateServer(bypassedPacket);
        this.send(bypassedPacket, null);
    }
}
