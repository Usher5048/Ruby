package ruby.helpers.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record ArmorPiece(int inventorySlot, EquipmentSlot slot, ItemStack stack) {
    public boolean isEquipped(Player player) {
        return player.getItemBySlot(this.slot).getItem() == this.stack.getItem();
    }

    public double score() {
        return ArmorPiece.scoreStack(this.stack);
    }

    public static double scoreStack(ItemStack stack) {
        if(stack.isEmpty()) return -1;

        double armor = 0;
        double toughness = 0;
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if(modifiers != null) {
            for(ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if(entry.attribute().is(Attributes.ARMOR))
                    armor += entry.modifier().amount();
                if(entry.attribute().is(Attributes.ARMOR_TOUGHNESS))
                    toughness += entry.modifier().amount();
            }
        }

        double score = armor * 20 + toughness * 5;

        if(stack.isDamageableItem()) {
            double durability = 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage();
            score += durability * 8;
        }

        var enchants = stack.get(DataComponents.ENCHANTMENTS);
        if(enchants != null) score += enchants.size() * 2;

        return score;
    }

    public static EquipmentSlot slotFromStack(ItemStack stack) {
        var equip = stack.get(DataComponents.EQUIPPABLE);
        return equip == null ? null : equip.slot();
    }
}
