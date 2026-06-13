package ruby.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.FreeLook;
import ruby.systems.modules.render.Freecam;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void ruby$overrideCamera(CallbackInfo ci, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled()) {
            this.setPos(freecam.getX(tickDelta), freecam.getY(tickDelta), freecam.getZ(tickDelta));
            this.setRotation(freecam.getYaw(tickDelta), freecam.getPitch(tickDelta));
            return;
        }

        FreeLook freeLook = Modules.getByClass(FreeLook.class);
        if (freeLook != null && freeLook.enabled()) {
            this.setRotation(freeLook.cameraYaw, freeLook.cameraPitch);
        }
    }
}
