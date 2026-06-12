package ruby.systems.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import ruby.helpers.Slots;
import ruby.helpers.inventory.*;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.Events;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import java.util.List;

public class AutoTotem extends Module {
    private final BooleanValue smart;
    private final DoubleValue healthThreshold;
    private final BooleanValue considerAbsorption;
    private final BooleanValue missingArmor;
    private final BooleanValue predictFall;
    private final BooleanValue smartSwitch;
    private final IntegerValue startDelay;
    private final IntegerValue clickDelayMin;
    private final IntegerValue clickDelayMax;
    private final IntegerValue closeDelay;

    public AutoTotem() {
        super("Auto Totem", "Automatically moves totems to your offhand.", ModuleType.COMBAT);

        smart = config.create(new BooleanValue.Builder("Smart")
                .description("Only equip when health or danger checks fail.")
                .defaultValue(true)
                .build());

        healthThreshold = config.create(new DoubleValue.Builder("Health")
                .description("Health threshold to equip totem.")
                .defaultValue(14.0).min(1).max(36).step(0.5)
                .visible(smart::value)
                .build());

        considerAbsorption = config.create(new BooleanValue.Builder("Absorption")
                .description("Include absorption hearts in health checks.")
                .defaultValue(true)
                .build());

        missingArmor = config.create(new BooleanValue.Builder("Missing Armor")
                .description("Equip when any armor slot is empty.")
                .defaultValue(true)
                .visible(smart::value)
                .build());

        predictFall = config.create(new BooleanValue.Builder("Fall Damage")
                .description("Equip when a fall would bring you below the health threshold.")
                .defaultValue(true)
                .visible(smart::value)
                .build());

        smartSwitch = config.create(new BooleanValue.Builder("Smart Switch")
                .description("Use offhand swap packets for hotbar totems.")
                .defaultValue(true)
                .build());

        startDelay = config.create(new IntegerValue.Builder("Start Delay")
                .description("Ticks to wait before the first action.")
                .defaultValue(1).min(0).max(20)
                .build());

        clickDelayMin = config.create(new IntegerValue.Builder("Click Delay Min")
                .description("Minimum ticks between inventory clicks.")
                .defaultValue(2).min(0).max(20)
                .build());

        clickDelayMax = config.create(new IntegerValue.Builder("Click Delay Max")
                .description("Maximum ticks between inventory clicks.")
                .defaultValue(4).min(0).max(20)
                .build());

        closeDelay = config.create(new IntegerValue.Builder("Close Delay")
                .description("Ticks to wait before closing inventory.")
                .defaultValue(1).min(0).max(20)
                .build());

        Events.INVENTORY_SCHEDULE.register(this::onSchedule);
    }

    private void onSchedule(ScheduleInventoryActionEvent event) {
        if(!this.enabled()) return;

        ClientPlayerEntity player = mc();
        if(player == null || player.isSpectator()) return;
        if(InventoryManager.isOperating()) return;

        if(!TotemEvaluator.shouldEquip(
                player,
                this.smart.value(),
                this.healthThreshold.value().floatValue(),
                this.considerAbsorption.value(),
                this.missingArmor.value(),
                this.predictFall.value()
        )) return;

        int totemSlot = this.findTotemSlot(player);
        if(totemSlot == Slots.INVALID_SLOT) return;

        List<InventoryAction> actions = this.buildActions(totemSlot);
        if(actions.isEmpty()) return;

        boolean needsInventory = actions.stream().anyMatch(InventoryAction::requiresInventoryOpen);
        InventoryConstraints constraints = new InventoryConstraints(
                this.startDelay.value(),
                this.clickDelayMin.value(),
                this.clickDelayMax.value(),
                this.closeDelay.value(),
                needsInventory,
                true
        );

        event.schedule(constraints, actions, InventoryPriority.IMPORTANT);
    }

    private List<InventoryAction> buildActions(int totemSlot) {
        if(Slots.HOTBAR.contains(totemSlot) && this.smartSwitch.value())
            return List.of(new InventoryAction.SwapOffhandHotbar(totemSlot));

        List<InventoryAction> actions = new ArrayList<>();
        int sourceId = Slots.indexToID(totemSlot);
        int offhandId = InventoryAction.offhandScreenSlot();

        if(Slots.HOTBAR.contains(totemSlot)) {
            actions.add(InventoryAction.Click.swap(offhandId, totemSlot));
            return actions;
        }

        actions.add(InventoryAction.Click.pickup(sourceId));
        actions.add(InventoryAction.Click.pickup(offhandId));
        return actions;
    }

    private int findTotemSlot(ClientPlayerEntity player) {
        int hotbar = Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.isOf(Items.TOTEM_OF_UNDYING));
        if(hotbar != Slots.INVALID_SLOT) return hotbar;
        return Slots.findFirst(Slots.MAIN, (stack, index) -> stack.isOf(Items.TOTEM_OF_UNDYING));
    }

    private static ClientPlayerEntity mc() {
        return ruby.RubyClient.client.player;
    }
}
