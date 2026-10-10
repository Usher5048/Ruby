package ruby.helpers.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import ruby.RubyClient;
import ruby.helpers.Slots;

public sealed interface InventoryAction {
    boolean requiresInventoryOpen();
    boolean perform();

    record Click(int slotId, int button, ContainerInput type) implements InventoryAction {
        @Override
        public boolean requiresInventoryOpen() {
            return true;
        }

        @Override
        public boolean perform() {
            Minecraft mc = RubyClient.client;
            LocalPlayer player = mc.player;
            if(player == null || mc.gameMode == null) return false;

            mc.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    this.slotId,
                    this.button,
                    this.type,
                    player
            );
            return true;
        }

        public static Click quickMove(int slotId) {
            return new Click(slotId, 0, ContainerInput.QUICK_MOVE);
        }

        public static Click pickup(int slotId) {
            return new Click(slotId, 0, ContainerInput.PICKUP);
        }

        public static Click swap(int slotId, int hotbarButton) {
            return new Click(slotId, hotbarButton, ContainerInput.SWAP);
        }

        public static Click throwSlot(int slotId) {
            return new Click(slotId, 1, ContainerInput.THROW);
        }
    }

    record SwapOffhandHotbar(int hotbarSlot) implements InventoryAction {
        @Override
        public boolean requiresInventoryOpen() {
            return false;
        }

        @Override
        public boolean perform() {
            Minecraft mc = RubyClient.client;
            LocalPlayer player = mc.player;
            if(player == null || mc.getConnection() == null) return false;

            int prev = player.getInventory().getSelectedSlot();
            if(prev != this.hotbarSlot)
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(this.hotbarSlot));

            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO,
                    Direction.DOWN,
                    0
            ));

            if(prev != this.hotbarSlot)
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(prev));

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
            Minecraft mc = RubyClient.client;
            LocalPlayer player = mc.player;
            if(player == null || mc.gameMode == null) return false;

            int prev = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(this.hotbarSlot);
            mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.getInventory().setSelectedSlot(prev);
            return true;
        }
    }

    static int offhandScreenSlot() {
        return Slots.indexToID(Slots.OFFHAND);
    }
}
