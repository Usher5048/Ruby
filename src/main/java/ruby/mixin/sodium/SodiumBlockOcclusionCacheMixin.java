package ruby.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

/**
 * Sodium 0.8.7+ — occlusion lives on {@link AbstractBlockRenderContext#shouldDrawSide}.
 */
@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class SodiumBlockOcclusionCacheMixin {
    @Shadow protected BlockState state;
    @Shadow protected BlockPos pos;
    @Shadow protected LevelSlice slice;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false)
    private void ruby$forceOreFaces(Direction facing, CallbackInfoReturnable<Boolean> cir) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray != null && xray.enabled() && !xray.isBlocked(this.state.getBlock(), null, this.slice)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"), remap = false)
    private boolean ruby$modifyDrawSide(boolean original, Direction facing) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return original;
        return xray.modifyDrawSide(this.state, this.slice, this.pos, facing, original);
    }
}
