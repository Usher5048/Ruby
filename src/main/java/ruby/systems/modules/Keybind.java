package ruby.systems.modules;

import com.mojang.blaze3d.platform.InputConstants;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;

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

        if(isKey) return code != InputConstants.KEY_ESCAPE;
        return code != InputConstants.MOUSE_BUTTON_LEFT && code != InputConstants.MOUSE_BUTTON_RIGHT;
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
        if(this.isUnbound() || RubyClient.client.gui.screen() != null) return false;

        if(!this.isKey) return InputUtils.isMousePressed(this.code);
        return InputUtils.isKeyPressed(this.code) || Keybind.isScanDown(this.code);
    }

    private static boolean isScanDown(int code) {
        try {
            return InputConstants.isKeyDown(code);
        } catch (IndexOutOfBoundsException ignored) {
            return false;
        }
    }

    public boolean isPressed() {
        if(RubyClient.client.gui.screen() != null) return false;
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
        if(RubyClient.client.gui.screen() != null) return false;
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
        if(!this.isKey) return "MB" + this.code;

        InputConstants.Key key = InputConstants.Type.KEYBOARD.getOrCreate(this.code);
        String name = key.getDisplayName().getString();
        if(name.isBlank() || name.equals(key.getName()) || name.startsWith("key."))
            return "KEY." + this.code;

        return name;
    }
}
