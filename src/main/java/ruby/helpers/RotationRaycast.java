package ruby.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;

public final class RotationRaycast {
    private RotationRaycast() {}

    public static float[] aimAt(Entity entity) {
        if(RubyClient.client.player == null) return new float[] { 0f, 0f };
        Vec3d eye = RubyClient.client.player.getEyePos();
        Box box = entity.getBoundingBox();
        Vec3d target = closestPoint(box, eye);
        return rotationTo(eye, target);
    }

    public static double squaredBoxedDistanceTo(Entity entity) {
        if(RubyClient.client.player == null) return Double.MAX_VALUE;
        Vec3d eye = RubyClient.client.player.getEyePos();
        return closestPoint(entity.getBoundingBox(), eye).squaredDistanceTo(eye);
    }

    public static boolean canHit(Entity entity, float yaw, float pitch, double range) {
        if(RubyClient.client.player == null) return false;

        Vec3d eye = RubyClient.client.player.getEyePos();
        Vec3d look = rotationVector(yaw, pitch);
        Vec3d end = eye.add(look.multiply(range));
        return entity.getBoundingBox().raycast(eye, end).isPresent();
    }

    public static boolean inFov(Entity entity, float fov) {
        if(RubyClient.client.player == null) return false;

        Vec3d eye = RubyClient.client.player.getEyePos();
        Vec3d toTarget = closestPoint(entity.getBoundingBox(), eye).subtract(eye);
        if(toTarget.lengthSquared() < 1.0E-7) return true;

        Vec3d look = RubyClient.client.player.getRotationVector();
        double dot = look.normalize().dotProduct(toTarget.normalize());
        dot = MathHelper.clamp(dot, -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle <= fov * 0.5;
    }

    private static Vec3d closestPoint(Box box, Vec3d point) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    private static Vec3d rotationVector(float yaw, float pitch) {
        float radPitch = pitch * MathHelper.RADIANS_PER_DEGREE;
        float radYaw = -yaw * MathHelper.RADIANS_PER_DEGREE;
        float cosPitch = MathHelper.cos(radPitch);
        return new Vec3d(
                MathHelper.sin(radYaw) * cosPitch,
                -MathHelper.sin(radPitch),
                MathHelper.cos(radYaw) * cosPitch
        );
    }

    private static float[] rotationTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = MathHelper.wrapDegrees((float) Math.toDegrees(-Math.atan2(dy, horiz)));
        return new float[] { yaw, pitch };
    }
}
