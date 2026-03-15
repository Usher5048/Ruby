package ruby.systems.events.entity;

import net.minecraft.entity.Entity;
import ruby.systems.events.Event;

public final class EntityEvent extends Event {
    private Entity entity;

    public EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity entity() {
        return this.entity;
    }
    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}
