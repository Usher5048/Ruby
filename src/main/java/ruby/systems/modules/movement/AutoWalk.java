package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Automatically walks forward without holding the key.
 * Optionally also sprints.
 */
public class AutoWalk extends Module {

    private final BooleanValue autoSprint;

    public AutoWalk() {
        super("Auto Walk", "Automatically walks forward.", ModuleType.MOVEMENT);

        autoSprint = config.create(new BooleanValue.Builder("Sprint")
                .description("Automatically sprint while walking.")
                .defaultValue(false)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (mc.currentScreen != null) return;

        mc.options.forwardKey.setPressed(true);
        if (autoSprint.value()) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.options.forwardKey.setPressed(false);
        if (autoSprint.value()) {
            mc.options.sprintKey.setPressed(false);
        }
    }
}
