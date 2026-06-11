package ruby.helpers;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;

public class Rotations {
    public static void serverLookAt(EntityAnchorArgumentType.EntityAnchor anchor, Vec3d target) {
        if(RubyClient.client.player == null || RubyClient.client.getNetworkHandler() == null) return;

        Vec3d eye = anchor.positionAt(RubyClient.client.player);
        double dx = target.getX() - eye.getX();
        double dy = target.getY() - eye.getY();
        double dz = target.getZ() - eye.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = MathHelper.wrapDegrees((float) Math.toDegrees(-Math.atan2(dy, horiz)));

        var player = RubyClient.client.player;
        RubyClient.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, player.isOnGround(), player.horizontalCollision
        ));
    }
}
