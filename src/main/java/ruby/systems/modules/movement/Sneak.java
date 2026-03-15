package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Forces the player to sneak by pressing the sneak key.
 */
public class Sneak extends Module {

    public Sneak() {
        super("Sneak", "Automatically sneaks for you.", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (player.getAbilities().flying) return;

        mc.options.sneakKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.options.sneakKey.setPressed(false);
    }
}
