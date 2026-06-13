package ruby.helpers.projectile;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ruby.RubyClient;
import ruby.helpers.RotationManager;

/**
 * Simplified projectile path simulator ported from Meteor Client.
 */
public class ProjectileSimulator {

    public Vec3d pos = Vec3d.ZERO;
    public Vec3d velocity = Vec3d.ZERO;
    private double gravity = 0.03;
    private float drag = 0.99f;

    public boolean set(PlayerEntity player, ItemStack stack, float tickDelta) {
        Item item = stack.getItem();

        float power;
        gravity = 0.03;

        if (item instanceof BowItem) {
            float charge = BowItem.getPullProgress(player.getItemUseTime());
            if (charge < 0.1f) charge = 1f;
            power = charge * 3f;
        } else if (item instanceof CrossbowItem) {
            power = 3.15f;
        } else if (item instanceof TridentItem) {
            power = 2.5f;
        } else if (item instanceof ExperienceBottleItem) {
            gravity = 0.07;
            power = 0.7f;
        } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            gravity = 0.05;
            power = 0.5f;
        } else if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem) {
            power = 1.5f;
        } else {
            return false;
        }

        drag = 0.99f;

        float yaw = RotationManager.hasRotation() ? RotationManager.rotationYaw() : player.getYaw(tickDelta);
        float pitch = RotationManager.hasRotation() ? RotationManager.rotationPitch() : player.getPitch(tickDelta);

        double x = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double y = -Math.sin(Math.toRadians(pitch + (item instanceof ExperienceBottleItem ? -20 : 0)));
        double z = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));

        Vec3d eye = player.getEyePos();
        pos = eye;
        velocity = new Vec3d(x, y, z).normalize().multiply(power);
        velocity = velocity.add(player.getVelocity().x, player.isOnGround() ? 0 : player.getVelocity().y, player.getVelocity().z);
        return true;
    }

    public boolean set(Entity entity) {
        pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        velocity = entity.getVelocity();
        gravity = 0.03;
        drag = 0.99f;
        return true;
    }

    public HitResult tick() {
        Vec3d prev = pos;
        velocity = velocity.subtract(0, gravity, 0).multiply(drag);
        pos = pos.add(velocity);

        if (pos.y < RubyClient.client.world.getBottomY()) {
            return BlockHitResult.createMissed(pos, null, BlockPos.ORIGIN);
        }

        BlockHitResult blockHit = RubyClient.client.world.raycast(new RaycastContext(
                prev, pos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                RubyClient.client.player
        ));

        if (blockHit.getType() != HitResult.Type.MISS) return blockHit;

        Box box = new Box(prev, pos).expand(0.3);
        EntityHitResult entityHit = ProjectileUtil.raycast(
                RubyClient.client.player,
                prev, pos,
                box,
                e -> !e.isSpectator() && e.canHit(),
                pos.squaredDistanceTo(prev)
        );

        return entityHit != null ? entityHit : BlockHitResult.createMissed(pos, null, BlockPos.ORIGIN);
    }

    public static float[] getRotation(PlayerEntity player, float tickDelta) {
        float yaw = RotationManager.hasRotation() ? RotationManager.rotationYaw() : player.getYaw(tickDelta);
        float pitch = RotationManager.hasRotation() ? RotationManager.rotationPitch() : player.getPitch(tickDelta);
        return new float[] { yaw, pitch };
    }

    public static double lerp(double prev, double cur, float delta) {
        return MathHelper.lerp(delta, prev, cur);
    }
}
