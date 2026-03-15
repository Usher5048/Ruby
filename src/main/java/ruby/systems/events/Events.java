package ruby.systems.events;

import ruby.systems.events.client.AttackEntityEvent;
import ruby.systems.events.client.ClientPlayerPreTickEvent;
import ruby.systems.events.client.SendMovementPacketsEvent;

public final class Events {
    public static final EventBuses.Single<ClientPlayerPreTickEvent> CLIENT_PLAYER_PRE_TICK = new EventBuses.Single<>();
    public static final EventBuses.Single<SendMovementPacketsEvent.Pre> SEND_MOVEMENT_PACKETS_PRE = new EventBuses.Single<>();
    public static final EventBuses.Single<AttackEntityEvent> ATTACK_ENTITY = new EventBuses.Single<>();

    private Events() {
    }
}
