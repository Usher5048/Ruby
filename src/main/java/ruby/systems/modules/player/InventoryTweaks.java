package ruby.systems.modules.player;

import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.helpers.Slots;
import ruby.mixin.HandledScreenAccessor;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryTweaks extends Module {
    public final BooleanValue shiftDragMove = this.config.create(new BooleanValue.Builder("Shift Drag Move")
            .description("If holding shift and LMB, mousing over items quick moves them")
            .defaultValue(true)
            .build());

    public final BooleanValue sortingEnabled = this.config.create(new BooleanValue.Builder("Sorting Enabled")
            .description("Whether the inventory should be sorted upon a key being pressed")
            .defaultValue(true)
            .build());

    public InventoryTweaks() {
        super("Inventory Tweaks", "Tweaks the way inventories work", ModuleType.PLAYER);
    }

    private boolean pressedLastCheck = false;
    private boolean sortKeyPressed() {
        boolean pressed = GLFW.glfwGetMouseButton(
                RubyClient.client.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE
        ) == GLFW.GLFW_PRESS;

        boolean wasPressed = pressed && !this.pressedLastCheck;
        this.pressedLastCheck = pressed;

        return wasPressed;
    }

    private record SlotMove(int from, int to) {}
    private enum SlotSection {
        HOTBAR,
        INVENTORY,
        CONTAINER
    }

    private SlotSection getSlotSection(HandledScreen<?> screen, Slot focusedSlot) {
        SlotSection section = null;
        if(focusedSlot.inventory instanceof PlayerInventory) {
            if(Slots.HOTBAR.contains(focusedSlot.getIndex())) section = SlotSection.HOTBAR;
            else if(Slots.MAIN.contains(focusedSlot.getIndex())) section = SlotSection.INVENTORY;
        } else if((
                screen instanceof Generic3x3ContainerScreen ||
                screen instanceof GenericContainerScreen ||
                screen instanceof ShulkerBoxScreen
        ) && focusedSlot.inventory instanceof SimpleInventory) section = SlotSection.CONTAINER;
        return section;
    }

    private int compareStacks(ItemStack a, ItemStack b) {
        if(a.isEmpty() && b.isEmpty()) return 0;
        if(a.isEmpty()) return 1;
        if(b.isEmpty()) return -1;

        int countCmp = Integer.compare(b.getCount(), a.getCount());
        if(countCmp != 0) return countCmp;

        return a.getItem().getName().getString().compareToIgnoreCase(b.getItem().getName().getString());
    }

    private final ArrayList<SlotMove> moves = new ArrayList<>();
    private void tickSort() {
        if(!(RubyClient.client.currentScreen instanceof HandledScreen<?> screen)) {
            this.moves.clear();
            return;
        }

        if(!this.moves.isEmpty()) {
            SlotMove move = this.moves.removeFirst();
            Slots.swap(move.from, move.to);
            return;
        }

        if(!this.sortKeyPressed()) return;

        Slot focusedSlot = ((HandledScreenAccessor) screen).ruby$getFocusedSlot();
        if(focusedSlot == null) return;

        SlotSection section = this.getSlotSection(screen, focusedSlot);
        if(section == null) return;

        ArrayList<Slot> slots = new ArrayList<>();
        for(Slot slot : screen.getScreenHandler().slots) {
            if(this.getSlotSection(screen, slot) != section) continue;
            slots.add(slot);
        }

        List<ItemStack> original = slots.stream()
                .map(s -> s.getStack().copy())
                .collect(Collectors.toCollection(ArrayList::new));

        List<ItemStack> sorted = original.stream()
                .sorted(this::compareStacks)
                .toList();

        for(int i = 0; i < slots.size(); i++) {
            ItemStack current = original.get(i);
            ItemStack target = sorted.get(i);
            if(ItemStack.areEqual(current, target)) continue;

            for(int j = i + 1; j < slots.size(); j++) {
                if(!ItemStack.areEqual(original.get(j), target)) continue;
                this.moves.add(new SlotMove(slots.get(j).id, slots.get(i).id));

                ItemStack tmp = original.get(i);
                original.set(i, original.get(j));
                original.set(j, tmp);
                break;
            }
        }
    }

    @Override
    public void tick() {
        if(this.sortingEnabled.value()) this.tickSort();
    }
}
