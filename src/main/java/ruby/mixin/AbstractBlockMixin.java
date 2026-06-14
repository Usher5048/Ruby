package ruby.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {
    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void ruby$xrayFullBright(
            BlockState state,
            BlockView world,
            BlockPos pos,
            CallbackInfoReturnable<Float> cir
    ) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray != null && xray.enabled()) {
            cir.setReturnValue(1.0f);
        }
    }
}
