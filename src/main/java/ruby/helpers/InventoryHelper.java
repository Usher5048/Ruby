package ruby.helpers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class InventoryHelper {
    private static boolean owned;
    private static boolean locked;

    private InventoryHelper() {}

    public static boolean isOpen(Minecraft mc) {
        return mc.gui.screen() instanceof InventoryScreen;
    }

    public static boolean isOwned() {
        return owned;
    }

    public static boolean isLocked() {
        return locked;
    }

    public static boolean canStart(Minecraft mc) {
        return mc.gui.screen() == null;
    }

    public static boolean isModuleSession(Minecraft mc) {
        return owned && isOpen(mc);
    }

    public static void openOwned(Minecraft mc) {
        if(mc.player == null) return;
        if(!isOpen(mc)) mc.setScreenAndShow(new InventoryScreen(mc.player));
        owned = true;
    }

    public static void lock() {
        locked = true;
    }

    public static void unlock() {
        locked = false;
    }

    public static void closeOwned(Minecraft mc) {
        if(owned && isOpen(mc)) mc.setScreenAndShow(null);
        owned = false;
        locked = false;
    }

    public static void releaseOwnership() {
        owned = false;
        locked = false;
    }
}
