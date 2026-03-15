package ruby.systems.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.systems.config.Configuration;

import java.util.Locale;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory type;

    protected String origin;
    protected boolean enabled = false;
    protected boolean showToasts = true;
    public boolean keyHeld = false;

    public final Configuration config;
    private int keyCode = -1;

    public Module(String name, String description, ModuleCategory type) {
        this.name = name;
        this.description = description;
        this.type = type;

        this.origin = RubyClient.MOD_NAME;
        this.config = new Configuration();
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

    public int keyCode() {
        return this.keyCode;
    }
    public void keyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public String getKeyName() {
        if (this.keyCode < 0) return "-";
        String keyName = GLFW.glfwGetKeyName(this.keyCode, 0);
        if (keyName != null && !keyName.isBlank()) {
            return keyName.toUpperCase(Locale.ROOT);
        }

        return switch (this.keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BSP";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> String.valueOf(this.keyCode);
        };
    }

    public boolean showsToasts() {
        return this.showToasts;
    }
    public void showsToasts(boolean showToasts) {
        this.showToasts = showToasts;
    }
    public void notifyRaw(Text message) {
        this.notifyRaw(message, false);
    }
    public void notifyRaw(Text message, boolean actionBar) {
        RubyClient.notifyUserRaw(
                Text.literal(!actionBar ? "§8[§7" + this.name() + "§8]§r " : "").append(message),
                actionBar
        );
    }

    public void notify(String message) {
        this.notify(message, false);
    }
    public void notify(String message, boolean actionBar) {
        RubyClient.notifyUserRaw(
                Text.literal(!actionBar ? "§8[§7" + this.name() + "§8]§r " : "").append(message),
                actionBar
        );
    }


    public void tick() {}
    public void onPreTick() {}
    public void onPreMovementPackets() {}
    public void onEnable() {}
    public void onDisable() {}
    public void onRender3D() {}
    public void onRender2D(DrawContext context) {}
}
