package ruby.systems.modules.world;

import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Speeds up client ticks via {@link ruby.mixin.RenderTickCounterDynamicMixin}.
 */
public class Timer extends Module {
    private final DoubleValue multiplier;

    public static Timer INSTANCE;

    public Timer() {
        super("Timer", "Changes the speed of the game.", ModuleType.WORLD);
        INSTANCE = this;

        multiplier = config.create(new DoubleValue.Builder("Multiplier")
                .description("Game speed multiplier.")
                .defaultValue(1.0).min(0.1).max(10.0).step(0.1)
                .build());
    }

    /**
     * Called by the RenderTickCounter mixin to get the timer multiplier.
     * Returns 1.0 if not enabled.
     */
    public static double getMultiplier() {
        if (INSTANCE == null || !INSTANCE.enabled()) return 1.0;
        return INSTANCE.multiplier.value();
    }

    @Override
    public String getInfoString() {
        return String.format("%.1f", this.multiplier.value());
    }
}
