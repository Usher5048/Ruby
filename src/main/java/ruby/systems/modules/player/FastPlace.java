package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.mixin.MinecraftClientAccessor;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Allows you to use/place items faster by reducing the item use cooldown.
 * Each tick forces the client's item use cooldown to 0 (or a low value),
 * effectively letting you place blocks at maximum speed.
 */
public class FastPlace extends Module {

    private final IntegerValue cooldown;

    public FastPlace() {
        super("Fast Place", "Allows you to place items faster.", ModuleType.PLAYER);

        cooldown = config.create(new IntegerValue.Builder("Cooldown")
                .description("Item use cooldown in ticks (0 = fastest).")
                .defaultValue(0).min(0).max(4)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        ((MinecraftClientAccessor) mc).ruby$setItemUseCooldown(cooldown.value());
    }
}
