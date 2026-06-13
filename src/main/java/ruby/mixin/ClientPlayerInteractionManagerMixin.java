package ruby.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void ruby$onBeforeAttack(PlayerEntity player, Entity target, CallbackInfo info) {
        if(Events.ENTITY.fire(EntityEvents.BEFORE_ATTACK, new EntityEvent(target)))
            info.cancel();
    }

    @Inject(method = "attackEntity", at = @At("TAIL"), cancellable = true)
    private void ruby$onAfterAttack(PlayerEntity player, Entity target, CallbackInfo info) {
        if(Events.ENTITY.fire(EntityEvents.AFTER_ATTACK, new EntityEvent(target)))
            info.cancel();
    }
}
