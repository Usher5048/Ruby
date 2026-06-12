package ruby.helpers;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.bypasses.GrimBypass;

public class Rotations {
    public static void serverLookAt(EntityAnchorArgumentType.EntityAnchor anchor, Vec3d target) {
        if(RubyClient.client.player == null || RubyClient.client.getNetworkHandler() == null) return;

        float[] rotation = rotationTo(anchor, target);

        if(Bypasses.get() instanceof GrimBypass) {
            RotationManager.setTarget(rotation[0], rotation[1]);
            return;
        }

        var player = RubyClient.client.player;
        RubyClient.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                rotation[0], rotation[1], player.isOnGround(), player.horizontalCollision
        ));
    }

    public static void setTarget(float yaw, float pitch) {
        if(Bypasses.get() instanceof GrimBypass) {
            RotationManager.snapTo(yaw, pitch);
            return;
        }
        serverLookAt(EntityAnchorArgumentType.EntityAnchor.EYES,
                RubyClient.client.player.getEyePos().add(
                        Vec3d.fromPolar(pitch, yaw).multiply(10)
                ));
    }

    public static float[] rotationTo(EntityAnchorArgumentType.EntityAnchor anchor, Vec3d target) {
        Vec3d eye = anchor.positionAt(RubyClient.client.player);
        double dx = target.getX() - eye.getX();
        double dy = target.getY() - eye.getY();
        double dz = target.getZ() - eye.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = MathHelper.wrapDegrees((float) Math.toDegrees(-Math.atan2(dy, horiz)));
        return new float[] { yaw, pitch };
    }
}
