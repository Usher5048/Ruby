package ruby.systems.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import ruby.RubyClient;
import ruby.systems.config.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory type;

    protected String origin;
    protected boolean enabled = false;
    protected boolean showToasts = true;

    public final Configuration config;
    public Keybind keybind;

    public Module(String name, String description, ModuleCategory type) {
        this.name = name;
        this.description = description;
        this.type = type;

        this.origin = RubyClient.MOD_NAME;
        this.config = new Configuration();
        this.keybind = Keybind.unbound();
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
    public void notifyUser(String message) {
        this.notifyUser(message, false);
    }
    public void notifyUser(String message, boolean actionBar) {
        this.notifyUser(Text.of(message), actionBar);
    }
    public void notifyUser(Text message) {
        this.notifyUser(message, false);
    }
    public void notifyUser(Text message, boolean actionBar) {
        RubyClient.notifyUser(
                Text.empty()
                        .append(!actionBar ? Text.literal("[").withColor(0x666666) : Text.empty())
                        .append(!actionBar ? Text.literal(this.name()).withColor(0x999999) : Text.empty())
                        .append(!actionBar ? Text.literal("] ").withColor(0x666666) : Text.empty())
                        .append(message),
                actionBar
        );
    }


    public void tick() {}
    public void onEnable() {}
    public void onDisable() {}
    public void onRender3D() {}
    public void onRender2D(DrawContext context) {}

    @Override
    public String toString() {
        return Arrays.stream(name.split("-"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }
}
