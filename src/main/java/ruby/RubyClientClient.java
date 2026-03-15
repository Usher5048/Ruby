package ruby;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.ConfigManager;
import ruby.systems.events.Events;
import ruby.systems.gui.ClickGUI;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;
import ruby.systems.modules.combat.Hitboxes;

import java.util.ArrayList;

public class RubyClientClient implements ClientModInitializer {
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
            Identifier.of(RubyClient.MOD_ID, "gui")
    );
    private static final KeyBinding OPEN_CLICK_GUI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.ruby.open_click_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KEY_CATEGORY
    ));

    private static boolean ruby$mixinHooksRegistered;

    @Override
    public void onInitializeClient() {
        registerMixinHooks();

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> ConfigManager.saveState());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            for (Module module : Modules.getModules()) {
                if (module.keyCode() > 0 && InputUtil.isKeyPressed(
                        RubyClient.client.getWindow(), module.keyCode())) {
                    if (!module.keyHeld) {
                        module.keyHeld = true;
                        Modules.toggle(module);
                    }
                } else {
                    module.keyHeld = false;
                }
            }

            for (Module module : new ArrayList<>(Modules.getActiveModules())) {
                module.tick();
            }

            while (OPEN_CLICK_GUI.wasPressed()) {
                client.setScreen(new ClickGUI());
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            for (Module module : new ArrayList<>(Modules.getActiveModules())) {
                module.onRender2D(drawContext);
            }
        });
    }

    private static void registerMixinHooks() {
        if (ruby$mixinHooksRegistered) return;

        Events.CLIENT_PLAYER_PRE_TICK.register(event -> {
            for (Module module : new ArrayList<>(Modules.getActiveModules())) {
                module.onPreTick();
            }
        });

        Events.SEND_MOVEMENT_PACKETS_PRE.register(event -> {
            for (Module module : new ArrayList<>(Modules.getActiveModules())) {
                module.onPreMovementPackets();
            }
        });

        Events.ATTACK_ENTITY.register(event -> Hitboxes.onBeforeAttack(event.player, event.target));

        ruby$mixinHooksRegistered = true;
    }
}