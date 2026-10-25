package ruby.systems.events.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import ruby.systems.events.Event;

public final class EntityEvent extends Event {
    private final boolean isSneaking;
    private EntityHitResult hitResult;
    private Entity entity;
    private InteractionHand hand;

    public EntityEvent(
            boolean isSneaking, Entity entity,
            EntityHitResult hitResult, InteractionHand hand
    ) {
        this.isSneaking = isSneaking;
        this.entity = entity;
        this.hitResult = hitResult;
        this.hand = hand;
    }

    public boolean isSneaking() {
        return this.isSneaking;
    }
    public Entity entity() {
        return this.entity;
    }
    public EntityHitResult hitResult() {
        return this.hitResult;
    }
    public InteractionHand hand() {
        return this.hand;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
    public void setHitResult(EntityHitResult hitResult) {
        this.hitResult = hitResult;
    }
    public void setHand(InteractionHand hand) {
        this.hand = hand;
    }
}
