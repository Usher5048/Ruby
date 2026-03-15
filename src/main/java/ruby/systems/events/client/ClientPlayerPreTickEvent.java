package ruby.systems.events.client;

import ruby.systems.events.Event;

public final class ClientPlayerPreTickEvent extends Event {
    private static final ClientPlayerPreTickEvent INSTANCE = new ClientPlayerPreTickEvent();

    private ClientPlayerPreTickEvent() {
    }

    public static ClientPlayerPreTickEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
