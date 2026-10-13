package ruby.mixin;

import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrackedWaypoint.class)
public interface TrackedWaypointAccessor {
    @Accessor("type")
    TrackedWaypoint.Type ruby$getType();
}
