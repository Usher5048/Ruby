package ruby.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import ruby.RubyClient;
import ruby.systems.modules.combat.Criticals;

public final class RotationManager {
    private static final float TURN_SPEED = 180f;

    private static Float currentYaw = null;
    private static Float currentPitch = null;
    private static Float targetYaw = null;
    private static Float targetPitch = null;
    private static Entity targetEntity = null;

    private RotationManager() {}

    public static boolean hasRotation() {
        return currentYaw != null;
    }

    public static Entity targetEntity() {
        return targetEntity;
    }

    public static float rotationYaw() {
        return currentYaw;
    }

    public static float rotationPitch() {
        return currentPitch;
    }

    public static void reset() {
        currentYaw = null;
        currentPitch = null;
        targetYaw = null;
        targetPitch = null;
        targetEntity = null;
    }

    public static PlayerInput transformInput(PlayerInput in) {
        if(RubyClient.client.player == null) return in;

        if(Criticals.blocksSprintInput()) in = stripSprint(in);
        if(!hasRotation()) return in;

        float z = in.forward() == in.backward() ? 0f : (in.forward() ? 1f : -1f);
        float x = in.left() == in.right() ? 0f : (in.left() ? 1f : -1f);
        if(x == 0f && z == 0f) return stripSprint(in);

        float deltaYaw = (RubyClient.client.player.getYaw() - currentYaw) * MathHelper.RADIANS_PER_DEGREE;
        float cos = MathHelper.cos(deltaYaw);
        float sin = MathHelper.sin(deltaYaw);

        int side = Math.round(x * cos - z * sin);
        int fwd = Math.round(z * cos + x * sin);
        if(side == 0 && fwd == 0) return stripSprint(in);

        boolean forward = fwd > 0;
        boolean backward = fwd < 0;
        boolean left = side > 0;
        boolean right = side < 0;
        boolean sprint = in.sprint() && forward && !backward;

        return new PlayerInput(forward, backward, left, right, in.jump(), in.sneak(), sprint);
    }

    public static boolean shouldSprint(PlayerInput in) {
        if(Criticals.blocksSprintInput()) return false;
        if(!hasRotation()) return in.sprint();
        return transformInput(in).sprint();
    }

    private static PlayerInput stripSprint(PlayerInput in) {
        if(!in.sprint()) return in;
        return new PlayerInput(in.forward(), in.backward(), in.left(), in.right(), in.jump(), in.sneak(), false);
    }

    public static void setTarget(Entity entity) {
        if(entity == null) {
            clearTarget();
            return;
        }

        targetEntity = entity;
        float[] rotation = RotationRaycast.aimAt(entity);
        targetYaw = rotation[0];
        targetPitch = rotation[1];
    }

    public static void setTarget(float yaw, float pitch) {
        targetEntity = null;
        targetYaw = yaw;
        targetPitch = pitch;
    }

    public static void clearTarget() {
        targetEntity = null;
        targetYaw = null;
        targetPitch = null;
    }

    public static void snapTo(float yaw, float pitch) {
        if(RubyClient.client.player == null) return;

        targetYaw = yaw;
        targetPitch = pitch;

        float fromYaw = gcdBaseYaw();
        float fromPitch = gcdBasePitch();
        float[] snapped = normalize(fromYaw, fromPitch, yaw, pitch);
        currentYaw = snapped[0];
        currentPitch = snapped[1];
    }

    public static void update() {
        if(RubyClient.client.player == null) return;

        if(targetEntity != null && !targetEntity.isAlive()) clearTarget();

        if(targetYaw != null) {
            float fromYaw = gcdBaseYaw();
            float fromPitch = gcdBasePitch();

            if(targetEntity != null) {
                float[] aim = RotationRaycast.aimAt(targetEntity);
                targetYaw = aim[0];
                targetPitch = aim[1];
            }

            float[] normalized = normalize(fromYaw, fromPitch, targetYaw, targetPitch);
            float[] next = step(fromYaw, fromPitch, normalized[0], normalized[1]);

            currentYaw = next[0];
            currentPitch = next[1];
            return;
        }

        if(hasRotation()) reset();
    }

    private static float gcdBaseYaw() {
        if(currentYaw != null) return currentYaw;
        return RubyClient.client.player.getYaw();
    }

    private static float gcdBasePitch() {
        if(currentPitch != null) return currentPitch;
        return RubyClient.client.player.getPitch();
    }

    private static float[] step(float fromYaw, float fromPitch, float toYaw, float toPitch) {
        float dy = MathHelper.wrapDegrees(toYaw - fromYaw);
        float dp = toPitch - fromPitch;
        float yawStep = Math.min(Math.abs(dy), TURN_SPEED) * Math.signum(dy);
        float pitchStep = Math.min(Math.abs(dp), TURN_SPEED) * Math.signum(dp);
        return new float[] {
                fromYaw + yawStep,
                MathHelper.clamp(fromPitch + pitchStep, -90f, 90f)
        };
    }

    private static double mouseGcd() {
        if(RubyClient.client == null) return 0.15;
        double f = RubyClient.client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }

    private static float[] normalize(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        double gcd = mouseGcd();
        if(gcd <= 0) return new float[] { MathHelper.wrapDegrees(targetYaw), MathHelper.clamp(targetPitch, -90f, 90f) };

        float dy = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float dp = targetPitch - currentPitch;
        float yaw = currentYaw + Math.round(dy / gcd) * (float) gcd;
        float pitch = MathHelper.clamp(currentPitch + Math.round(dp / gcd) * (float) gcd, -90f, 90f);
        return new float[] { yaw, pitch };
    }
}
