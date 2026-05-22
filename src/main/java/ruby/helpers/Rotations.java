package ruby.helpers;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;

public class Rotations {
    public static void serverLookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        Vec3d pos = anchorPoint.positionAt(RubyClient.client.player);

        double offX = target.getX() - pos.getX();
        double offY = target.getY() - pos.getY();
        double offZ = target.getZ() - pos.getZ();
        double horizontalDist = Math.sqrt(offX * offX + offZ * offZ);

        RubyClient.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(offZ, offX)) - 90f),
                MathHelper.wrapDegrees((float) Math.toDegrees(-Math.atan2(offY, horizontalDist))),
                RubyClient.client.player.isOnGround(),
                RubyClient.client.player.horizontalCollision
        ));
    }
}
