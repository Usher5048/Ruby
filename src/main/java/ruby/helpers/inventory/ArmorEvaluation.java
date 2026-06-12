package ruby.helpers.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ArmorEvaluation {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorEvaluation() {}

    public static List<ArmorPiece> findUpgrades(PlayerEntity player, int durabilityThresholdPercent) {
        Map<EquipmentSlot, ArmorPiece> best = new EnumMap<>(EquipmentSlot.class);

        for(EquipmentSlot slot : SLOTS) {
            ItemStack equipped = player.getEquippedStack(slot);
            double equippedScore = scoreForComparison(equipped, slot, durabilityThresholdPercent);
            best.put(slot, equipped.isEmpty() ? null : new ArmorPiece(-1, slot, equipped));
        }

        for(int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if(stack.isEmpty()) continue;

            EquipmentSlot slot = ArmorPiece.slotFromStack(stack);
            if(slot == null) continue;
            if(isPlayerHead(stack)) continue;
            if(durabilityTooLow(stack, durabilityThresholdPercent)) continue;

            double score = scoreForComparison(stack, slot, durabilityThresholdPercent);
            ArmorPiece current = best.get(slot);
            double currentScore = current == null || current.inventorySlot() == -1
                    ? -1
                    : scoreForComparison(current.stack(), slot, durabilityThresholdPercent);

            if(score > currentScore)
                best.put(slot, new ArmorPiece(i, slot, stack));
        }

        List<ArmorPiece> upgrades = new ArrayList<>();
        for(EquipmentSlot slot : SLOTS) {
            ArmorPiece piece = best.get(slot);
            if(piece == null || piece.inventorySlot() == -1) continue;
            if(piece.isEquipped(player)) continue;
            upgrades.add(piece);
        }
        return upgrades;
    }

    private static double scoreForComparison(ItemStack stack, EquipmentSlot slot, int durabilityThresholdPercent) {
        if(stack.isEmpty()) return -1;
        if(isPlayerHead(stack)) return -1;
        if(durabilityTooLow(stack, durabilityThresholdPercent)) return -1;
        return ArmorPiece.scoreStack(stack);
    }

    public static boolean isPlayerHead(ItemStack stack) {
        return stack.isOf(Items.PLAYER_HEAD);
    }

    private static boolean durabilityTooLow(ItemStack stack, int durabilityThresholdPercent) {
        if(!stack.isDamageable() || durabilityThresholdPercent <= 0) return false;
        double remaining = 1.0 - (double) stack.getDamage() / stack.getMaxDamage();
        return remaining * 100 <= durabilityThresholdPercent;
    }

    public static boolean isElytra(ItemStack stack) {
        var equip = stack.get(DataComponentTypes.EQUIPPABLE);
        if(equip != null && equip.slot() == EquipmentSlot.CHEST) {
            var glider = stack.get(DataComponentTypes.GLIDER);
            return glider != null;
        }
        return stack.isOf(Items.ELYTRA);
    }
}
