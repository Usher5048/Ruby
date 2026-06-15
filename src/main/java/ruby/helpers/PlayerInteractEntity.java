package ruby.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.mixin.PlayerInteractEntityC2SPacketAccessor;

import java.util.concurrent.atomic.AtomicReference;

public class PlayerInteractEntity implements PlayerInteractEntityC2SPacket.Handler {
    public enum Type {
        INTERACT,
        INTERACT_AT,
        ATTACK
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        if(RubyClient.client.world == null) return null;
        int id = ((PlayerInteractEntityC2SPacketAccessor) packet).getEntityID();
        return RubyClient.client.world.getEntityById(id);
    }

    private final AtomicReference<Type> type;
    public PlayerInteractEntity(AtomicReference<Type> type) {
        this.type = type;
    }

    @Override public void interact(Hand hand) {
        this.type.set(Type.INTERACT);
    }
    @Override public void interactAt(Hand hand, Vec3d pos) {
        this.type.set(Type.INTERACT_AT);
    }
    @Override public void attack() {
        this.type.set(Type.ATTACK);
    }
}
