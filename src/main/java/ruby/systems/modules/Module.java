package ruby.systems.modules;

import net.minecraft.network.chat.Component;
import ruby.RubyClient;
import ruby.systems.config.Configuration;
import ruby.systems.events.Render2DEvent;
import ruby.systems.gui.Toasts;

public class Module {
    private final String name;
    private final String description;
    private final ModuleType type;
    private final int hudColor;

    protected String origin;
    protected boolean enabled = false;
    protected boolean showToasts = true;

    public final Configuration config;
    public Keybind keybind;

    protected Module(String name, String description, ModuleType type) {
        this.name = name;
        this.description = description;
        this.type = type;

        this.origin = RubyClient.MOD_NAME;
        this.config = new Configuration();
        this.keybind = Keybind.unbound();
        this.hudColor = Module.colorFromName(name);
    }

    /** Optional suffix shown next to the module name on the active-modules HUD. */
    public String getInfoString() {
        return null;
    }

    /** Stable per-module color used by the active-modules HUD random color mode. */
    public int hudColor() {
        return this.hudColor;
    }

    private static int colorFromName(String name) {
        int hash = name.hashCode();
        float hue = ((hash & 0x7FFFFFFF) % 360) / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.55f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public String name() {
        return this.name;
    }
    public String description() {
        return this.description;
    }
    public ModuleType category() {
        return this.type;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public boolean showsToasts() {
        return this.showToasts;
    }
    public void showsToasts(boolean showToasts) {
        this.showToasts = showToasts;
    }
    /**
     * A toast from this module. It uses the module name as the second line
     * and stays quiet when Show toasts is off.
     */
    public Toasts.Toast toast(String title) {
        return Toasts.toast(title).source(this.name()).when(this::showsToasts);
    }

    public void notifyUser(String message) {
        this.toast(message).show();
    }
    public void notifyUser(Component message, boolean inChat) {
        if(!inChat) {
            this.toast(message.getString()).show();
            return;
        }

        RubyClient.notifyUser(Component.empty()
                .append(Component.literal("[").withColor(0x666666))
                .append(Component.literal(this.name()).withColor(0x999999))
                .append(Component.literal("] ").withColor(0x666666))
                .append(message), true);
    }

    public boolean isDefault() {
        if(!this.keybind.isUnbound()) return false;
        if(!this.showsToasts()) return false;
        if(this.enabled()) return false;

        for(String key : this.config.getAll())
            if(!this.config.get(key).isDefault()) return false;

        return true;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public void tick() {}
    public void tickEnd() {}
    public void onEnable() {}
    public void onDisable() {}
    public void render2D(Render2DEvent event) {}
    public void render3D() {}
}
