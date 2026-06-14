package ruby.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium throws {@link IllegalStateException} when {@code splitPlaneEdgeDot == 0} during
 * translucent BSP sorting. XRay semi-transparent blocks hit this constantly — skip instead.
 */
@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.InnerPartitionBSPNode",
        remap = false
)
public class SodiumInnerPartitionBSPNodeMixin {
    @Inject(
            method = "interpolateAttributes(FLorg/joml/Vector3fc;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;)V",
            at = @At(value = "NEW", target = "java/lang/IllegalStateException"),
            cancellable = true,
            remap = false
    )
    private static void ruby$skipDegenerateSplit(
            float splitDistance,
            Vector3fc splitPlane,
            ChunkVertexEncoder.Vertex inside,
            ChunkVertexEncoder.Vertex outside,
            ChunkVertexEncoder.Vertex targetA,
            ChunkVertexEncoder.Vertex targetB,
            ChunkVertexEncoder.Vertex targetC,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
