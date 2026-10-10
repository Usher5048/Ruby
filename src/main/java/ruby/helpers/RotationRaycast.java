package ruby.helpers;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;

public final class RotationRaycast {
    private RotationRaycast() {}

    public static float[] aimAt(Entity entity) {
        if(RubyClient.client.player == null) return new float[] { 0f, 0f };
        Vec3 eye = RubyClient.client.player.getEyePosition();
        AABB box = entity.getBoundingBox();
        Vec3 target = closestPoint(box, eye);
        return rotationTo(eye, target);
    }

    public static double squaredBoxedDistanceTo(Entity entity) {
        if(RubyClient.client.player == null) return Double.MAX_VALUE;
        Vec3 eye = RubyClient.client.player.getEyePosition();
        return closestPoint(entity.getBoundingBox(), eye).distanceToSqr(eye);
    }

    public static boolean canHit(Entity entity, float yaw, float pitch, double range) {
        if(RubyClient.client.player == null) return false;

        Vec3 eye = RubyClient.client.player.getEyePosition();
        Vec3 look = rotationVector(yaw, pitch);
        Vec3 end = eye.add(look.scale(range));
        return entity.getBoundingBox().clip(eye, end).isPresent();
    }

    public static boolean inFov(Entity entity, float fov) {
        if(RubyClient.client.player == null) return false;

        Vec3 eye = RubyClient.client.player.getEyePosition();
        Vec3 toTarget = closestPoint(entity.getBoundingBox(), eye).subtract(eye);
        if(toTarget.lengthSqr() < 1.0E-7) return true;

        Vec3 look = Vec3.directionFromRotation(RubyClient.client.player.getRotationVector());
        double dot = look.normalize().dot(toTarget.normalize());
        dot = Mth.clamp(dot, -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle <= fov * 0.5;
    }

    private static Vec3 closestPoint(AABB box, Vec3 point) {
        double x = Mth.clamp(point.x, box.minX, box.maxX);
        double y = Mth.clamp(point.y, box.minY, box.maxY);
        double z = Mth.clamp(point.z, box.minZ, box.maxZ);
        return new Vec3(x, y, z);
    }

    private static Vec3 rotationVector(float yaw, float pitch) {
        float radPitch = pitch * Mth.RAD_TO_DEG;
        float radYaw = -yaw * Mth.RAD_TO_DEG;
        float cosPitch = Mth.cos(radPitch);
        return new Vec3(
                Mth.sin(radYaw) * cosPitch,
                -Mth.sin(radPitch),
                Mth.cos(radYaw) * cosPitch
        );
    }

    private static float[] rotationTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = Mth.wrapDegrees((float) Math.toDegrees(-Math.atan2(dy, horiz)));
        return new float[] { yaw, pitch };
    }
}
