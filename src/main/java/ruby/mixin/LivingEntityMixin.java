package ruby.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.combat.Criticals;
import ruby.systems.modules.movement.AirJump;
import ruby.systems.modules.movement.NoJumpDelay;
import ruby.systems.modules.movement.NoPush;
import ruby.systems.modules.player.AutoTool;
import ruby.systems.modules.world.Scaffold;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "jump", at = @At("TAIL"))
    private void ruby$critJumpMotion(CallbackInfo ci) {
        if(!((Object) this instanceof ClientPlayerEntity)) return;
        Criticals criticals = Modules.getByClass(Criticals.class);
        if(criticals != null) criticals.onPlayerJump(0.42f);
    }

    @Inject(method = "getMainHandStack", at = @At("RETURN"), cancellable = true)
    private void ruby$spoofMainHandStack(CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        if (!AutoTool.shouldSpoofVisualSlot()) return;

        cir.setReturnValue(player.getInventory().getStack(AutoTool.visualSlot));
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void ruby$noJumpDelay(CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;

        NoJumpDelay noJumpDelay = Modules.getByClass(NoJumpDelay.class);
        if (noJumpDelay == null || !noJumpDelay.enabled()) return;

        AirJump airJump = Modules.getByClass(AirJump.class);
        if (airJump != null && airJump.enabled()) return;

        Scaffold scaffold = Modules.getByClass(Scaffold.class);
        if (scaffold != null && scaffold.enabled()) return;

        ((LivingEntityAccessor) this).ruby$setJumpingCooldown(0);
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void ruby$noEntityPush(Entity entity, CallbackInfo ci) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        if (!NoPush.canPush(NoPush.PushBy.Entities)) {
            ci.cancel();
        }
    }
}
