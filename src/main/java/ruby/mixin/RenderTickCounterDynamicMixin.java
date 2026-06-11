package ruby.mixin;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.world.Timer;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class RenderTickCounterDynamicMixin {
    @Inject(method = "beginRenderTick(JZ)I", at = @At("RETURN"))
    private void ruby$timer(long timeMillis, boolean tick, CallbackInfoReturnable<Integer> cir) {
        double multiplier = Timer.getMultiplier();
        if(multiplier == 1.0) return;

        int ticks = cir.getReturnValue();
        if(multiplier > 1.0) {
            cir.setReturnValue(Math.max(ticks, (int) Math.ceil(ticks * multiplier)));
        } else {
            cir.setReturnValue((int) Math.floor(ticks * multiplier));
        }
    }
}
