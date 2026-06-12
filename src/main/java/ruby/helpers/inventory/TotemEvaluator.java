package ruby.helpers.inventory;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

public final class TotemEvaluator {
    private TotemEvaluator() {}

    public static boolean shouldEquip(
            PlayerEntity player,
            boolean smart,
            float healthThreshold,
            boolean considerAbsorption,
            boolean missingArmor,
            boolean predictFall
    ) {
        if(player.isCreative() || player.isSpectator() || !player.isAlive()) return false;
        if(player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return false;

        if(missingArmor && hasMissingArmor(player)) return true;

        float health = player.getHealth();
        if(considerAbsorption) health += player.getAbsorptionAmount();

        if(!smart) return true;

        if(health <= healthThreshold) return true;

        if(predictFall && player.fallDistance > 3.0f) {
            float fallDamage = estimateFallDamage(player);
            if(health - fallDamage <= healthThreshold) return true;
        }

        return false;
    }

    private static boolean hasMissingArmor(PlayerEntity player) {
        for(EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            if(player.getEquippedStack(slot).isEmpty()) return true;
        }
        return false;
    }

    private static float estimateFallDamage(PlayerEntity player) {
        float distance = (float) player.fallDistance - 3.0f;
        if(distance <= 0) return 0;
        return distance;
    }
}
