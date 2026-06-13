package ruby.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.helpers.render.Renderer;
import ruby.systems.events.Events;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Freecam;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Camera camera;
    @Shadow @Final private MinecraftClient client;

    @Shadow protected abstract void bobView(MatrixStack matrices, float tickDelta);
    @Shadow protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Unique private final MatrixStack matrices = new MatrixStack();

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
                    args = {"ldc=hand"}
            )
    )
    private void callRenderEvent(
            RenderTickCounter tickCounter, CallbackInfo info,
            @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f position,
            @Local MatrixStack matrixStack
    ) {
        RenderSystem.getModelViewStack().pushMatrix().mul(position);

        this.matrices.push();
        this.tiltViewWhenHurt(this.matrices, this.camera.getLastTickProgress());
        if(this.client.options.getBobView().getValue())
            this.bobView(this.matrices, this.camera.getLastTickProgress());

        Matrix4f inverseBob = new Matrix4f(this.matrices.peek().getPositionMatrix()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        this.matrices.pop();

        Renderer.updateMatrices(projection, position);

        Renderer.begin();
        Events.RENDER3D.fire(Events.GenericEvent.get());
        Renderer.end(matrixStack);
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void ruby$freecamHands(float tickDelta, boolean sleeping, org.joml.Matrix4f matrix, CallbackInfo ci) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled() && !freecam.renderHands()) {
            ci.cancel();
        }
    }
}
