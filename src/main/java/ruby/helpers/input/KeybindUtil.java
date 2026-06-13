package ruby.helpers.input;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;

public final class KeybindUtil {

    private KeybindUtil() {}

    public static boolean isBindingDown(KeyBinding binding) {
        long window = RubyClient.client.getWindow().getHandle();
        InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        return GLFW.glfwGetKey(window, key.getCode()) == GLFW.GLFW_PRESS;
    }
}
