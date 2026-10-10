package ruby.systems.bypasses;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import ruby.RubyClient;

public class GrimBypass extends Bypass {
    private boolean teleportExempt;
    private boolean flyingSinceTickEnd;
    private boolean injectingTickEnd;
    private int lastSlot = -1;

    private static ServerboundMovePlayerPacket copyMove(
            ServerboundMovePlayerPacket from,
            double x, double y, double z,
            float yaw, float pitch
    ) {
        boolean ground = from.isOnGround();
        boolean horizC = from.horizontalCollision();
        if(from.hasPosition() && from.hasRotation())
            return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, ground, horizC);

        if(from.hasPosition()) return new ServerboundMovePlayerPacket.Pos(x, y, z, ground, horizC);
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, ground, horizC);
    }

    private boolean isAsync(Packet<?> packet) {
        return packet instanceof ServerboundKeepAlivePacket ||
                packet instanceof ServerboundResourcePackPacket ||
                packet instanceof ServerboundChunkBatchReceivedPacket;
    }

    private boolean isFlying(Packet<?> packet) {
        return packet instanceof ServerboundMovePlayerPacket move &&
                (move.hasPosition() || move.hasRotation());
    }

    private boolean isTickPacketIncludingMovement(Packet<?> packet) {
        if(packet instanceof ServerboundClientTickEndPacket && !this.sentMovementThisTick())
            return true;

        return this.isFlying(packet);
    }

    private boolean isTickPacket(Packet<?> packet) {
        if(this.isTickPacketIncludingMovement(packet)) {
            if(this.isFlying(packet)) return !this.lastPacketWasTeleport();
            return true;
        }

        return false;
    }

    private boolean isSequenced(Packet<?> packet) {
        if(
                packet instanceof ServerboundUseItemPacket ||
                packet instanceof ServerboundUseItemOnPacket
        ) return true;

        if(packet instanceof ServerboundPlayerActionPacket action) {
            return action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK ||
                    action.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
                    action.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
        }

        return false;
    }


    private Packet<?> aimModulo360(Packet<?> packet) {
        if(!(packet instanceof ServerboundMovePlayerPacket move) || !move.hasRotation()) return packet;

        // Grim only checks the range (-360, 360) for yaw so we just force outsize that range
        float yaw = move.getYRot(this.yaw()) % 360;
        if(yaw < 0) yaw += 360;
        yaw += 360;

        return GrimBypass.copyMove(
                move,
                move.getX(this.position().x()),
                move.getY(this.position().y()),
                move.getZ(this.position().z()),
                yaw, move.getXRot(this.pitch())
        );
    }

    private Packet<?> aimDuplicateLook(Packet<?> packet) {
        if(!(packet instanceof ServerboundMovePlayerPacket move) || !move.hasRotation()) return packet;

        if(this.teleportExempt) {
            this.teleportExempt = false;
            return packet;
        }

        if(move.getYRot(this.yaw()) != this.yaw() || move.getXRot(this.pitch()) != this.pitch())
            return packet;

        // Duplicate rotation: strip look but keep position. Never drop ground-only updates.
        if(move.hasPosition()) {
            return new ServerboundMovePlayerPacket.Pos(
                    move.getX(this.position().x()),
                    move.getY(this.position().y()),
                    move.getZ(this.position().z()),
                    move.isOnGround(), move.horizontalCollision()
            );
        }

        if(move.isOnGround() != this.onGround())
            return packet;

        return null;
    }

    private Packet<?> badPacketsA(Packet<?> packet) {
        if(!(packet instanceof ServerboundSetCarriedItemPacket slot)) return packet;
        if(slot.getSlot() != this.lastSlot) return packet;
        return null;
    }

    private Packet<?> badPacketsC(Packet<?> packet) {
        if(RubyClient.client.player == null) return packet;
        if(!RubyClient.client.player.isSleeping()) return packet;
        if(!(packet instanceof ServerboundPlayerCommandPacket cmd)) return packet;
        if(cmd.getAction() != ServerboundPlayerCommandPacket.Action.STOP_SLEEPING) return packet;
        return null;
    }

    private Packet<?> badPacketsD(Packet<?> packet) {
        if(!(packet instanceof ServerboundMovePlayerPacket move) || !move.hasRotation()) return packet;

        // lock pitch so grim doesnt whine
        return GrimBypass.copyMove(
                move,
                move.getX(this.position().x()),
                move.getY(this.position().y()),
                move.getZ(this.position().z()),
                move.getYRot(this.yaw()),
                Math.clamp(move.getXRot(this.pitch()), -90, 90)
        );
    }

    private ServerboundUseItemPacket queued = null;
    private Packet<?> badPacketsJ(Packet<?> packet) {
        if(RubyClient.client.player == null) return packet;

        if(packet instanceof ServerboundMovePlayerPacket move && this.queued != null) {
            if(!move.hasRotation()) return packet;
            this.connection.send(new ServerboundUseItemPacket(
                    this.queued.hand(),
                    this.queued.sequence(),
                    move.getYRot(0),
                    move.getXRot(0)
            ));

            this.queued = null;
            return packet;
        }

        if(packet instanceof ServerboundUseItemPacket interact) {
            if(!this.sentMovementThisTick()) {
                this.queued = interact;
                return null;
            }

            return new ServerboundUseItemPacket(
                    interact.hand(),
                    interact.sequence(),
                    this.yaw(),
                    this.pitch()
            );
        }

        // TODO: This still flags it, but delaying sequence is worse, fix
        if(this.isSequenced(packet) && this.queued != null) {
            this.connection.send(this.queued);
            this.queued = null;
        }

        return packet;
    }

    private Packet<?> badPacketsL(Packet<?> packet) {
        if(!(packet instanceof ServerboundPlayerActionPacket action)) return packet;
        if(action.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return packet;
        if(action.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) return packet;
        if(action.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) return packet;

        return new ServerboundPlayerActionPacket(action.getAction(), BlockPos.ZERO, Direction.DOWN, 0);
    }

    private boolean isVehicleSprint(Packet<?> packet) {
        if(RubyClient.client.player == null || !RubyClient.client.player.canControlVehicle())
            return false;
        if(!(packet instanceof ServerboundPlayerCommandPacket cmd)) return false;
        ServerboundPlayerCommandPacket.Action mode = cmd.getAction();
        return mode == ServerboundPlayerCommandPacket.Action.START_SPRINTING
                || mode == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
    }

    private void sendTickEnd() {
        if(!this.flyingSinceTickEnd || this.injectingTickEnd) return;

        this.injectingTickEnd = true;
        try {
            this.connection.send(ServerboundClientTickEndPacket.INSTANCE);
            this.flyingSinceTickEnd = false;
        } finally {
            this.injectingTickEnd = false;
        }
    }

    private boolean sentAttack;
    private boolean sentSwingSinceLastAttack;
    private Packet<?> packetOrderB(Packet<?> packet) {
        if(packet instanceof ServerboundPunchPacket) {
            this.sentSwingSinceLastAttack = true;
            this.sentAttack = false;
            return packet;
        }

        if(packet instanceof ServerboundInteractPacket interact) {
            if(interact.usingSecondaryAction()) return packet;

            this.sentAttack = true;
            if(!this.sentSwingSinceLastAttack) {
                this.connection.send(new ServerboundPunchPacket()); // flag
                this.sentAttack = false;
            }

            this.sentSwingSinceLastAttack = false;
            return packet;
        }

        if(!this.isAsync(packet) && this.sentAttack) {
            this.connection.send(new ServerboundPunchPacket()); // flag
            this.sentAttack = false;
        }

        return packet;
    }

    private Packet<?> packetOrderO(Packet<?> packet) {
        if(packet instanceof ServerboundClientTickEndPacket) {
            this.flyingSinceTickEnd = false;
            return packet;
        }

        if(packet instanceof ServerboundMovePlayerPacket) {
            if(!this.teleportExempt) this.flyingSinceTickEnd = true;
            return packet;
        }

        if(this.isAsync(packet) || packet instanceof ServerboundMoveVehiclePacket || this.isVehicleSprint(packet))
            return packet;

        if(this.flyingSinceTickEnd) this.sendTickEnd();
        return packet;
    }

    @Override
    protected void onPacket(Packet<?> packet) {
        if(packet instanceof ClientboundPlayerPositionPacket) this.teleportExempt = true;
        if(packet instanceof ServerboundSetCarriedItemPacket slot) this.lastSlot = slot.getSlot();
        if(packet instanceof ClientboundSetHeldSlotPacket(int slot)) this.lastSlot = slot;
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
    public void onInit() {
        this.teleportExempt = false;
        this.flyingSinceTickEnd = false;
        this.injectingTickEnd = false;
        this.lastSlot = -1;
        this.sentAttack = false;
        this.sentSwingSinceLastAttack = true;
        this.queued = null;
    }

    @Override
    public void onTick() {
        this.teleportExempt = false;
    }
}
