package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Allows you to use GUIs while in nether portals.
 * This is a flag module - its active state is checked by other systems.
 * When enabled, prevents the nether portal overlay from blocking input.
 */
public class Portals extends Module {

    public Portals() {
        super("Portals", "Allows you to use GUIs while in nether portals.", ModuleCategory.PLAYER);
    }
}
