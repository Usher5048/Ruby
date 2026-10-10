package ruby.helpers;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import ruby.RubyClient;
import java.util.List;
import java.util.Optional;

public class HitResults {
    public static BlockHitResult createBlock(BlockPos pos, Direction dir) {
        return createBlock(pos, dir, false);
    }

    public static BlockHitResult createBlock(BlockPos pos, Direction dir, boolean insideBlock) {
        double xOff = Math.random();
        double yOff = Math.random();
        double zOff = Math.random();

        if(insideBlock) {
            return new BlockHitResult(
                    new Vec3(pos).add(xOff, yOff, zOff),
                    dir,
                    pos,
                    true
            );
        }

        double actualXOff = dir.getStepX() == 0 ? xOff : Math.clamp(dir.getStepX(), 0, 1);
        double actualYOff = dir.getStepY() == 0 ? yOff : Math.clamp(dir.getStepY(), 0, 1);
        double actualZOff = dir.getStepZ() == 0 ? zOff : Math.clamp(dir.getStepZ(), 0, 1);

        return new BlockHitResult(
                new Vec3(pos).add(actualXOff, actualYOff, actualZOff),
                dir,
                pos,
                false
        );
    }

    public static HitResult raycast(ClipContext context) {
        if(RubyClient.client.level == null)
            return BlockHitResult.miss(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO);

        BlockHitResult result = RubyClient.client.level.clip(context);
        Vec3 start = context.getFrom();
        Vec3 end = context.getTo();

        Vec3 direction = end.subtract(start);
        double closestDistance = direction.length();

        Entity closestEntity = null;
        Vec3 closestHitPos = null;

        AABB searchBox = new AABB(start, end).inflate(1);
        List<Entity> entities = RubyClient.client.level.getEntities(RubyClient.client.player, searchBox);

        for(Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox();
            Optional<Vec3> hit = entityBox.clip(start, end);

            if(hit.isEmpty()) continue;

            double dist = start.distanceTo(hit.get());
            if(dist >= closestDistance) continue;

            closestHitPos = hit.get();
            closestEntity = entity;
            closestDistance = dist;
        }

        if(closestEntity != null && closestDistance < start.distanceTo(result.getLocation()))
            return new EntityHitResult(closestEntity, closestHitPos);

        return result;
    }
}
