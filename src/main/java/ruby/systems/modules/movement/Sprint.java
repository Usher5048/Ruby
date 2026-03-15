package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Sprint: Two modes - Strict (vanilla sprint conditions) and Rage (always sprint).
 * keepSprint prevents sprint from being reset on hit (Meteor uses a mixin for this;
 * the tick-based approach sets sprinting every tick to counteract resets).
 */
public class Sprint extends Module {
    public enum Mode { Strict, Rage }

    private final EnumValue<Mode> mode;

    public Sprint() {
        super("Sprint", "Automatically sprints for you.", ModuleType.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Sprinting mode.")
                .defaultValue(Mode.Strict)
                .build());

        config.create(new BooleanValue.Builder("Keep Sprint")
                .description("Keeps sprinting even after being hit.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (shouldSprint(player)) {
            player.setSprinting(true);
        }
    }

    private boolean shouldSprint(ClientPlayerEntity player) {
        if (player.forwardSpeed <= 0) return false;

        if (mode.value() == Mode.Rage) return true;

        // Strict: respect vanilla sprint conditions
        return !player.isSneaking()
                && !player.isUsingItem()
                && player.getHungerManager().getFoodLevel() > 6;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.setSprinting(false);
    }
}
