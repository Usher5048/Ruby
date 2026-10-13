package ruby.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.waypoints.TrackedWaypoint$AzimuthWaypoint")
public interface AzimuthWaypointAccessor {
    @Accessor("angle")
    float getAngle();
}
