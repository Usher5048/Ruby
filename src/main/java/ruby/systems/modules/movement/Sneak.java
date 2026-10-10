package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Forces the player to sneak by pressing the sneak key.
 */
public class Sneak extends Module {

    public Sneak() {
        super("Sneak", "Automatically sneaks for you.", ModuleType.MOVEMENT);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (player.getAbilities().flying) return;

        mc.options.keyShift.setDown(true);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.options.keyShift.setDown(false);
    }
}
