package ruby.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.events.Events;
import ruby.systems.events.tick.TickEvents;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    private void tickBegin(CallbackInfo info) {
        if(Events.TICK.fire(TickEvents.BEGIN, new Events.GenericEvent()))
            info.cancel();
    }

    @Inject(at = @At("TAIL"), method = "tick", cancellable = true)
    private void tickEnd(CallbackInfo info) {
        if(Events.TICK.fire(TickEvents.END, new Events.GenericEvent()))
            info.cancel();
    }
}
