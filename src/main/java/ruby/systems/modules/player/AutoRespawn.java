package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Automatically respawns when the player dies.
 * Waits a configurable delay (in ticks) before pressing the respawn button.
 */
public class AutoRespawn extends Module {

    private final IntegerValue delay;

    private int deathTimer = 0;

    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawns when you die.", ModuleCategory.PLAYER);

        delay = config.create(new IntegerValue.Builder("Delay")
                .description("Delay in ticks before respawning.")
                .defaultValue(0).min(0).max(100)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mc.currentScreen instanceof DeathScreen) {
            deathTimer++;
            if (deathTimer > delay.value()) {
                mc.player.requestRespawn();
                mc.setScreen(null);
                deathTimer = 0;
            }
        } else {
            deathTimer = 0;
        }
    }
}
