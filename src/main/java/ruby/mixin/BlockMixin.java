package ruby.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void ruby$forceXrayFace(BlockState state, BlockState neighborState, Direction direction,
                                            CallbackInfoReturnable<Boolean> cir) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return;
        if (!xray.isBlocked(state.getBlock(), null)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private static boolean ruby$modifyDrawSide(boolean original, BlockState state, BlockState neighborState,
                                                Direction direction) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return original;
        if (original || xray.isBlocked(state.getBlock(), null)) return original;

        return !neighborState.isOpaqueFullCube()
                || neighborState.getBlock() != state.getBlock()
                || xray.isBlocked(neighborState.getBlock(), null);
    }
}
