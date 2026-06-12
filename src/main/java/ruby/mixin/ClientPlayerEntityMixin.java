package ruby.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.helpers.RotationManager;
import ruby.systems.modules.combat.Criticals;
import ruby.systems.modules.combat.KillAura;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void ruby$updateRotations(CallbackInfo ci) {
        KillAura.updateRotations();
        RotationManager.update();
        KillAura.tryAttack();
    }

    @Redirect(
            method = {"sendMovementPackets", "tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F")
    )
    private float ruby$silentYaw(ClientPlayerEntity instance) {
        if(RotationManager.hasRotation() && (Object) this == instance)
            return RotationManager.rotationYaw();
        return instance.getYaw();
    }

    @Redirect(
            method = {"sendMovementPackets", "tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F")
    )
    private float ruby$silentPitch(ClientPlayerEntity instance) {
        if(RotationManager.hasRotation() && (Object) this == instance)
            return RotationManager.rotationPitch();
        return instance.getPitch();
    }

    @Inject(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick()V", shift = At.Shift.AFTER)
    )
    private void ruby$syncSprintFromInput(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if(Criticals.blocksSprintInput()) {
            player.setSprinting(false);
            return;
        }
        if(!RotationManager.hasRotation()) return;
        player.setSprinting(RotationManager.shouldSprint(player.input.playerInput));
    }

    @Redirect(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canStartSprinting()Z")
    )
    private boolean ruby$canStartSprinting(ClientPlayerEntity instance) {
        if(Criticals.blocksSprintInput()) return false;
        if(!RotationManager.hasRotation() || (Object) this != instance)
            return ((ClientPlayerEntityAccessor) instance).ruby$canStartSprinting();
        return ((ClientPlayerEntityAccessor) instance).ruby$canStartSprinting()
                && RotationManager.shouldSprint(instance.input.playerInput);
    }

    @Redirect(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldStopSprinting()Z")
    )
    private boolean ruby$tickMovementStopSprint(ClientPlayerEntity instance) {
        if(Criticals.blocksSprintInput()) return true;
        if(((ClientPlayerEntityAccessor) instance).ruby$shouldStopSprinting()) return true;
        if(!RotationManager.hasRotation() || (Object) this != instance) return false;
        return !RotationManager.shouldSprint(instance.input.playerInput);
    }

    @Redirect(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PlayerInput;sprint()Z")
    )
    private boolean ruby$inputSprint(PlayerInput input) {
        if(Criticals.blocksSprintInput()) return false;
        if(!RotationManager.hasRotation()) return input.sprint();
        return RotationManager.shouldSprint(input);
    }

    @Redirect(
            method = "sendSprintingPacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z")
    )
    private boolean ruby$networkSprint(ClientPlayerEntity instance) {
        if(Criticals.blocksNetworkSprint()) return false;
        if(!RotationManager.hasRotation() || (Object) this != instance)
            return instance.isSprinting();
        if(!RotationManager.shouldSprint(instance.input.playerInput))
            return false;
        return instance.isSprinting();
    }

    @Inject(method = "shouldStopSprinting", at = @At("RETURN"), cancellable = true)
    private void ruby$forceStopSprint(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) return;
        if(Criticals.blocksSprintInput()) {
            cir.setReturnValue(true);
            return;
        }
        if(!RotationManager.hasRotation()) return;

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if(!RotationManager.shouldSprint(player.input.playerInput))
            cir.setReturnValue(true);
    }
}
