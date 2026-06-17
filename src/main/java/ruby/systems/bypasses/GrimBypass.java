package ruby.systems.bypasses;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Nullables;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ruby.RubyClient;
import ruby.helpers.PlayerInteractEntity;

import java.util.concurrent.atomic.AtomicReference;

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

        if(from.changesPosition()) return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, ground, horizC);
        return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, ground, horizC);
    }

    private boolean isAsync(Packet<?> packet) {
        return packet instanceof KeepAliveC2SPacket ||
                packet instanceof ResourcePackStatusC2SPacket ||
                packet instanceof AcknowledgeChunksC2SPacket;
    }

    private boolean isTransaction(Packet<?> packet) {
        return packet instanceof CommonPongC2SPacket;
    }

    private boolean isFlying(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket move &&
                (move.changesPosition() || move.changesLook());
    }


    private Packet<?> aimModulo360(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move) || !move.changesLook()) return packet;

        // Grim only checks the range (-360, 360) for yaw so we just force outsize that range
        float yaw = move.getYaw(this.yaw()) % 360;
        if(yaw < 0) yaw += 360;
        yaw += 360;

        return GrimBypass.copyMove(
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

        // Duplicate rotation: strip look but keep position. Never drop ground-only updates.
        if(move.changesPosition()) {
            return new PlayerMoveC2SPacket.PositionAndOnGround(
                    move.getX(this.position().getX()),
                    move.getY(this.position().getY()),
                    move.getZ(this.position().getZ()),
                    move.isOnGround(), move.horizontalCollision()
            );
        }

        if(move.isOnGround() != this.onGround())
            return packet;

        return null;
    }

    private Packet<?> badPacketsA(Packet<?> packet) {
        if(!(packet instanceof UpdateSelectedSlotC2SPacket slot)) return packet;
        if(slot.getSelectedSlot() != this.lastSlot) return packet;
        return null;
    }

    private Packet<?> badPacketsC(Packet<?> packet) {
        if(RubyClient.client == null || RubyClient.client.player == null) return packet;
        if(!RubyClient.client.player.isSleeping()) return packet;
        if(!(packet instanceof ClientCommandC2SPacket cmd)) return packet;
        if(cmd.getMode() != ClientCommandC2SPacket.Mode.STOP_SLEEPING) return packet;
        return null;
    }

    private Packet<?> badPacketsD(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket move) || !move.changesLook()) return packet;

        // lock pitch so grim doesnt whine
        return GrimBypass.copyMove(
                move,
                move.getX(this.position().getX()),
                move.getY(this.position().getY()),
                move.getZ(this.position().getZ()),
                move.getYaw(this.yaw()),
                Math.clamp(move.getPitch(this.pitch()), -90, 90)
        );
    }

    // Fixed but only while not sending rotation packets
    private Packet<?> badPacketsJ(Packet<?> packet) {
        if(packet instanceof PlayerInteractItemC2SPacket interact) {
            if (RubyClient.client.player == null) return packet;
            return new PlayerInteractItemC2SPacket(interact.getHand(),interact.getSequence(),this.yaw(),this.pitch());
        }
        return packet;
    }

    private Packet<?> badPacketsL(Packet<?> packet) {
        if(!(packet instanceof PlayerActionC2SPacket action)) return packet;
        if(action.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return packet;
        if(action.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) return packet;
        if(action.getAction() == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) return packet;

        return new PlayerActionC2SPacket(action.getAction(), BlockPos.ORIGIN, Direction.DOWN, 0);
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

    private boolean sentAttack;
    private boolean sentSwingSinceLastAttack;
    private Packet<?> packetOrderB(Packet<?> packet) {
        if(packet instanceof HandSwingC2SPacket) {
            this.sentSwingSinceLastAttack = true;
            this.sentAttack = false;
            return packet;
        }

        if(packet instanceof PlayerInteractEntityC2SPacket interact) {
            AtomicReference<PlayerInteractEntity.Type> type = new AtomicReference<>(null);
            interact.handle(new PlayerInteractEntity(type));
            if(type.get() != PlayerInteractEntity.Type.ATTACK) return packet;

            this.sentAttack = true;
            if(!this.sentSwingSinceLastAttack) {
                this.connection.send(new HandSwingC2SPacket(Hand.MAIN_HAND)); // flag
                this.sentAttack = false;
            }

            this.sentSwingSinceLastAttack = false;
            return packet;
        }

        if(!this.isAsync(packet) && this.sentAttack) {
            this.connection.send(new HandSwingC2SPacket(Hand.MAIN_HAND)); // flag
            this.sentAttack = false;
        }

        return packet;
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
        Packet<?> modified = packet;

        modified = this.aimModulo360(modified);
        modified = this.badPacketsD(modified);
        modified = this.badPacketsJ(modified);
        modified = this.badPacketsL(modified);
        modified = this.packetOrderB(modified);
        modified = this.packetOrderO(modified);

        modified = this.aimDuplicateLook(modified);
        modified = this.badPacketsA(modified);
        modified = this.badPacketsC(modified);

        return modified;
    }

    @Override
    public void init() {
        this.teleportExempt = false;
        this.flyingSinceTickEnd = false;
        this.injectingTickEnd = false;
        this.lastSlot = -1;
        this.sentAttack = false;
        this.sentSwingSinceLastAttack = true;
    }

    @Override
    public void tick() {
        this.teleportExempt = false;
    }
}
