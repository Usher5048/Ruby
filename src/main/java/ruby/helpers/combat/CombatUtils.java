package ruby.helpers.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import ruby.RubyClient;

public final class CombatUtils {
    private CombatUtils() {
    }

    public static boolean shouldBeAttacked(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity == RubyClient.client.player) return false;
        if (!living.isAlive()) return false;
        if (entity instanceof Player player && player.isCreative()) return false;
        return entity instanceof Player || entity instanceof Monster || entity instanceof Animal;
    }

    public static LivingEntity findEnemy(double range) {
        LocalPlayer player = RubyClient.client.player;
        if (player == null || RubyClient.client.level == null) return null;

        AABB box = player.getBoundingBox().inflate(range);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : RubyClient.client.level.getEntities(player, box, CombatUtils::shouldBeAttacked)) {
            if (!(entity instanceof LivingEntity living)) continue;
            double dist = player.distanceToSqr(entity);
            if (dist > range * range) continue;
            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }

        return closest;
    }
}
