package ruby.systems.bypasses;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import ruby.RubyClient;

public class GrimBypass extends Bypass {
    private boolean teleportExempt;
    private boolean flyingSinceTickEnd;
    private boolean injectingTickEnd;
    private int lastSlot = -1;

    private static PlayerMoveC2SPacket copyMove(
            PlayerMoveC2SPacket from,
            double x, double y, double z,
            float yaw, float pitch
    ) {
        boolean ground = from.isOnGround();
        boolean horizC = from.horizontalCollision();
        if(from.changesPosition() && from.changesLook())
            return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground, horizC);
        if(from.changesPosition())
            return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, ground, horizC);
        return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, ground, horizC);
    }

    private Packet<?> aimModulo360(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move) || !move.changesLook()) return packet;

        float yaw = move.getYaw(this.yaw()) % 360;
        if(yaw < 0) yaw += 360;
        yaw += 360;

        return copyMove(
                move,
                move.getX(this.position().getX()),
                move.getY(this.position().getY()),
                move.getZ(this.position().getZ()),
                yaw, move.getPitch(this.pitch())
        );
    }

    private Packet<?> aimDuplicateLook(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move) || !move.changesLook()) return packet;

        if(this.teleportExempt) {
            this.teleportExempt = false;
            return packet;
        }

        if(move.getYaw(this.yaw()) != this.yaw() || move.getPitch(this.pitch()) != this.pitch())
            return packet;

        return null;
    }

    private Packet<?> badPacketsA(Packet<?> packet) {
        if(packet instanceof UpdateSelectedSlotC2SPacket slot && slot.getSelectedSlot() == this.lastSlot)
            return null;
        return packet;
    }

    private Packet<?> badPacketsC(Packet<?> packet) {
        if(RubyClient.client == null || RubyClient.client.player == null) return packet;
        if(!RubyClient.client.player.isSleeping()) return packet;
        if(packet instanceof ClientCommandC2SPacket cmd
                && cmd.getMode() == ClientCommandC2SPacket.Mode.STOP_SLEEPING)
            return null;
        return packet;
    }

    private Packet<?> badPacketsD(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move) || !move.changesLook()) return packet;

        return copyMove(
                move,
                move.getX(this.position().getX()),
                move.getY(this.position().getY()),
                move.getZ(this.position().getZ()),
                move.getYaw(this.yaw()),
                Math.clamp(move.getPitch(this.pitch()), -90, 90)
        );
    }

    private boolean isAsync(Packet<?> packet) {
        return packet instanceof KeepAliveC2SPacket
                || packet instanceof ResourcePackStatusC2SPacket
                || packet instanceof AcknowledgeChunksC2SPacket;
    }

    private boolean isVehicleSprint(Packet<?> packet) {
        if(RubyClient.client == null || RubyClient.client.player == null || !RubyClient.client.player.hasVehicle())
            return false;
        if(!(packet instanceof ClientCommandC2SPacket cmd)) return false;
        ClientCommandC2SPacket.Mode mode = cmd.getMode();
        return mode == ClientCommandC2SPacket.Mode.START_SPRINTING
                || mode == ClientCommandC2SPacket.Mode.STOP_SPRINTING;
    }

    private void sendTickEnd() {
        if(!this.flyingSinceTickEnd || this.injectingTickEnd || this.connection == null) return;

        this.injectingTickEnd = true;
        try {
            this.connection.send(ClientTickEndC2SPacket.INSTANCE);
            this.flyingSinceTickEnd = false;
        } finally {
            this.injectingTickEnd = false;
        }
    }

    private Packet<?> packetOrderO(Packet<?> packet) {
        if(packet instanceof ClientTickEndC2SPacket) {
            this.flyingSinceTickEnd = false;
            return packet;
        }

        if(packet instanceof PlayerMoveC2SPacket) {
            if(!this.teleportExempt) this.flyingSinceTickEnd = true;
            return packet;
        }

        if(this.isAsync(packet) || packet instanceof VehicleMoveC2SPacket || this.isVehicleSprint(packet))
            return packet;

        if(this.flyingSinceTickEnd) this.sendTickEnd();
        return packet;
    }

    @Override
    protected void onPacket(Packet<?> packet) {
        if(packet instanceof PlayerPositionLookS2CPacket) this.teleportExempt = true;
        if(packet instanceof UpdateSelectedSlotC2SPacket slot) this.lastSlot = slot.getSelectedSlot();
        if(packet instanceof UpdateSelectedSlotS2CPacket slot) this.lastSlot = slot.slot();
    }

    @Override
    public Packet<?> modifyPacket(Packet<?> packet) {
        Packet<?> out = this.aimModulo360(packet);
        out = this.badPacketsD(out);
        out = this.aimDuplicateLook(out);
        if(out == null) return null;

        out = this.packetOrderO(out);
        out = this.badPacketsA(out);
        if(out == null) return null;

        return this.badPacketsC(out);
    }

    @Override
    public void init() {
        this.teleportExempt = false;
        this.flyingSinceTickEnd = false;
        this.injectingTickEnd = false;
        this.lastSlot = -1;
    }

    @Override
    public void tick() {
        this.teleportExempt = false;
    }
}
