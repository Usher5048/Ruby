package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;

public final class AutoToolVisualSlotSpoof {
    private AutoToolVisualSlotSpoof() {
    }

    public static int beginVisualSwap() {
        if (!AutoTool.silentSwapped || AutoTool.visualSlot < 0) return -1;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return -1;

        PlayerInventory inv = mc.player.getInventory();
        int savedSlot = inv.getSelectedSlot();
        inv.setSelectedSlot(AutoTool.visualSlot);
        return savedSlot;
    }

    public static void endVisualSwap(int savedSlot) {
        if (savedSlot < 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.getInventory().setSelectedSlot(savedSlot);
    }
}
