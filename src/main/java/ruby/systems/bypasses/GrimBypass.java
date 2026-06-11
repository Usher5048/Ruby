package ruby.systems.bypasses;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import ruby.RubyClient;

public class GrimBypass extends Bypass {
    private boolean exempt = false;
    private int lastSlot = -1;

    private Packet<?> aimDuplicateLook(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move)) return packet;

        boolean sameRotation = move.changesLook() &&
                this.yaw() == move.getYaw(this.yaw()) &&
                this.pitch() == move.getPitch(this.pitch());

        if(this.exempt) {
            this.exempt = false;
            return packet;
        }

        if(!sameRotation) return packet;
        return null;
    }

    private Packet<?> aimModulo360(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move)) return packet;
        if(!move.changesLook()) return packet;

        double x = move.getX(this.position().getX());
        double y = move.getY(this.position().getY());
        double z = move.getZ(this.position().getZ());
        float yaw = move.getYaw(this.yaw()) % 360;
        float pitch = move.getPitch(this.pitch());
        boolean ground = move.isOnGround();
        boolean horizC = move.horizontalCollision();

        if(yaw < 0) yaw += 360;
        yaw += 360;

        if(move.changesPosition()) return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground, horizC);
        else return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, ground, horizC);
    }

    private Packet<?> badPacketsA(Packet<?> packet) {
        if(!(packet instanceof UpdateSelectedSlotC2SPacket slot)) return packet;
        if(slot.getSelectedSlot() != this.lastSlot) return packet;
        return null;
    }

    private Packet<?> badPacketsB(Packet<?> packet) {
        return packet;
    }

    private Packet<?> badPacketsC(Packet<?> packet) {
        if(RubyClient.client.player == null) return packet;
        if(RubyClient.client.player.isSleeping()) return packet;
        if(!(packet instanceof ClientCommandC2SPacket command)) return packet;
        if(command.getMode() != ClientCommandC2SPacket.Mode.STOP_SLEEPING) return packet;
        return null;
    }

    private Packet<?> badPacketsD(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move)) return packet;
        if(!move.changesLook()) return packet;

        double x = move.getX(this.position().getX());
        double y = move.getY(this.position().getY());
        double z = move.getZ(this.position().getZ());
        float yaw = move.getYaw(this.yaw());
        float pitch = Math.clamp(move.getPitch(this.pitch()), -90, 90);
        boolean ground = move.isOnGround();
        boolean horizC = move.horizontalCollision();

        if(move.changesPosition()) return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground, horizC);
        else return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, ground, horizC);
    }

    @Override
    protected void onPacket(Packet<?> packet) {
        if(packet instanceof PlayerPositionLookS2CPacket) this.exempt = true;
        if(packet instanceof UpdateSelectedSlotC2SPacket slot) this.lastSlot = slot.getSelectedSlot();
        if(packet instanceof UpdateSelectedSlotS2CPacket slot) this.lastSlot = slot.slot();
    }

    @Override
    public Packet<?> modifyPacket(Packet<?> packet) {
        Packet<?> modified = packet;

        // Modifying
        modified = this.aimModulo360(modified);
        modified = this.badPacketsB(modified);
        modified = this.badPacketsD(modified);

        // Canceling
        modified = this.aimDuplicateLook(modified);
        modified = this.badPacketsA(modified);
        modified = this.badPacketsC(modified);

        return modified;
    }

    @Override
    public void init() {
        this.exempt = false;
    }

    @Override
    public void tick() {
        this.exempt = false;
    }
}