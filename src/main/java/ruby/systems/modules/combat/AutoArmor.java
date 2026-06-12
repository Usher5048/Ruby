package ruby.systems.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ruby.helpers.Slots;
import ruby.helpers.inventory.*;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.Events;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import java.util.List;

public class AutoArmor extends Module {
    private final IntegerValue startDelay;
    private final IntegerValue clickDelayMin;
    private final IntegerValue clickDelayMax;
    private final IntegerValue closeDelay;
    private final IntegerValue saveThreshold;
    private final BooleanValue useHotbar;
    private final BooleanValue antiBreak;

    public AutoArmor() {
        super("Auto Armor", "Automatically equips the best armor.", ModuleType.COMBAT);

        startDelay = config.create(new IntegerValue.Builder("Start Delay")
                .description("Ticks to wait after opening inventory.")
                .defaultValue(1).min(0).max(20)
                .build());

        clickDelayMin = config.create(new IntegerValue.Builder("Click Delay Min")
                .description("Minimum ticks between inventory clicks.")
                .defaultValue(1).min(0).max(20)
                .build());

        clickDelayMax = config.create(new IntegerValue.Builder("Click Delay Max")
                .description("Maximum ticks between inventory clicks.")
                .defaultValue(2).min(0).max(20)
                .build());

        closeDelay = config.create(new IntegerValue.Builder("Close Delay")
                .description("Ticks to wait before closing inventory.")
                .defaultValue(1).min(0).max(20)
                .build());

        saveThreshold = config.create(new IntegerValue.Builder("Save Threshold")
                .description("Replace armor below this durability percent. 0 disables.")
                .defaultValue(0).min(0).max(100)
                .build());

        useHotbar = config.create(new BooleanValue.Builder("Hotbar")
                .description("Equip armor directly from the hotbar when possible.")
                .defaultValue(true)
                .build());

        antiBreak = config.create(new BooleanValue.Builder("Anti Break")
                .description("Ignore armor that is almost broken.")
                .defaultValue(true)
                .build());

        Events.INVENTORY_SCHEDULE.register(this::onSchedule);
    }

    private void onSchedule(ScheduleInventoryActionEvent event) {
        if(!this.enabled()) return;

        ClientPlayerEntity player = mc();
        if(player == null || player.isSpectator()) return;
        if(InventoryManager.isOperating()) return;

        int durabilityThreshold = this.antiBreak.value() ? 10 : this.saveThreshold.value();
        List<ArmorPiece> upgrades = ArmorEvaluation.findUpgrades(player, durabilityThreshold);
        if(upgrades.isEmpty()) return;

        InventoryConstraints constraints = new InventoryConstraints(
                this.startDelay.value(),
                this.clickDelayMin.value(),
                this.clickDelayMax.value(),
                this.closeDelay.value(),
                true,
                false
        );

        List<InventoryAction> actions = new ArrayList<>();
        for(ArmorPiece piece : upgrades)
            actions.addAll(this.buildActions(player, piece));

        if(!actions.isEmpty())
            event.schedule(constraints, actions, InventoryPriority.IMPORTANT);
    }

    private List<InventoryAction> buildActions(ClientPlayerEntity player, ArmorPiece piece) {
        EquipmentSlot armorSlot = piece.slot();
        ItemStack equipped = player.getEquippedStack(armorSlot);
        if(ArmorEvaluation.isElytra(equipped)) return List.of();

        int sourceId = Slots.indexToID(piece.inventorySlot());
        if(sourceId == Slots.INVALID_SLOT) return List.of();

        boolean inHotbar = Slots.HOTBAR.contains(piece.inventorySlot());
        boolean armorOccupied = !equipped.isEmpty();
        int armorScreenId = this.armorScreenSlot(armorSlot);

        if(inHotbar && this.useHotbar.value() && !armorOccupied)
            return List.of(new InventoryAction.UseHotbar(piece.inventorySlot()));

        List<InventoryAction> actions = new ArrayList<>();
        if(armorOccupied) {
            if(this.hasInventorySpace(player))
                actions.add(InventoryAction.Click.quickMove(armorScreenId));
            else
                actions.add(InventoryAction.Click.throwSlot(armorScreenId));
        }

        actions.add(InventoryAction.Click.quickMove(sourceId));
        return actions;
    }

    private boolean hasInventorySpace(ClientPlayerEntity player) {
        for(int i = 0; i < 36; i++) {
            if(player.getInventory().getStack(i).isEmpty()) return true;
        }
        return false;
    }

    private int armorScreenSlot(EquipmentSlot slot) {
        return switch(slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> Slots.INVALID_SLOT;
        };
    }

    private static ClientPlayerEntity mc() {
        return ruby.RubyClient.client.player;
    }
}
