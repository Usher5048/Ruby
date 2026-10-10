package ruby.systems.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

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
        super("Chest Swap", "Swaps between elytra and chestplate.", ModuleType.PLAYER);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
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

        Modules.disable(this);
    }

    private boolean isElytra(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.get(DataComponents.GLIDER) != null;
    }

    private int findChestplate(LocalPlayer player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            var equip = stack.get(DataComponents.EQUIPPABLE);
            if (equip != null && equip.slot() == EquipmentSlot.CHEST && !isElytra(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findElytra(LocalPlayer player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isElytra(stack)) return i;
        }
        return -1;
    }

    private void swapWithChestSlot(Minecraft mc, LocalPlayer player, int invSlot) {
        int syncId = player.containerMenu.containerId;
        int sourceScreenSlot = invSlot < 9 ? invSlot + 36 : invSlot;

        // Pick up from inventory, place in chest slot (screen slot 6), pick up old
        mc.gameMode.handleContainerInput(syncId, sourceScreenSlot, 0, ContainerInput.PICKUP, player);
        mc.gameMode.handleContainerInput(syncId, 6, 0, ContainerInput.PICKUP, player);
        // If there was something in the chest slot, put it back in inventory
        mc.gameMode.handleContainerInput(syncId, sourceScreenSlot, 0, ContainerInput.PICKUP, player);
    }
}
