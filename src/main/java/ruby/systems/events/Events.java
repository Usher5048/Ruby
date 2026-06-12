package ruby.systems.events;

import ruby.helpers.inventory.ScheduleInventoryActionEvent;
import ruby.systems.events.chat.ChatEvent;
import ruby.systems.events.chat.ChatEvents;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.events.render.Render2DEvent;
import ruby.systems.events.render.Render3DEvent;
import ruby.systems.events.tick.TickEvents;

public final class Events {
    public static class GenericEvent extends Event {}

    public static final EventBuses.Many<PacketEvents,  PacketEvent> PACKET = new EventBuses.Many<>();
    public static final EventBuses.Many<EntityEvents,  EntityEvent> ENTITY = new EventBuses.Many<>();
    public static final EventBuses.Many<  TickEvents, GenericEvent> TICK   = new EventBuses.Many<>();
    public static final EventBuses.Many<  ChatEvents,    ChatEvent> CHAT   = new EventBuses.Many<>();

    public static final EventBuses.Single<Render2DEvent> RENDER2D = new EventBuses.Single<>();
    public static final EventBuses.Single<Render3DEvent> RENDER3D = new EventBuses.Single<>();
    public static final EventBuses.Single<ScheduleInventoryActionEvent> INVENTORY_SCHEDULE = new EventBuses.Single<>();
}
