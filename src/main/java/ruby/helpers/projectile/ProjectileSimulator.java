package ruby.helpers.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.helpers.RotationManager;

/**
 * Simplified projectile path simulator ported from Meteor Client.
 */
public class ProjectileSimulator {

    public Vec3 pos = Vec3.ZERO;
    public Vec3 velocity = Vec3.ZERO;
    private double gravity = 0.03;
    private float drag = 0.99f;

    public boolean set(Player player, ItemStack stack, float tickDelta) {
        Item item = stack.getItem();

        float power;
        gravity = 0.03;

        if (item instanceof BowItem) {
            float charge = BowItem.getPowerForTime(player.getTicksUsingItem());
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
        } else if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            power = 1.5f;
        } else {
            return false;
        }

        drag = 0.99f;

        float yaw = RotationManager.hasRotation() ? RotationManager.rotationYaw() : player.getViewYRot(tickDelta);
        float pitch = RotationManager.hasRotation() ? RotationManager.rotationPitch() : player.getViewXRot(tickDelta);

        double x = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double y = -Math.sin(Math.toRadians(pitch + (item instanceof ExperienceBottleItem ? -20 : 0)));
        double z = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));

        pos = player.getEyePosition();
        velocity = new Vec3(x, y, z).normalize().scale(power);
        velocity = velocity.add(player.getDeltaMovement().x, player.onGround() ? 0 : player.getDeltaMovement().y, player.getDeltaMovement().z);
        return true;
    }

    public boolean set(Entity entity) {
        pos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        velocity = entity.getDeltaMovement();
        gravity = 0.03;
        drag = 0.99f;
        return true;
    }

    public HitResult tick() {
        if(RubyClient.client.level == null) return null;
        if(RubyClient.client.player == null) return null;

        Vec3 prev = pos;
        velocity = velocity.subtract(0, gravity, 0).scale(drag);
        pos = pos.add(velocity);

        if (pos.y < RubyClient.client.level.getMinY()) {
            return BlockHitResult.miss(pos, Direction.DOWN, BlockPos.ZERO);
        }

        BlockHitResult blockHit = RubyClient.client.level.clip(new ClipContext(
                prev, pos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                RubyClient.client.player
        ));

        if (blockHit.getType() != HitResult.Type.MISS) return blockHit;

        AABB box = new AABB(prev, pos).inflate(0.3);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                RubyClient.client.player,
                prev, pos,
                box,
                e -> !e.isSpectator() && e.canBeHitByProjectile(),
                pos.distanceToSqr(prev)
        );

        return entityHit != null ? entityHit : BlockHitResult.miss(pos, Direction.DOWN, BlockPos.ZERO);
    }

    public static float[] getRotation(Player player, float tickDelta) {
        float yaw = RotationManager.hasRotation() ? RotationManager.rotationYaw() : player.getViewYRot(tickDelta);
        float pitch = RotationManager.hasRotation() ? RotationManager.rotationPitch() : player.getViewXRot(tickDelta);
        return new float[] { yaw, pitch };
    }

    public static double lerp(double prev, double cur, float delta) {
        return Mth.lerp(delta, prev, cur);
    }
}
