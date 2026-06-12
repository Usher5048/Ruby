package ruby.helpers.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ruby.RubyClient;
import ruby.helpers.Slots;

public sealed interface InventoryAction {
    boolean requiresInventoryOpen();
    boolean perform();

    record Click(int slotId, int button, SlotActionType type) implements InventoryAction {
        @Override
        public boolean requiresInventoryOpen() {
            return true;
        }

        @Override
        public boolean perform() {
            MinecraftClient mc = RubyClient.client;
            ClientPlayerEntity player = mc.player;
            if(player == null || mc.interactionManager == null) return false;

            mc.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    this.slotId,
                    this.button,
                    this.type,
                    player
            );
            return true;
        }

        public static Click quickMove(int slotId) {
            return new Click(slotId, 0, SlotActionType.QUICK_MOVE);
        }

        public static Click pickup(int slotId) {
            return new Click(slotId, 0, SlotActionType.PICKUP);
        }

        public static Click swap(int slotId, int hotbarButton) {
            return new Click(slotId, hotbarButton, SlotActionType.SWAP);
        }

        public static Click throwSlot(int slotId) {
            return new Click(slotId, 1, SlotActionType.THROW);
        }
    }

    record SwapOffhandHotbar(int hotbarSlot) implements InventoryAction {
        @Override
        public boolean requiresInventoryOpen() {
            return false;
        }

        @Override
        public boolean perform() {
            MinecraftClient mc = RubyClient.client;
            ClientPlayerEntity player = mc.player;
            if(player == null || mc.getNetworkHandler() == null) return false;

            int prev = player.getInventory().getSelectedSlot();
            if(prev != this.hotbarSlot)
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.hotbarSlot));

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ORIGIN,
                    Direction.DOWN,
                    0
            ));

            if(prev != this.hotbarSlot)
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prev));

            return true;
        }
    }

    record UseHotbar(int hotbarSlot) implements InventoryAction {
        @Override
        public boolean requiresInventoryOpen() {
            return false;
        }

        @Override
        public boolean perform() {
            MinecraftClient mc = RubyClient.client;
            ClientPlayerEntity player = mc.player;
            if(player == null || mc.interactionManager == null) return false;

            int prev = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(this.hotbarSlot);
            mc.interactionManager.interactItem(player, net.minecraft.util.Hand.MAIN_HAND);
            player.getInventory().setSelectedSlot(prev);
            return true;
        }
    }

    static int offhandScreenSlot() {
        return Slots.indexToID(Slots.OFFHAND);
    }
}
