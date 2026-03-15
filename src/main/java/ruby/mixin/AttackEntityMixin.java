package ruby.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.events.Events;
import ruby.systems.events.client.AttackEntityEvent;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AttackEntityMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void ruby$onBeforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (Events.ATTACK_ENTITY.fireEvent(AttackEntityEvent.get(player, target))) {
            ci.cancel();
        }
    }
}
