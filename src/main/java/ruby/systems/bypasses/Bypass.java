package ruby.systems.bypasses;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;

public class Bypass {
    protected static class WrappedConnection {
        private Connection connection;

        protected void send(Packet<?> packet) {
            this.connection.send(packet, null);
        }
        protected void resend(Packet<?> packet) {
            this.connection.send(packet);
        }
    }

    protected final WrappedConnection connection = new WrappedConnection();

    private boolean movementTick = false;
    private boolean serverOnGround = false;
    private Vec3 serverPosition = Vec3.ZERO;
    private Vec3 lastServerPosition = Vec3.ZERO;
    private Vec3 serverVelocity = Vec3.ZERO;
    private float serverYaw = Float.NaN;
    private float serverPitch = Float.NaN;
    private boolean lastPacketTeleport = false;
    private boolean setTeleport = false;

    public Packet<?> modifyPacket(Packet<?> packet) {
        return packet;
    }

    protected void onPacket(Packet<?> packet) {}
    protected void onInit() {}
    protected void onTick() {}

    public void init() {
        this.movementTick = false;
        this.serverOnGround = false;
        this.serverPosition = Vec3.ZERO;
        this.lastServerPosition = Vec3.ZERO;
        this.serverVelocity = Vec3.ZERO;
        this.serverYaw = Float.NaN;
        this.serverPitch = Float.NaN;
        this.lastPacketTeleport = false;
        this.setTeleport = false;

        this.onInit();
    }

    public void tick() {}

    public Vec3 position() {
        return this.serverPosition != null ? this.serverPosition : Vec3.ZERO;
    }
    public Vec3 lastPosition() {
        return this.lastServerPosition != null ? this.lastServerPosition : this.position();
    }

    public Vec3 velocity() {
        return this.serverVelocity;
    }
    public boolean onGround() {
        return this.serverOnGround;
    }
    public float yaw() {
        if(Float.isNaN(this.serverYaw) && RubyClient.client.player != null)
            return RubyClient.client.player.getYRot();
        return this.serverYaw;
    }

    public float pitch() {
        if(Float.isNaN(this.serverPitch) && RubyClient.client.player != null)
            return RubyClient.client.player.getXRot();
        return this.serverPitch;
    }

    public boolean sentMovementThisTick() {
        return this.movementTick;
    }
    public boolean lastPacketWasTeleport() {
        return this.lastPacketTeleport;
    }
    public void setMovementTick(boolean val) {
        this.movementTick = val;
    }

    public void resetConnection(Connection connection) {
        this.connection.connection = connection;
    }
    public void updateServer(Packet<?> packet) {
        if(packet.type().flow() == PacketFlow.SERVERBOUND)
            this.lastPacketTeleport = false;

        switch(packet) {
            case ServerboundClientTickEndPacket tick -> this.lastServerPosition = this.serverPosition;
            case ServerboundMovePlayerPacket move -> {
                if(this.setTeleport) {
                    this.lastPacketTeleport = true;
                    this.setTeleport = false;
                }

                this.serverOnGround = move.isOnGround();
                if(move.hasRotation()) {
                    this.serverYaw = move.getYRot(0);
                    this.serverPitch = move.getXRot(0);
                }

                if(move.hasPosition()) {
                    this.serverPosition = new Vec3(
                            move.getX(0),
                            move.getY(0),
                            move.getZ(0)
                    );

                    if(this.lastServerPosition == null) this.lastServerPosition = this.serverPosition;
                    this.serverVelocity = this.serverPosition.subtract(this.lastServerPosition);
                }
            }

            case ServerboundAcceptTeleportationPacket teleport -> this.setTeleport = true;
            case ClientboundPlayerPositionPacket teleport -> {
                PositionMoveRotation current = new PositionMoveRotation(
                        this.position(), this.velocity(), this.yaw(), this.pitch()
                );

                PositionMoveRotation next = PositionMoveRotation.calculateAbsolute(current, teleport.change(), teleport.relatives());
                this.serverYaw = next.yRot();
                this.serverPitch = next.xRot();
                this.serverPosition = next.position();
                this.serverVelocity = next.deltaMovement();
            }

            case ClientboundPlayerRotationPacket(float yaw, boolean rYaw, float pitch, boolean rPitch) -> {
                PositionMoveRotation current = new PositionMoveRotation(
                        this.position(), this.velocity(), this.yaw(), this.pitch()
                );

                PositionMoveRotation next = PositionMoveRotation.calculateAbsolute(
                        current,
                        current.withRotation(yaw, pitch),
                        Relative.rotation(rYaw, rPitch)
                );

                this.serverYaw = next.yRot();
                this.serverPitch = next.xRot();
            }

            default -> {}
        }

        this.onPacket(packet);
    }
}
