package ruby.systems.modules;

import ruby.RubyClient;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory type;

    protected String origin;
    protected boolean enabled = false;
    protected boolean showToasts = true;

//    public Keybind keybind;
//    public Configuration config;

    public Module(String name, String description, ModuleCategory type) {
        this.name = name;
        this.description = description;
        this.type = type;

        this.origin = RubyClient.MOD_NAME;
//        this.config = new Configuration();
//        this.keybind = Keybind.unbound();
    }

    public String name() {
        return this.name;
    }
    public String description() {
        return this.description;
    }
    public ModuleCategory category() {
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

    public void tick() {}
    public void onEnable() {}
    public void onDisable() {}
    public void onRender3D() {}
    public void onRender2D() {}
}
