package ruby.helpers.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public final class TotemEvaluator {
    private TotemEvaluator() {}

    public static boolean shouldEquip(
            Player player,
            boolean smart,
            float healthThreshold,
            boolean considerAbsorption,
            boolean missingArmor,
            boolean predictFall
    ) {
        if(player.isCreative() || player.isSpectator() || !player.isAlive()) return false;
        if(player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return false;

        if(missingArmor && hasMissingArmor(player)) return true;

        float health = player.getHealth();
        if(considerAbsorption) health += player.getAbsorptionAmount();

        if(!smart) return true;

        if(health <= healthThreshold) return true;

        if(predictFall && player.fallDistance > 3.0f) {
            float fallDamage = estimateFallDamage(player);
            return health - fallDamage <= healthThreshold;
        }

        return false;
    }

    private static boolean hasMissingArmor(Player player) {
        for(EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            if(player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private static float estimateFallDamage(Player player) {
        float distance = (float) player.fallDistance - 3.0f;
        if(distance <= 0) return 0;
        return distance;
    }
}
