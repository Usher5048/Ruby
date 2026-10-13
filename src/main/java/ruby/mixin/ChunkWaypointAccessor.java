package ruby.mixin;

import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.waypoints.TrackedWaypoint$ChunkWaypoint")
public interface ChunkWaypointAccessor {
    @Accessor("chunkPos")
    ChunkPos getChunkPos();
}
