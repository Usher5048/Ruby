package ruby.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.helpers.RotationManager;
import ruby.systems.modules.Modules;
import ruby.systems.modules.combat.Hitboxes;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void ruby$expandHitbox(CallbackInfoReturnable<Float> cir) {
        Hitboxes hitboxes = Modules.getByClass(Hitboxes.class);
        if(hitboxes == null) return;

        double value = hitboxes.getEntityValue((Entity) (Object) this);
        if(value != 0) cir.setReturnValue((float) value);
    }

    @ModifyArg(
            method = "updateVelocity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"
            ),
            index = 2
    )
    private float ruby$movementYaw(float yaw) {
        if((Object) this instanceof ClientPlayerEntity && RotationManager.hasRotation())
            return RotationManager.rotationYaw();
        return yaw;
    }
}
