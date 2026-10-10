package ruby.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.helpers.RotationManager;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.modules.combat.Criticals;
import ruby.systems.modules.combat.KillAura;
import ruby.systems.modules.movement.NoPush;
import ruby.systems.modules.render.Freecam;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void ruby$updateRotations(CallbackInfo ci) {
        Bypasses.get().setMovementTick(false);
        KillAura.updateRotations();
        RotationManager.update();
        KillAura.tryAttack();
    }

    @Redirect(
            method = {"sendPosition", "sendChanges", "applyInput"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F")
    )
    private float ruby$silentYaw(LocalPlayer instance) {
        if(RotationManager.hasRotation() && (Object) this == instance)
            return RotationManager.rotationYaw();
        return instance.getYRot();
    }

    @Redirect(
            method = {"sendPosition", "sendChanges", "applyInput"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F")
    )
    private float ruby$silentPitch(LocalPlayer instance) {
        if(RotationManager.hasRotation() && (Object) this == instance)
            return RotationManager.rotationPitch();
        return instance.getXRot();
    }

    @Inject(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER)
    )
    private void ruby$syncSprintFromInput(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if(Criticals.blocksSprintInput()) {
            player.setSprinting(false);
            return;
        }
        if(!RotationManager.hasRotation()) return;
        player.setSprinting(RotationManager.shouldSprint(player.input.keyPresses));
    }

    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z")
    )
    private boolean ruby$canStartSprinting(LocalPlayer instance) {
        if(Criticals.blocksSprintInput()) return false;
        if(!RotationManager.hasRotation() || (Object) this != instance)
            return ((ClientPlayerEntityAccessor) instance).ruby$canStartSprinting();
        return ((ClientPlayerEntityAccessor) instance).ruby$canStartSprinting()
                && RotationManager.shouldSprint(instance.input.keyPresses);
    }

    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldStopRunSprinting()Z")
    )
    private boolean ruby$stopRunSprinting(LocalPlayer instance) {
        return ruby$stopSprint(instance, ((ClientPlayerEntityAccessor) instance).ruby$shouldStopRunSprinting());
    }

    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldStopSwimSprinting()Z")
    )
    private boolean ruby$stopSwimSprinting(LocalPlayer instance) {
        return ruby$stopSprint(instance, ((ClientPlayerEntityAccessor) instance).ruby$shouldStopSwimSprinting());
    }

    private boolean ruby$stopSprint(LocalPlayer instance, boolean originalStop) {
        if(Criticals.blocksSprintInput()) return true;
        if(originalStop) return true;
        if(!RotationManager.hasRotation() || (Object) this != instance) return false;
        return !RotationManager.shouldSprint(instance.input.keyPresses);
    }

    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z")
    )
    private boolean ruby$inputSprint(Input input) {
        if(Criticals.blocksSprintInput()) return false;
        if(!RotationManager.hasRotation()) return input.sprint();
        return RotationManager.shouldSprint(input);
    }

    @Redirect(
            method = "sendIsSprintingIfNeeded",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z")
    )
    private boolean ruby$networkSprint(LocalPlayer instance) {
        if(Criticals.blocksNetworkSprint()) return false;
        if(!RotationManager.hasRotation() || (Object) this != instance)
            return instance.isSprinting();
        if(!RotationManager.shouldSprint(instance.input.keyPresses))
            return false;
        return instance.isSprinting();
    }

    @Inject(method = {"shouldStopRunSprinting", "shouldStopSwimSprinting"}, at = @At("RETURN"), cancellable = true)
    private void ruby$forceStopSprint(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) return;
        if(Criticals.blocksSprintInput()) {
            cir.setReturnValue(true);
            return;
        }
        if(!RotationManager.hasRotation()) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        if(!RotationManager.shouldSprint(player.input.keyPresses))
            cir.setReturnValue(true);
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void ruby$noBlockPush(double x, double z, CallbackInfo ci) {
        if (!NoPush.canPush(NoPush.PushBy.Blocks)) {
            ci.cancel();
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void ruby$freecamSneak(CallbackInfo ci) {
        Freecam freecam = ruby.systems.modules.Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.staySneaking()) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            player.setShiftKeyDown(true);
        }
    }

    @Inject(
            method = "sendPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void updateLastSent(CallbackInfo info) {
        Bypasses.get().setMovementTick(true);
    }
}
