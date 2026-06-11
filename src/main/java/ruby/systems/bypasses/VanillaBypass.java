package ruby.systems.bypasses;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;

import java.util.ArrayList;
import java.util.List;

public class VanillaBypass extends Bypass {
    private int airTime;
    private boolean resetAir = false;
    private int movePacketsThisTick = 0;
    private final ArrayList<List<Packet<?>>> queue = new ArrayList<>(); // ass

    private PlayerMoveC2SPacket getMovePacket(PlayerMoveC2SPacket original, Vec3d pos) {
        return this.getMovePacket(original, pos.getX(), pos.getY(), pos.getZ());
    }

    private PlayerMoveC2SPacket getMovePacket(
            PlayerMoveC2SPacket original,
            double x, double y, double z
    ) {
        float yaw = original.getYaw(this.yaw());
        float pitch = original.getPitch(this.pitch());
        boolean horizC = original.horizontalCollision();
        boolean ground = original.isOnGround();
        if(!original.changesLook()) return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground, horizC);
        else return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, ground, horizC);
    }

    public Packet<?> bypassFlight(Packet<?> packet) {
        if(this.airTime < 40 && !this.resetAir) return packet;
        if(RubyClient.client.player == null) return packet;
        if(!(packet instanceof PlayerMoveC2SPacket movePacket)) return packet;
        if(!movePacket.changesPosition()) return packet;

        boolean horizC = RubyClient.client.player.horizontalCollision;
        float yaw = movePacket.getYaw(RubyClient.client.player.getYaw());
        float pitch = movePacket.getPitch(RubyClient.client.player.getPitch());

        double x = RubyClient.client.player.getX();
        double y = RubyClient.client.player.getY() + (this.resetAir ? 0.1 : -0.1);
        double z = RubyClient.client.player.getZ();
        boolean ground = !this.resetAir && movePacket.isOnGround();

        this.resetAir = !this.resetAir;
        if(this.resetAir) this.airTime = 0;

        RubyClient.client.player.setPos(x, y, z);

        if(!movePacket.changesLook()) return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground, horizC);
        else return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, ground, horizC);
    }

    public Packet<?> bypassMovedToQuickly(Packet<?> packet) {
        if(!(packet instanceof PlayerMoveC2SPacket movePacket)) return packet;
        if(!movePacket.changesPosition()) return packet;
        if(RubyClient.client.player == null) return packet;
        this.movePacketsThisTick++;

        Vec3d goal = new Vec3d(
                movePacket.getX(0),
                movePacket.getY(0),
                movePacket.getZ(0)
        );

        Vec3d attemptedJump = goal.subtract(this.position());

        double serverThresh = RubyClient.client.player.isGliding() ? 300 : 100;
        if(attemptedJump.lengthSquared() < serverThresh) return packet;
        this.movePacketsThisTick--; // if we get here, we canceled the packet

        double factor = Math.sqrt(serverThresh / attemptedJump.lengthSquared());
        Vec3d deltaPerPacket = attemptedJump.multiply(factor); // max jump per packet

        // Gap too big for single tick: do max tick, queue next tick
        if(attemptedJump.lengthSquared() > Math.max(1, 5 - this.movePacketsThisTick) * serverThresh) {
            this.queue.add(List.of(packet));

            RubyClient.notifyUser("Requeuing " + attemptedJump.length() + " using " + (5 - this.movePacketsThisTick) + " temp packets");

            Vec3d pos = this.position();
            for(int i = this.movePacketsThisTick; i < 5; i++) {
                pos = pos.add(deltaPerPacket);
                this.connection.send(this.getMovePacket(movePacket, pos));
            }

            return null;
        }

        // split gap into packets the server will actually accept
        RubyClient.notifyUser("Sending " + (5 - this.movePacketsThisTick) + " to step " + attemptedJump.length() + " blocks");
        Vec3d start = this.position();
        for(int i = 0; i < 5 - this.movePacketsThisTick; i++) {
            double t = (i + 1.0) / (5.0 - this.movePacketsThisTick);
            double x = start.getX() + (goal.getX() - start.getX()) * t;
            double y = start.getY() + (goal.getY() - start.getY()) * t;
            double z = start.getZ() + (goal.getZ() - start.getZ()) * t;
            this.connection.send(this.getMovePacket(movePacket, x, y, z));
        }

        return null;
    }

    @Override
    public Packet<?> modifyPacket(Packet<?> packet) {
        packet = this.bypassFlight(packet);
        packet = this.bypassMovedToQuickly(packet);
        return packet;
    }

    @Override
    public void init() {
        this.movePacketsThisTick = 0;
        this.resetAir = false;
        this.queue.clear();
        this.airTime = 0;
    }

    @Override
    public void tick() {
        this.movePacketsThisTick = 0;

        if(!this.queue.isEmpty()) {
            List<Packet<?>> packets = this.queue.removeFirst();
            for(Packet<?> packet : packets) {
                Packet<?> modified = this.modifyPacket(packet);
                if(modified == null) continue;
                this.connection.send(modified);
            }
        }

        if(RubyClient.client.player == null) return;
        this.movePacketsThisTick = 0;

        this.airTime++;
        if(
                this.position().getY() < this.lastPosition().getY() ||
                RubyClient.client.player.isOnGround()
        ) this.airTime = 0;
    }
}