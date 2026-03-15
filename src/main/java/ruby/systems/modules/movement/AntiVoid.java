package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;
import ruby.systems.modules.Modules;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Prevents the player from falling into the void.
 * - Jump: Makes the player jump when near the void
 * - Flight: Enables the Fly module when near the void
 */
public class AntiVoid extends Module {

    public enum Mode { Jump, Flight }

    private final EnumValue<Mode> mode;

    private boolean wasFlightEnabled = false;
    private boolean hasRun = false;

    public AntiVoid() {
        super("Anti Void", "Attempts to prevent you from falling into the void.", ModuleCategory.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The method to prevent falling into the void.")
                .defaultValue(Mode.Jump)
                .build());
    }

    @Override
    public void onEnable() {
        if (mode.value() == Mode.Flight) {
            Fly flyModule = Modules.getByClass(Fly.class);
            wasFlightEnabled = flyModule != null && flyModule.enabled();
        }
        hasRun = false;
    }

    @Override
    public void onDisable() {
        if (!wasFlightEnabled && mode.value() == Mode.Flight) {
            Fly flyModule = Modules.getByClass(Fly.class);
            if (flyModule != null && flyModule.enabled()) {
                Modules.disable(flyModule);
            }
        }
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        int minY = mc.world.getBottomY();

        // Only activate when near the void
        if (player.getY() > minY || player.getY() < minY - 15) {
            if (hasRun && mode.value() == Mode.Flight) {
                Fly flyModule = Modules.getByClass(Fly.class);
                if (flyModule != null) Modules.disable(flyModule);
                hasRun = false;
            }
            return;
        }

        switch (mode.value()) {
            case Flight -> {
                Fly flyModule = Modules.getByClass(Fly.class);
                if (flyModule != null) Modules.enable(flyModule);
                hasRun = true;
            }
            case Jump -> player.jump();
        }
    }
}
