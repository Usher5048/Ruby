package ruby.systems.modules.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.helpers.Slots;
import ruby.mixin.HandledScreenAccessor;
import ruby.systems.config.BooleanValue;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvents;
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

    private boolean openedOnServer = false;
    public final BooleanValue xCarryEnabled = this.config.create(new BooleanValue.Builder("Enable XCarry")
            .description("Allows carrying 4 extra items in the crafting grid slots")
            .defaultValue(true)
            .changed(val -> {
                if(val) return;
                if(RubyClient.client.player == null) return;
                if(RubyClient.client.getConnection() == null) return;

                this.openedOnServer = false;
                RubyClient.client.getConnection().send(new ServerboundContainerClosePacket(
                        RubyClient.client.player.inventoryMenu.containerId
                ));
            }).build());

    public InventoryTweaks() {
        super("Inventory Tweaks", "Tweaks the way inventories work", ModuleType.PLAYER);

        Events.PACKET.register(PacketEvents.SEND, event -> {
            if(!this.enabled()) return;
            if(!this.xCarryEnabled.value()) return;
            if(RubyClient.client.player == null) return;

            if(!(event.packet() instanceof ServerboundContainerClosePacket packet)) return;
            if(packet.getContainerId() != RubyClient.client.player.inventoryMenu.containerId) return;

            this.openedOnServer = true;
            event.setCancelled(true);
        });
    }

    private boolean pressedLastCheck = false;
    private boolean sortKeyPressed() {
        boolean pressed = InputUtils.isMousePressed(InputConstants.MOUSE_BUTTON_MIDDLE);

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

    private SlotSection getSlotSection(AbstractContainerScreen<?> screen, Slot focusedSlot) {
        SlotSection section = null;
        if(focusedSlot.container instanceof Inventory) {
            if(Slots.HOTBAR.contains(focusedSlot.getContainerSlot())) section = SlotSection.HOTBAR;
            else if(Slots.MAIN.contains(focusedSlot.getContainerSlot())) section = SlotSection.INVENTORY;
        } else if((
                screen instanceof DispenserScreen ||
                screen instanceof ContainerScreen ||
                screen instanceof ShulkerBoxScreen
        ) && focusedSlot.container instanceof SimpleContainer) section = SlotSection.CONTAINER;
        return section;
    }

    private int compareStacks(ItemStack a, ItemStack b) {
        if(a.isEmpty() && b.isEmpty()) return 0;
        if(a.isEmpty()) return 1;
        if(b.isEmpty()) return -1;

        int countCmp = Integer.compare(b.getCount(), a.getCount());
        if(countCmp != 0) return countCmp;

        return a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
    }

    private final ArrayList<SlotMove> moves = new ArrayList<>();
    private void tickSort() {
        if(!(RubyClient.client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
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
        for(Slot slot : screen.getMenu().slots) {
            if(this.getSlotSection(screen, slot) != section) continue;
            slots.add(slot);
        }

        List<ItemStack> original = slots.stream()
                .map(s -> s.getItem().copy())
                .collect(Collectors.toCollection(ArrayList::new));

        List<ItemStack> sorted = original.stream()
                .sorted(this::compareStacks)
                .toList();

        for(int i = 0; i < slots.size(); i++) {
            ItemStack current = original.get(i);
            ItemStack target = sorted.get(i);
            if(ItemStack.matches(current, target)) continue;

            for(int j = i + 1; j < slots.size(); j++) {
                if(!ItemStack.matches(original.get(j), target)) continue;
                this.moves.add(new SlotMove(slots.get(j).index, slots.get(i).index));

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

    @Override
    public void onDisable() {
        if(!this.openedOnServer) return;
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getConnection() == null) return;

        this.openedOnServer = false;
        RubyClient.client.getConnection().send(new ServerboundContainerClosePacket(
                RubyClient.client.player.inventoryMenu.containerId
        ));
    }
}
