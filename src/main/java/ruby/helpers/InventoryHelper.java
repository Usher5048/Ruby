package ruby.helpers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public final class InventoryHelper {
    private static boolean owned;
    private static boolean locked;

    private InventoryHelper() {}

    public static boolean isOpen(MinecraftClient mc) {
        return mc.currentScreen instanceof InventoryScreen;
    }

    public static boolean isOwned() {
        return owned;
    }

    public static boolean isLocked() {
        return locked;
    }

    public static boolean canStart(MinecraftClient mc) {
        return mc.currentScreen == null;
    }

    public static boolean isModuleSession(MinecraftClient mc) {
        return owned && isOpen(mc);
    }

    public static void openOwned(MinecraftClient mc) {
        if(mc.player == null) return;
        if(!isOpen(mc)) mc.setScreen(new InventoryScreen(mc.player));
        owned = true;
    }

    public static void lock() {
        locked = true;
    }

    public static void unlock() {
        locked = false;
    }

    public static void closeOwned(MinecraftClient mc) {
        if(owned && isOpen(mc)) mc.setScreen(null);
        owned = false;
        locked = false;
    }

    public static void releaseOwnership() {
        owned = false;
        locked = false;
    }
}
