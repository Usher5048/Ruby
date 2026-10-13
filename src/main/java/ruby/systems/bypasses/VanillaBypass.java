package ruby.systems.bypasses;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.systems.gui.Toasts;

import java.util.ArrayList;
import java.util.List;

public class VanillaBypass extends Bypass {
    private int airTime;
    private boolean resetAir = false;
    private int movePacketsThisTick = 0;
    private final ArrayList<List<Packet<?>>> queue = new ArrayList<>(); // ass

    private ServerboundMovePlayerPacket getMovePacket(ServerboundMovePlayerPacket original, Vec3 pos) {
        return this.getMovePacket(original, pos.x(), pos.y(), pos.z());
    }

    private ServerboundMovePlayerPacket getMovePacket(
            ServerboundMovePlayerPacket original,
            double x, double y, double z
    ) {
        float yaw = original.getYRot(this.yaw());
        float pitch = original.getXRot(this.pitch());
        boolean horizC = original.horizontalCollision();
        boolean ground = original.isOnGround();
        if(!original.hasRotation()) return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, ground, horizC);
        else return new ServerboundMovePlayerPacket.Pos(x, y, z, ground, horizC);
    }

    public Packet<?> bypassFlight(Packet<?> packet) {
        if(this.airTime < 40 && !this.resetAir) return packet;
        if(RubyClient.client.player == null) return packet;
        if(!(packet instanceof ServerboundMovePlayerPacket movePacket)) return packet;
        if(!movePacket.hasPosition()) return packet;

        boolean horizC = RubyClient.client.player.horizontalCollision;
        float yaw = movePacket.getYRot(RubyClient.client.player.getYRot());
        float pitch = movePacket.getXRot(RubyClient.client.player.getXRot());

        double x = RubyClient.client.player.getX();
        double y = RubyClient.client.player.getY() + (this.resetAir ? 0.1 : -0.1);
        double z = RubyClient.client.player.getZ();
        boolean ground = !this.resetAir && movePacket.isOnGround();

        this.resetAir = !this.resetAir;
        if(this.resetAir) this.airTime = 0;

        RubyClient.client.player.setPos(x, y, z);

        if(!movePacket.hasRotation()) return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, ground, horizC);
        else return new ServerboundMovePlayerPacket.Pos(x, y, z, ground, horizC);
    }

    public Packet<?> bypassMovedToQuickly(Packet<?> packet) {
        if(!(packet instanceof ServerboundMovePlayerPacket movePacket)) return packet;
        if(!movePacket.hasPosition()) return packet;
        if(RubyClient.client.player == null) return packet;
        this.movePacketsThisTick++;

        Vec3 goal = new Vec3(
                movePacket.getX(0),
                movePacket.getY(0),
                movePacket.getZ(0)
        );

        Vec3 attemptedJump = goal.subtract(this.position());

        double serverThresh = RubyClient.client.player.isFallFlying() ? 300 : 100;
        if(attemptedJump.lengthSqr() < serverThresh) return packet;
        this.movePacketsThisTick--; // if we get here, we canceled the packet

        double factor = Math.sqrt(serverThresh / attemptedJump.lengthSqr());
        Vec3 deltaPerPacket = attemptedJump.scale(factor); // max jump per packet

        // Gap too big for single tick: do max tick, queue next tick
        if(attemptedJump.lengthSqr() > Math.max(1, 5 - this.movePacketsThisTick) * serverThresh) {
            this.queue.add(List.of(packet));

            RubyClient.notifyUser("Requeuing " + attemptedJump.length() + " using " + (5 - this.movePacketsThisTick) + " temp packets");

            Vec3 pos = this.position();
            for(int i = this.movePacketsThisTick; i < 5; i++) {
                pos = pos.add(deltaPerPacket);
                this.connection.send(this.getMovePacket(movePacket, pos));
            }

            return null;
        }

        // split gap into packets the server will actually accept
        RubyClient.notifyUser("Sending " + (5 - this.movePacketsThisTick) + " to step " + attemptedJump.length() + " blocks");
        Vec3 start = this.position();
        for(int i = 0; i < 5 - this.movePacketsThisTick; i++) {
            double t = (i + 1.0) / (5.0 - this.movePacketsThisTick);
            double x = start.x() + (goal.x() - start.x()) * t;
            double y = start.y() + (goal.y() - start.y()) * t;
            double z = start.z() + (goal.z() - start.z()) * t;
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
                this.position().y() < this.lastPosition().y() ||
                RubyClient.client.player.onGround()
        ) this.airTime = 0;
    }
}