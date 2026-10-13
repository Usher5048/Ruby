package ruby.systems.modules;

import ruby.RubyClient;
import ruby.systems.config.Configuration;
import ruby.systems.events.Render2DEvent;
import ruby.systems.gui.Toasts;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleType type;

    protected String origin;
    protected boolean enabled = false;
    protected boolean showToasts = true;
    private final int hudColor;

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
