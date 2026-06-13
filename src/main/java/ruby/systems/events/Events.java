package ruby.systems.events;

import ruby.helpers.inventory.ScheduleInventoryActionEvent;
import ruby.systems.events.blink.BlinkPacketEvent;
import ruby.systems.events.chat.ChatEvent;
import ruby.systems.events.chat.ChatEvents;
import ruby.systems.events.client.UseCooldownEvent;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;

public final class Events {
    public static class GenericEvent extends Event {
        private static final GenericEvent INSTANCE = new GenericEvent();
        private GenericEvent() {}

        public static GenericEvent get() {
            return GenericEvent.INSTANCE;
        }
    }

    public static final EventBuses.Many<PacketEvents,  PacketEvent> PACKET = new EventBuses.Many<>();
    public static final EventBuses.Many<EntityEvents,  EntityEvent> ENTITY = new EventBuses.Many<>();
    public static final EventBuses.Many<  TickEvents, GenericEvent> TICK   = new EventBuses.Many<>();
    public static final EventBuses.Many<  ChatEvents,    ChatEvent> CHAT   = new EventBuses.Many<>();

    public static final EventBuses.Single<Render2DEvent> RENDER2D = new EventBuses.Single<>();
    public static final EventBuses.Single<GenericEvent> RENDER3D = new EventBuses.Single<>();
    public static final EventBuses.Single<ScheduleInventoryActionEvent> INVENTORY_SCHEDULE = new EventBuses.Single<>();
    public static final EventBuses.Single<BlinkPacketEvent> BLINK = new EventBuses.Single<>();
    public static final EventBuses.Single<UseCooldownEvent> USE_COOLDOWN = new EventBuses.Single<>();
}
