package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's ChestSwap: Swaps between elytra and chestplate in the chest armor slot.
 * Uses DataComponentTypes.GLIDER to identify elytra in 1.21+.
 * On enable: performs the swap, then disables itself.
 */
public class ChestSwap extends Module {
    public ChestSwap() {
        super("Chest Swap", "Swaps between elytra and chestplate.", ModuleCategory.PLAYER);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) return;

        ItemStack currentChest = player.getEquippedStack(EquipmentSlot.CHEST);
        boolean hasElytra = isElytra(currentChest);

        if (hasElytra) {
            // Find chestplate in inventory
            int slot = findChestplate(player);
            if (slot != -1) swapWithChestSlot(mc, player, slot);
        } else {
            // Find elytra in inventory
            int slot = findElytra(player);
            if (slot != -1) swapWithChestSlot(mc, player, slot);
        }

        // Auto-disable after swap
        this.enabled = false;
    }

    private boolean isElytra(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.get(DataComponentTypes.GLIDER) != null;
    }

    private int findChestplate(ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            var equip = stack.get(DataComponentTypes.EQUIPPABLE);
            if (equip != null && equip.slot() == EquipmentSlot.CHEST && !isElytra(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findElytra(ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isElytra(stack)) return i;
        }
        return -1;
    }

    private void swapWithChestSlot(MinecraftClient mc, ClientPlayerEntity player, int invSlot) {
        int syncId = player.currentScreenHandler.syncId;
        int sourceScreenSlot = invSlot < 9 ? invSlot + 36 : invSlot;

        // Pick up from inventory, place in chest slot (screen slot 6), pick up old
        mc.interactionManager.clickSlot(syncId, sourceScreenSlot, 0, SlotActionType.PICKUP, player);
        mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, player);
        // If there was something in the chest slot, put it back in inventory
        mc.interactionManager.clickSlot(syncId, sourceScreenSlot, 0, SlotActionType.PICKUP, player);
    }
}
