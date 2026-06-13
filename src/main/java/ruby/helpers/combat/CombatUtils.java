package ruby.helpers.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import ruby.RubyClient;

public final class CombatUtils {
    private CombatUtils() {
    }

    public static boolean shouldBeAttacked(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity == RubyClient.client.player) return false;
        if (!living.isAlive()) return false;
        if (entity instanceof PlayerEntity player && player.isCreative()) return false;
        return entity instanceof PlayerEntity || entity instanceof HostileEntity || entity instanceof AnimalEntity;
    }

    public static LivingEntity findEnemy(double range) {
        ClientPlayerEntity player = RubyClient.client.player;
        if (player == null || RubyClient.client.world == null) return null;

        Box box = player.getBoundingBox().expand(range);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : RubyClient.client.world.getOtherEntities(player, box, CombatUtils::shouldBeAttacked)) {
            if (!(entity instanceof LivingEntity living)) continue;
            double dist = player.squaredDistanceTo(entity);
            if (dist > range * range) continue;
            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }

        return closest;
    }
}
