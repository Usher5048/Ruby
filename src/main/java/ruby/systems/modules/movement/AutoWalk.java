package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.gui.screen() != null) return;

        mc.options.keyUp.setDown(true);
        if (autoSprint.value()) {
            mc.options.keySprint.setDown(true);
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.options.keyUp.setDown(false);
        if (autoSprint.value()) {
            mc.options.keySprint.setDown(false);
        }
    }
}
