package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Allows you to move, jump, sneak, and sprint while in GUI screens.
 * Works by pressing movement keys based on their keybind state each tick
 * when a screen is open.
 */
public class GUIMove extends Module {

    private final BooleanValue jump;
    private final BooleanValue sneak;
    private final BooleanValue sprint;

    public GUIMove() {
        super("GUI Move", "Allows you to move while in GUIs.", ModuleType.MOVEMENT);

        jump = config.create(new BooleanValue.Builder("Jump")
                .description("Allows you to jump while in GUIs.")
                .defaultValue(true)
                .build());

        sneak = config.create(new BooleanValue.Builder("Sneak")
                .description("Allows you to sneak while in GUIs.")
                .defaultValue(true)
                .build());

        sprint = config.create(new BooleanValue.Builder("Sprint")
                .description("Allows you to sprint while in GUIs.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.currentScreen == null) return;

        // Pass-through movement keys when a GUI is open
        passKey(mc.options.forwardKey);
        passKey(mc.options.backKey);
        passKey(mc.options.leftKey);
        passKey(mc.options.rightKey);

        if (jump.value()) passKey(mc.options.jumpKey);
        if (sneak.value()) passKey(mc.options.sneakKey);
        if (sprint.value()) passKey(mc.options.sprintKey);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);

        if (jump.value()) mc.options.jumpKey.setPressed(false);
        if (sneak.value()) mc.options.sneakKey.setPressed(false);
        if (sprint.value()) mc.options.sprintKey.setPressed(false);
    }

    private void passKey(KeyBinding bind) {
        bind.setPressed(org.lwjgl.glfw.GLFW.glfwGetKey(
                MinecraftClient.getInstance().getWindow().getHandle(),
                ((net.minecraft.client.util.InputUtil.Key) getKeyFromBinding(bind)).getCode()
        ) == org.lwjgl.glfw.GLFW.GLFW_PRESS);
    }

    private static Object getKeyFromBinding(KeyBinding bind) {
        return net.minecraft.client.util.InputUtil.fromTranslationKey(bind.getBoundKeyTranslationKey());
    }
}
