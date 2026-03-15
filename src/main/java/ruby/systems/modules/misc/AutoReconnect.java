package ruby.systems.modules.misc;

import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's AutoReconnect: Automatically reconnects to the last server after being disconnected.
 * Requires a mixin to net.minecraft.client.gui.screen.DisconnectedScreen to add
 * a reconnect button and auto-reconnect timer.
 * <p>
 * TODO: Add mixin to DisconnectedScreen to implement auto-reconnect functionality.
 */
public class AutoReconnect extends Module {
    private final DoubleValue delay;

    public static AutoReconnect INSTANCE;

    public AutoReconnect() {
        super("Auto Reconnect", "Automatically reconnects when disconnected.", ModuleCategory.MISC);
        INSTANCE = this;

        delay = config.create(new DoubleValue.Builder("Delay")
                .description("Delay in seconds before reconnecting.")
                .defaultValue(3.5).min(0.5).max(60).step(0.5)
                .build());
    }

    /**
     * Called by the DisconnectedScreen mixin to get the reconnect delay.
     * Returns the delay in milliseconds.
     */
    public static long getDelayMs() {
        if (INSTANCE == null || !INSTANCE.enabled()) return -1;
        return (long) (INSTANCE.delay.value() * 1000);
    }
}
