package ruby.systems.events.client;

import ruby.systems.events.Event;

public final class SendMovementPacketsEvent {
    private SendMovementPacketsEvent() {
    }

    public static final class Pre extends Event {
        private static final Pre INSTANCE = new Pre();

        private Pre() {
        }

        public static Pre get() {
            INSTANCE.setCancelled(false);
            return INSTANCE;
        }
    }
}
