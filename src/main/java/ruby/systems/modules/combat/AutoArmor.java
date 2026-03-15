package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Core logic from Meteor's AutoArmor:
 * - Scans inventory for armor pieces
 * - Scores each piece (based on durability/material tier as proxy for protection)
 * - Equips best piece for each slot
 * - Delay between operations to avoid flagging anti-cheat
 * - Anti-break: won't equip nearly broken armor
 */
public class AutoArmor extends Module {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final IntegerValue delay;
    private final BooleanValue antiBreak;

    private int timer = 0;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the best armor.", ModuleCategory.COMBAT);

        delay = config.create(new IntegerValue.Builder("Delay")
                .description("Delay in ticks between equipping armor pieces.")
                .defaultValue(1).min(0).max(20)
                .build());

        antiBreak = config.create(new BooleanValue.Builder("Anti Break")
                .description("Stops wearing armor if its durability is low.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        if (timer > 0) { timer--; return; }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack currentArmor = player.getEquippedStack(slot);
            double currentScore = getArmorScore(currentArmor, slot);

            int bestSlot = -1;
            double bestScore = currentScore;

            // Search main inventory + hotbar for better armor
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;

                EquippableComponent equip = stack.get(DataComponentTypes.EQUIPPABLE);
                if (equip == null || equip.slot() != slot) continue;

                double score = getArmorScore(stack, slot);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1) {
                equipArmor(mc, player, bestSlot, slot);
                timer = delay.value();
                return; // One swap per cycle
            }
        }
    }

    private double getArmorScore(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return -1;

        // Anti-break check
        if (antiBreak.value() && stack.isDamageable()) {
            if (stack.getMaxDamage() - stack.getDamage() <= 3) return -1;
        }

        // Score based on max durability (correlates with material tier)
        // Netherite > Diamond > Iron > Chain > Gold > Leather
        double score = stack.getMaxDamage();

        // Bonus for remaining durability
        if (stack.isDamageable()) {
            double durabilityPercent = 1.0 - ((double) stack.getDamage() / stack.getMaxDamage());
            score += durabilityPercent * 10;
        }

        return score;
    }

    private void equipArmor(MinecraftClient mc, ClientPlayerEntity player, int invSlot, EquipmentSlot armorSlot) {
        int syncId = player.currentScreenHandler.syncId;
        int armorScreenSlot = armorSlotToScreenSlot(armorSlot);
        int sourceScreenSlot = invToScreenSlot(invSlot);

        // If armor slot already has something, move it out first
        ItemStack currentArmor = player.getEquippedStack(armorSlot);
        if (!currentArmor.isEmpty()) {
            // Shift-click current armor to inventory
            mc.interactionManager.clickSlot(syncId, armorScreenSlot, 0, SlotActionType.QUICK_MOVE, player);
        }

        // Shift-click new armor from inventory to armor slot
        mc.interactionManager.clickSlot(syncId, sourceScreenSlot, 0, SlotActionType.QUICK_MOVE, player);
    }

    private int armorSlotToScreenSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    private int invToScreenSlot(int invSlot) {
        if (invSlot < 9) return invSlot + 36; // Hotbar 0-8 → Screen 36-44
        return invSlot; // Main 9-35 → Screen 9-35
    }
}
