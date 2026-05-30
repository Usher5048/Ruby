package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import ruby.mixin.ClientPlayerInteractionManagerAccessor;

public final class AutoToolServerSlot {
    private static boolean applyingMiningSlot;

    private AutoToolServerSlot() {
    }

    public static boolean isApplyingMiningSlot() {
        return applyingMiningSlot;
    }

    /** Keeps the real selected slot (and server sync) on the mining tool. */
    public static void applyMiningSlot(ClientPlayerEntity player, int slot) {
        if (player == null || slot < 0 || slot > 8) return;

        PlayerInventory inv = player.getInventory();
        if (inv.getSelectedSlot() == slot) return;

        applyingMiningSlot = true;
        try {
            inv.setSelectedSlot(slot);
        } finally {
            applyingMiningSlot = false;
        }

        syncSelectedSlot();
    }

    /** Restores the player's visible slot to the server when mining ends. */
    public static void restoreVisualSlot(ClientPlayerEntity player, int slot) {
        if (player == null || slot < 0 || slot > 8) return;

        PlayerInventory inv = player.getInventory();
        if (inv.getSelectedSlot() == slot) {
            syncSelectedSlot();
            return;
        }

        applyingMiningSlot = true;
        try {
            inv.setSelectedSlot(slot);
        } finally {
            applyingMiningSlot = false;
        }

        syncSelectedSlot();
    }

    private static void syncSelectedSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager != null) {
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).ruby$syncSelectedSlot();
        }
    }
}
