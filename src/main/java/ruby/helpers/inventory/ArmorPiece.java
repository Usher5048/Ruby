package ruby.helpers.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public record ArmorPiece(int inventorySlot, EquipmentSlot slot, ItemStack stack) {
    public boolean isEquipped(PlayerEntity player) {
        return player.getEquippedStack(this.slot).getItem() == this.stack.getItem();
    }

    public double score() {
        return ArmorPiece.scoreStack(this.stack);
    }

    public static double scoreStack(ItemStack stack) {
        if(stack.isEmpty()) return -1;

        double armor = 0;
        double toughness = 0;
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if(modifiers != null) {
            for(AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                if(entry.attribute().matches(EntityAttributes.ARMOR))
                    armor += entry.modifier().value();
                if(entry.attribute().matches(EntityAttributes.ARMOR_TOUGHNESS))
                    toughness += entry.modifier().value();
            }
        }

        double score = armor * 20 + toughness * 5;

        if(stack.isDamageable()) {
            double durability = 1.0 - (double) stack.getDamage() / stack.getMaxDamage();
            score += durability * 8;
        }

        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if(enchants != null) score += enchants.getSize() * 2;

        return score;
    }

    public static EquipmentSlot slotFromStack(ItemStack stack) {
        var equip = stack.get(DataComponentTypes.EQUIPPABLE);
        return equip == null ? null : equip.slot();
    }
}
