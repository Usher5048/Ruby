package ruby.systems.bypasses;

import net.minecraft.entity.EntityPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;

public class Bypass {
    protected static class Connection {
        private ClientConnection clientConnection;

        protected void send(Packet<?> packet) {
            this.clientConnection.send(packet, null);
        }
        protected void resend(Packet<?> packet) {
            this.clientConnection.send(packet);
        }
    }

    protected final Connection connection = new Connection();

    private boolean movementTick = false;
    private boolean serverOnGround = false;
    private Vec3d serverPosition = Vec3d.ZERO;
    private Vec3d lastServerPosition = Vec3d.ZERO;
    private Vec3d serverVelocity = Vec3d.ZERO;
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
        this.serverPosition = Vec3d.ZERO;
        this.lastServerPosition = Vec3d.ZERO;
        this.serverVelocity = Vec3d.ZERO;
        this.serverYaw = Float.NaN;
        this.serverPitch = Float.NaN;
        this.lastPacketTeleport = false;
        this.setTeleport = false;

        this.onInit();
    }

    public void tick() {}

    public Vec3d position() {
        return this.serverPosition != null ? this.serverPosition : Vec3d.ZERO;
    }
    public Vec3d lastPosition() {
        return this.lastServerPosition != null ? this.lastServerPosition : this.position();
    }

    public Vec3d velocity() {
        return this.serverVelocity;
    }
    public boolean onGround() {
        return this.serverOnGround;
    }
    public float yaw() {
        if(Float.isNaN(this.serverYaw) && RubyClient.client.player != null)
            return RubyClient.client.player.getYaw();
        return this.serverYaw;
    }

    public float pitch() {
        if(Float.isNaN(this.serverPitch) && RubyClient.client.player != null)
            return RubyClient.client.player.getPitch();
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

    public void resetConnection(ClientConnection connection) {
        this.connection.clientConnection = connection;
    }
    public void updateServer(Packet<?> packet) {
        if(packet.getPacketType().side() == NetworkSide.SERVERBOUND)
            this.lastPacketTeleport = false;

        switch(packet) {
            case ClientTickEndC2SPacket tick -> this.lastServerPosition = this.serverPosition;
            case PlayerMoveC2SPacket move -> {
                if(this.setTeleport) {
                    this.lastPacketTeleport = true;
                    this.setTeleport = false;
                }

                this.serverOnGround = move.isOnGround();
                if(move.changesLook()) {
                    this.serverYaw = move.getYaw(0);
                    this.serverPitch = move.getPitch(0);
                }

                if(move.changesPosition()) {
                    this.serverPosition = new Vec3d(
                            move.getX(0),
                            move.getY(0),
                            move.getZ(0)
                    );

                    if(this.lastServerPosition == null) this.lastServerPosition = this.serverPosition;
                    this.serverVelocity = this.serverPosition.subtract(this.lastServerPosition);
                }
            }

            case TeleportConfirmC2SPacket teleport -> this.setTeleport = true;
            case PlayerPositionLookS2CPacket teleport -> {
                EntityPosition current = new EntityPosition(
                        this.position(), this.velocity(), this.yaw(), this.pitch()
                );
                EntityPosition next = EntityPosition.apply(current, teleport.change(), teleport.relatives());
                this.serverYaw = next.yaw();
                this.serverPitch = next.pitch();
                this.serverPosition = next.position();
                this.serverVelocity = next.deltaMovement();
            }

            case PlayerRotationS2CPacket(float yaw, boolean rYaw, float pitch, boolean rPitch) -> {
                EntityPosition current = new EntityPosition(
                        this.position(), this.velocity(), this.yaw(), this.pitch()
                );
                EntityPosition next = EntityPosition.apply(
                        current, current.withRotation(yaw, pitch), PositionFlag.ofRot(rYaw, rPitch)
                );
                this.serverYaw = next.yaw();
                this.serverPitch = next.pitch();
            }

            default -> {}
        }

        this.onPacket(packet);
    }
}
