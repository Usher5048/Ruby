package ruby.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.helpers.RotationManager;
import ruby.systems.modules.Modules;
import ruby.systems.modules.combat.Hitboxes;
import ruby.systems.modules.movement.NoPush;
import ruby.systems.modules.render.FreeLook;
import ruby.systems.modules.render.Freecam;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void ruby$freecamLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer)) return;

        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled()) {
            freecam.changeLookDirection(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
            return;
        }

        FreeLook freeLook = Modules.getByClass(FreeLook.class);
        if (freeLook != null && freeLook.enabled() && freeLook.cameraMode()) {
            freeLook.changeLookDirection(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
        }
    }

    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    private void ruby$expandHitbox(CallbackInfoReturnable<Float> cir) {
        Hitboxes hitboxes = Modules.getByClass(Hitboxes.class);
        if(hitboxes == null) return;

        double value = hitboxes.getEntityValue((Entity) (Object) this);
        if(value != 0) cir.setReturnValue((float) value);
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void ruby$noFluidPush(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LocalPlayer)) return;
        if (!NoPush.canPush(NoPush.PushBy.Liquids)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(
            method = "moveRelative",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"
            ),
            index = 2
    )
    private float ruby$movementYaw(float yaw) {
        if((Object) this instanceof LocalPlayer && RotationManager.hasRotation())
            return RotationManager.rotationYaw();
        return yaw;
    }
}
