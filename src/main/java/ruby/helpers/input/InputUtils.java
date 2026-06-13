package ruby.helpers.input;

import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import ruby.mixin.KeyBindingAccessor;

public class InputUtils {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] mouseButtons = new boolean[16];

    public static void setKeyState(int key, boolean pressed) {
        if(key >= 0 && key < keys.length)
            keys[key] = pressed;
    }

    public static void setMouseState(int button, boolean pressed) {
        if(button >= 0 && button < mouseButtons.length)
            mouseButtons[button] = pressed;
    }

    public static boolean isMousePressed(int button) {
        if(button == GLFW.GLFW_KEY_UNKNOWN) return false;
        if(button < 0 || button >= mouseButtons.length) return false;

        return mouseButtons[button];
    }

    public static boolean isKeyPressed(int key) {
        if(key == GLFW.GLFW_KEY_UNKNOWN) return false;
        if(key < 0 || key >= keys.length) return false;

        return keys[key];
    }

    public static boolean isKeyPressed(KeyBinding bind) {
        return InputUtils.isKeyPressed(((KeyBindingAccessor) bind).getBoundKey().getCode());
    }
}
