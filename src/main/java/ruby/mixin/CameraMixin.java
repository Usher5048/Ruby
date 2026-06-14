package ruby.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.FreeLook;
import ruby.systems.modules.render.Freecam;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow private float yaw;
    @Shadow private float pitch;

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void ruby$freecamThirdPerson(
            World area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickProgress,
            CallbackInfo ci
    ) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled()) {
            this.thirdPerson = true;
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void ruby$freecamPos(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled()) {
            args.set(0, freecam.getX(tickDelta));
            args.set(1, freecam.getY(tickDelta));
            args.set(2, freecam.getZ(tickDelta));
        }
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void ruby$freecamRotation(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled()) {
            args.set(0, freecam.getYaw(tickDelta));
            args.set(1, freecam.getPitch(tickDelta));
            return;
        }

        FreeLook freeLook = Modules.getByClass(FreeLook.class);
        if (freeLook != null && freeLook.enabled()) {
            args.set(0, freeLook.cameraYaw);
            args.set(1, freeLook.cameraPitch);
        }
    }
}
