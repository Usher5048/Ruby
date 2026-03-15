package ruby.systems.events.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import ruby.systems.events.Event;

public final class AttackEntityEvent extends Event {
    private static final AttackEntityEvent INSTANCE = new AttackEntityEvent();

    public PlayerEntity player;
    public Entity target;

    private AttackEntityEvent() {
    }

    public static AttackEntityEvent get(PlayerEntity player, Entity target) {
        INSTANCE.setCancelled(false);
        INSTANCE.player = player;
        INSTANCE.target = target;
        return INSTANCE;
    }
}
