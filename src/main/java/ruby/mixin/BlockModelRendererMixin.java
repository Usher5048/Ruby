package ruby.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {
    @ModifyReturnValue(method = "shouldDrawFace", at = @At("RETURN"))
    private static boolean ruby$modifyDrawSide(
            boolean original,
            BlockRenderView world,
            BlockState state,
            boolean solid,
            Direction direction,
            BlockPos pos
    ) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return original;
        return xray.modifyDrawSide(state, world, pos, direction, original);
    }
}
