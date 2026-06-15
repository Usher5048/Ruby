package ruby.systems.events.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import ruby.helpers.PlayerInteractEntity;
import ruby.systems.events.Event;

public final class EntityEvent extends Event {
    private final PlayerInteractEntity.Type type;
    private EntityHitResult hitResult;
    private Entity entity;
    private Hand hand;

    public EntityEvent(
            PlayerInteractEntity.Type type, Entity entity,
            EntityHitResult hitResult, Hand hand
    ) {
        this.type = type;
        this.entity = entity;
        this.hitResult = hitResult;
        this.hand = hand;
    }

    public PlayerInteractEntity.Type type() {
        return this.type;
    }
    public Entity entity() {
        return this.entity;
    }
    public EntityHitResult hitResult() {
        return this.hitResult;
    }
    public Hand hand() {
        return this.hand;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
    public void setHitResult(EntityHitResult hitResult) {
        this.hitResult = hitResult;
    }
    public void setHand(Hand hand) {
        this.hand = hand;
    }
}
