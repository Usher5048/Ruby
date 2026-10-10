package ruby.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

@Mixin(BlockBehaviour.class)
public abstract class AbstractBlockMixin {
    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void ruby$xrayFullBright(
            BlockState state,
            BlockGetter world,
            BlockPos pos,
            CallbackInfoReturnable<Float> cir
    ) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray != null && xray.enabled()) {
            cir.setReturnValue(1.0f);
        }
    }
}
