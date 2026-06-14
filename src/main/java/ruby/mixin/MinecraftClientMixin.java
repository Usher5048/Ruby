package ruby.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.helpers.RotationManager;
import ruby.systems.events.Events;
import ruby.systems.events.TickEvents;
import ruby.systems.events.client.UseCooldownEvent;
import ruby.systems.modules.Modules;
import ruby.systems.modules.combat.KillAura;
import ruby.systems.modules.render.Freecam;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    private void tickBegin(CallbackInfo info) {
        if(Events.TICK.fire(TickEvents.BEGIN, Events.GenericEvent.get()))
            info.cancel();
    }

    @Inject(at = @At("TAIL"), method = "tick", cancellable = true)
    private void tickEnd(CallbackInfo info) {
        if(Events.TICK.fire(TickEvents.END, Events.GenericEvent.get()))
            info.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void ruby$noBreakingWhileAiming(boolean breaking, CallbackInfo ci) {
        KillAura killAura = Modules.getByClass(KillAura.class);
        if(breaking && killAura != null && killAura.enabled() && RotationManager.hasRotation())
            ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void ruby$freecamScreen(Screen screen, CallbackInfo ci) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled()) freecam.onScreenOpen();
    }

    @Inject(method = "doItemUse", at = @At("TAIL"))
    private void ruby$useCooldownEvent(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        UseCooldownEvent event = new UseCooldownEvent(((MinecraftClientAccessor) client).ruby$getItemUseCooldown());
        Events.USE_COOLDOWN.fire(event);
        ((MinecraftClientAccessor) client).ruby$setItemUseCooldown(event.cooldown());
    }
}
