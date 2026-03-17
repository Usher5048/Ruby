package ruby.systems.modules;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;

public class Keybind {
    public static final int UNKNOWN = Keybind.CODE_MASK;

    private static final int CODE_MASK = 0x3FFFFFFF;
    private static final int KEY_FLAG  = 0x80000000;
    private static final int TOR_FLAG  = 0x40000000;

    private int code;
    private boolean isKey;
    private boolean tor;

    private boolean wasPressed0;
    private boolean wasPressed1;

    private Keybind(int code, boolean isKey, boolean tor) {
        this.code = code & Keybind.CODE_MASK;
        this.isKey = isKey;
        this.tor = tor;
    }

    public static Keybind unbound() {
        return new Keybind(Keybind.UNKNOWN, true, false);
    }

    public boolean togglesOnRelease() {
        return this.tor;
    }
    public void togglesOnRelease(boolean tor) {
        this.tor = tor;
    }

    public static boolean canBindTo(int code, boolean isKey) {
        code = code & Keybind.CODE_MASK;
        if(code == Keybind.UNKNOWN) return false;

        if(isKey) return code != GLFW.GLFW_KEY_ESCAPE;
        return code != GLFW.GLFW_MOUSE_BUTTON_LEFT && code != GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    public void unbind() {
        this.code = Keybind.UNKNOWN;
    }
    public void key(int code) {
        this.bind(code, true);
    }
    public void mouse(int code) {
        this.bind(code, false);
    }
    public void bind(int code, boolean isKey) {
        this.code = code & Keybind.CODE_MASK;
        this.isKey = isKey;
    }

    public int serialize() {
        return (this.isKey ? Keybind.KEY_FLAG : 0) |
                (this.tor ? Keybind.TOR_FLAG : 0) |
                this.code;
    }

    public void deserialize(int d) {
        this.code = d & Keybind.CODE_MASK;
        this.isKey = (d & Keybind.KEY_FLAG) != 0;
        this.tor = (d & Keybind.TOR_FLAG) != 0;
    }

    public boolean isUnbound() {
        return this.code == Keybind.UNKNOWN;
    }
    public boolean isHeld() {
        if(this.code < 0) return false;

        if(!this.isKey) {
            return GLFW.glfwGetMouseButton(RubyClient.client.getWindow().getHandle(), this.code) == GLFW.GLFW_PRESS
                    && RubyClient.client.currentScreen == null;
        }

        return InputUtil.isKeyPressed(RubyClient.client.getWindow(), this.code)
                && RubyClient.client.currentScreen == null;
    }

    public boolean isPressed() {
        if(RubyClient.client.currentScreen != null) return false;
        if(this.code < 0) return false;

        boolean isHeld = this.isHeld();

        if(isHeld && !this.wasPressed0) {
            this.wasPressed0 = true;
            return true;
        }

        if(!isHeld) this.wasPressed0 = false;
        return false;
    }

    public boolean wasPressed() {
        if(RubyClient.client.currentScreen != null) return false;
        if(this.code < 0) return false;

        boolean isHeld = this.isHeld();

        if(!isHeld && this.wasPressed1) {
            this.wasPressed1 = false;
            return true;
        }

        if(isHeld) this.wasPressed1 = true;
        return false;
    }

    @Override
    public String toString() {
        if(this.isUnbound()) return "None";

        if(this.isKey) {
            String keyname = GLFW.glfwGetKeyName(this.code, GLFW.glfwGetKeyScancode(this.code));
            return keyname != null ? keyname : "key." + this.code;
        }

        return "Mouse Button " + (this.code + 1);
    }
}
