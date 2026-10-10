package ruby.helpers;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.bypasses.GrimBypass;

public class Rotations {
    public static void serverLookAt(EntityAnchorArgument.Anchor anchor, Vec3 target) {
        if(RubyClient.client.player == null || RubyClient.client.getConnection() == null) return;

        float[] rotation = rotationTo(anchor, target);

        if(Bypasses.get() instanceof GrimBypass) {
            RotationManager.setTarget(rotation[0], rotation[1]);
            return;
        }

        var player = RubyClient.client.player;
        RubyClient.client.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                rotation[0], rotation[1], player.onGround(), player.horizontalCollision
        ));
    }

    public static void setTarget(float yaw, float pitch) {
        if(Bypasses.get() instanceof GrimBypass) {
            RotationManager.snapTo(yaw, pitch);
            return;
        }
        serverLookAt(EntityAnchorArgument.Anchor.EYES,
                RubyClient.client.player.getEyePosition().add(
                        Vec3.directionFromRotation(pitch, yaw).scale(10)
                ));
    }

    public static float[] rotationTo(EntityAnchorArgument.Anchor anchor, Vec3 target) {
        Vec3 eye = anchor.apply(RubyClient.client.player);
        double dx = target.x() - eye.x();
        double dy = target.y() - eye.y();
        double dz = target.z() - eye.z();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = Mth.wrapDegrees((float) Math.toDegrees(-Math.atan2(dy, horiz)));
        return new float[] { yaw, pitch };
    }
}
