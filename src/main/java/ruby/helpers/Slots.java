package ruby.helpers;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Slots {
    public record Range(int start, int end) {
        public boolean contains(int slot) {
            return slot >= this.start && slot <= this.end;
        }
        public int size() {
            return this.end - this.start + 1;
        }
    }

    public static final int INVALID_SLOT = -1;
    public static final Range HOTBAR = new Range(0, 8);
    public static final Range MAIN = new Range(9, 35);
    public static final Range INVENTORY = new Range(Slots.HOTBAR.start(), Slots.MAIN.end());
    public static final Range ARMOR = new Range(36, 39);
    public static final int OFFHAND = 40;

    public static final Range ALL = new Range(0, 40);

    public static int indexToID(int slotIndex) {
        if(RubyClient.client.player == null) return Slots.INVALID_SLOT;

        if(RubyClient.client.player.currentScreenHandler instanceof AnvilScreenHandler) {
            return Slots.HOTBAR.contains(slotIndex) ? slotIndex + 30 :
                    Slots.MAIN.contains(slotIndex) ? 3 + slotIndex - 9:
                    Slots.INVALID_SLOT;
        } else if(RubyClient.client.player.currentScreenHandler instanceof SmithingScreenHandler) {
            return Slots.HOTBAR.contains(slotIndex) ? slotIndex + 31 :
                    Slots.MAIN.contains(slotIndex) ? 4 + slotIndex - 9 :
                    Slots.INVALID_SLOT;
        } else if(RubyClient.client.player.currentScreenHandler instanceof ShulkerBoxScreenHandler) {
            return Slots.HOTBAR.contains(slotIndex) ? slotIndex + 54 :
                    Slots.MAIN.contains(slotIndex) ? 27 + slotIndex - 9 :
                    Slots.INVALID_SLOT;
        } else if(RubyClient.client.player.currentScreenHandler instanceof CraftingScreenHandler) {
            return Slots.HOTBAR.contains(slotIndex) ? slotIndex + 37 :
                    Slots.MAIN.contains(slotIndex) ? slotIndex + 1 :
                    Slots.INVALID_SLOT;
        } else if(RubyClient.client.player.currentScreenHandler instanceof GenericContainerScreenHandler h) {
            return Slots.HOTBAR.contains(slotIndex) ? (h.getRows() + 3) * 9 + slotIndex :
                    Slots.MAIN.contains(slotIndex) ? h.getRows() * 9 + slotIndex - 9 :
                    Slots.INVALID_SLOT;
        } else if(RubyClient.client.player.currentScreenHandler instanceof MerchantScreenHandler) {
            return Slots.HOTBAR.contains(slotIndex) ? 30 + slotIndex :
                    Slots.MAIN.contains(slotIndex) ? 3 + (slotIndex - 9) :
                    Slots.INVALID_SLOT;
        }

        return Slots.HOTBAR.contains(slotIndex) ? slotIndex + 36 :
                Slots.MAIN.contains(slotIndex) ? slotIndex :
                Slots.ARMOR.contains(slotIndex) ? 5 + slotIndex - 36 :
                slotIndex == Slots.OFFHAND ? 45 :
                Slots.INVALID_SLOT;
    }

    public static int idToIndex(int slotId) {
        return Slots.HOTBAR.contains(slotId - 36) ? slotId - 36 :
                Slots.MAIN.contains(slotId) ? slotId :
                Slots.ARMOR.contains(slotId + 31) ? 36 + slotId - 5 :
                slotId == 45 ? Slots.OFFHAND :
                Slots.INVALID_SLOT;
    }

    public static int getSelectedID() {
        if(RubyClient.client.player == null) return Slots.INVALID_SLOT;
        return indexToID(RubyClient.client.player.getInventory().getSelectedSlot());
    }

    public static int findBest(Range range, BiFunction<ItemStack, Integer, Double> criteria) {
        if(RubyClient.client.player == null) return Slots.INVALID_SLOT;

        HashMap<Integer, Double> slots = new HashMap<>();

        for(int i = range.end(); i >= range.start(); i--) {
            ItemStack stack = RubyClient.client.player.getInventory().getStack(i);
            slots.put(i, criteria.apply(stack, i));
        }

        int bestSlot = Slots.INVALID_SLOT;
        double bestScore = Double.NaN;
        for(Map.Entry<Integer, Double> entry : slots.entrySet()) {
            if(Double.isNaN(entry.getValue())) continue;

            if(entry.getValue() > bestScore || Double.isNaN(bestScore)) {
                bestSlot = entry.getKey();
                bestScore = entry.getValue();
            }
        }

        return bestSlot;
    }

    public static ArrayList<Integer> find(Range range, BiFunction<ItemStack, Integer, Boolean> filter) {
        if(RubyClient.client.player == null) return new ArrayList<>();

        ArrayList<Integer> slotIndices = new ArrayList<>();

        for(int i = range.end(); i >= range.start(); i--) {
            ItemStack stack = RubyClient.client.player.getInventory().getStack(i);
            if(!filter.apply(stack, i)) continue;

            slotIndices.add(i);
        }

        return slotIndices;
    }

    public static int findFirst(Range range, BiFunction<ItemStack, Integer, Boolean> filter) {
        ArrayList<Integer> slotIndices = find(range, filter);
        if(slotIndices.isEmpty()) return Slots.INVALID_SLOT;

        return slotIndices.getLast();
    }

    public static void swap(int source, int destination) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.interactionManager == null) return;

        if(source == Slots.INVALID_SLOT) return;
        if(destination == Slots.INVALID_SLOT) return;
        if(source == destination) return;

        RubyClient.client.interactionManager.clickSlot(
                RubyClient.client.player.currentScreenHandler.syncId,
                source,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                SlotActionType.PICKUP,
                RubyClient.client.player
        );

        RubyClient.client.interactionManager.clickSlot(
                RubyClient.client.player.currentScreenHandler.syncId,
                destination,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                SlotActionType.PICKUP,
                RubyClient.client.player
        );

        if(RubyClient.client.player.currentScreenHandler.getCursorStack().isEmpty()) return;

        RubyClient.client.interactionManager.clickSlot(
                RubyClient.client.player.currentScreenHandler.syncId,
                source,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                SlotActionType.PICKUP,
                RubyClient.client.player
        );
    }

    public static void drop(int slotID, boolean dropAll) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.interactionManager == null) return;

        RubyClient.client.interactionManager.clickSlot(
                RubyClient.client.player.currentScreenHandler.syncId,
                slotID,
                dropAll ? 2 : 1,
                SlotActionType.THROW,
                RubyClient.client.player
        );
    }

    // "binds" the slotID to the inventory INDEX
    // only works with hotbar or offhand slots
    public static void bind(int slotID, int hotbarSlot) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.interactionManager == null) return;
        if(slotID == Slots.indexToID(hotbarSlot)) return;

        RubyClient.client.interactionManager.clickSlot(
                RubyClient.client.player.currentScreenHandler.syncId,
                slotID,
                hotbarSlot,
                SlotActionType.SWAP,
                RubyClient.client.player
        );
    }
}
