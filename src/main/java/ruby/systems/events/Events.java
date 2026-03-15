package ruby.systems.events;

import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.events.tick.TickEvents;

public final class Events {
    public static class GenericEvent extends Event {}

    public static final EventBuses.Many<PacketEvents,  PacketEvent> PACKET = new EventBuses.Many<>();
    public static final EventBuses.Many<EntityEvents,  EntityEvent> ENTITY = new EventBuses.Many<>();
    public static final EventBuses.Many<  TickEvents, GenericEvent> TICK   = new EventBuses.Many<>();
}
