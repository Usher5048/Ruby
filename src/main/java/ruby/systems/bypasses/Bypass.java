package ruby.systems.bypasses;

import net.minecraft.entity.EntityPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;

public class Bypass {
    protected ClientConnection connection;
    private boolean serverOnGround;
    private Vec3d serverPosition;
    private Vec3d lastServerPosition;
    private Vec3d serverVelocity = Vec3d.ZERO;
    private float serverYaw = Float.NaN;
    private float serverPitch = Float.NaN;

    public Packet<?> modifyPacket(Packet<?> packet) {
        return packet;
    }

    protected void onPacket(Packet<?> packet) {}

    public void init() {}

    public void tick() {}

    protected Vec3d position() {
        return this.serverPosition != null ? this.serverPosition : Vec3d.ZERO;
    }

    protected Vec3d lastPosition() {
        return this.lastServerPosition != null ? this.lastServerPosition : this.position();
    }

    protected Vec3d velocity() {
        return this.serverVelocity;
    }

    protected boolean onGround() {
        return this.serverOnGround;
    }

    protected float yaw() {
        if(Float.isNaN(this.serverYaw) && RubyClient.client.player != null)
            return RubyClient.client.player.getYaw();
        return this.serverYaw;
    }

    protected float pitch() {
        if(Float.isNaN(this.serverPitch) && RubyClient.client.player != null)
            return RubyClient.client.player.getPitch();
        return this.serverPitch;
    }

    public void resetConnection(ClientConnection connection) {
        this.connection = connection;
    }

    public void updateServer(Packet<?> packet) {
        switch(packet) {
            case PlayerMoveC2SPacket move -> {
                if(RubyClient.client.player == null) return;

                if(move.changesLook()) {
                    this.serverYaw = move.getYaw(RubyClient.client.player.getYaw());
                    this.serverPitch = move.getPitch(RubyClient.client.player.getPitch());
                }
                this.serverOnGround = move.isOnGround();

                if(move.changesPosition()) {
                    Vec3d next = new Vec3d(
                            move.getX(RubyClient.client.player.getX()),
                            move.getY(RubyClient.client.player.getY()),
                            move.getZ(RubyClient.client.player.getZ())
                    );
                    if(this.serverPosition != null) this.serverVelocity = next.subtract(this.serverPosition);
                    this.lastServerPosition = this.serverPosition;
                    this.serverPosition = next;
                }
            }

            case PlayerPositionLookS2CPacket teleport -> {
                EntityPosition current = new EntityPosition(
                        this.position(), this.velocity(), this.yaw(), this.pitch()
                );
                EntityPosition next = EntityPosition.apply(current, teleport.change(), teleport.relatives());
                this.serverYaw = next.yaw();
                this.serverPitch = next.pitch();
                this.lastServerPosition = this.serverPosition;
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
