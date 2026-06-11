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
    private boolean serverOnGround = false;
    private Vec3d serverPosition = null;
    private Vec3d lastServerPosition = null;
    private Vec3d serverVelocity = null;
    private float serverYaw = Float.NaN;
    private float serverPitch = Float.NaN;

    public Packet<?> modifyPacket(Packet<?> packet) {
        return packet;
    }
    protected void onPacket(Packet<?> packet) {}
    public void init() {}
    public void tick() {}

    protected Vec3d position() {
        if(this.serverPosition == null) return Vec3d.ZERO;
        return this.serverPosition;
    }

    protected Vec3d lastPosition() {
        return this.lastServerPosition;
    }
    protected Vec3d velocity() {
        if(this.serverVelocity == null) return Vec3d.ZERO;
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
            case PlayerMoveC2SPacket c2sMove -> {
                if(RubyClient.client.player == null) return;

                this.serverYaw = c2sMove.getYaw(RubyClient.client.player.getYaw());
                this.serverPitch = c2sMove.getPitch(RubyClient.client.player.getPitch());
                this.serverOnGround = c2sMove.isOnGround();
                this.lastServerPosition = this.serverPosition;
                this.serverPosition = new Vec3d(
                        c2sMove.getX(RubyClient.client.player.getX()),
                        c2sMove.getY(RubyClient.client.player.getY()),
                        c2sMove.getZ(RubyClient.client.player.getZ())
                );

                this.serverVelocity = this.serverPosition.subtract(this.lastServerPosition);
            }

            case PlayerPositionLookS2CPacket s2cFull -> {
                EntityPosition pos = new EntityPosition(
                        this.position(),
                        this.velocity(),
                        this.yaw(), this.pitch()
                );

                EntityPosition newPos = EntityPosition.apply(pos, s2cFull.change(), s2cFull.relatives());

                this.serverYaw = newPos.yaw();
                this.serverPitch = newPos.pitch();
                this.lastServerPosition = this.serverPosition;
                this.serverPosition = newPos.position();
                this.serverVelocity = newPos.deltaMovement();
            }

            case PlayerRotationS2CPacket(float yaw, boolean rYaw, float pitch, boolean rPitch) -> {
                EntityPosition pos = new EntityPosition(
                        this.position(),
                        this.velocity(),
                        this.yaw(), this.pitch()
                );

                EntityPosition newPos = EntityPosition.apply(
                        pos, pos.withRotation(yaw, pitch),
                        PositionFlag.ofRot(rYaw, rPitch)
                );

                this.serverYaw = newPos.yaw();
                this.serverPitch = newPos.pitch();
            }

            default -> {}
        }

        this.onPacket(packet);
    }
}
