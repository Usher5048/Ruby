package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Core logic: find Totem of Undying in inventory, move to offhand.
 * Meteor's modes: Smart (only when health <threshold), Strict (always).
 * Uses clickSlot inventory operations for server-side validity.
 */
public class AutoTotem extends Module {
    private final BooleanValue smart;
    private final DoubleValue healthThreshold;

    public AutoTotem() {
        super("Auto Totem", "Automatically moves totems to your offhand.", ModuleCategory.COMBAT);

        smart = config.create(new BooleanValue.Builder("Smart")
                .description("Only equips totem when health is below threshold.")
                .defaultValue(false)
                .build());

        healthThreshold = config.create(new DoubleValue.Builder("Health")
                .description("Health threshold to equip totem in smart mode.")
                .defaultValue(10.0).min(1).max(36).step(0.5)
                .visible(smart::value)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        // Check if offhand already has totem
        ItemStack offhand = player.getOffHandStack();
        boolean locked = false;
        if (offhand.isOf(Items.TOTEM_OF_UNDYING)) {
            locked = false;
            return;
        }

        // Smart mode: only equip when low health
        if (smart.value() && player.getHealth() + player.getAbsorptionAmount() > healthThreshold.value()) {
            return;
        }

        // Find totem in inventory
        int totemSlot = findTotem(player);
        if (totemSlot == -1) return;

        // Move totem to offhand
        moveToOffhand(mc, player, totemSlot);
        locked = true;
    }

    private int findTotem(ClientPlayerEntity player) {
        // Search hotbar first (0-8), then main inventory (9-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    private void moveToOffhand(MinecraftClient mc, ClientPlayerEntity player, int invSlot) {
        int syncId = player.currentScreenHandler.syncId;

        if (invSlot < 9) {
            // Hotbar slot: swap with offhand using SWAP action (button = hotbar slot)
            mc.interactionManager.clickSlot(syncId, 45, invSlot, SlotActionType.SWAP, player);
        } else {
            // Main inventory slot 9-35: pickup then place in offhand
            mc.interactionManager.clickSlot(syncId, invSlot, 0, SlotActionType.PICKUP, player);
            mc.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, player);
        }
    }
}
