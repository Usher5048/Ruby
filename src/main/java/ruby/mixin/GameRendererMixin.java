package ruby.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.joml.Matrix4f;
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
    @Shadow private void bobHurt(CameraRenderState cameraState, PoseStack matrices) {}
    @Shadow private void bobView(CameraRenderState cameraState, PoseStack matrices) {}

    @Unique private final PoseStack matrices = new PoseStack();

    @Inject(
            method = "render3dHud",
            at = @At(
                    value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    args = {"ldc=hand"}
            )
    )
    private void callRenderEvent(
            CameraRenderState cameraState,
            PlayerRenderState playerState,
            OptionsRenderState optionsState,
            boolean renderHand,
            CallbackInfo info
    ) {
        // Walk bob and hurt tilt are multiplied into the projection, not the view.
        Matrix4f view = new Matrix4f(cameraState.viewRotationMatrix);
        Matrix4f projection = new Matrix4f(cameraState.projectionMatrix);

        this.matrices.pushPose();
        this.bobHurt(cameraState, this.matrices);
        if (optionsState.bobView) this.bobView(cameraState, this.matrices);
        projection.mul(this.matrices.last().pose());
        this.matrices.popPose();

        RenderSystem.getModelViewStack().pushMatrix().mul(view);
        Renderer.updateMatrices(projection, view);

        Renderer.begin();
        Events.RENDER3D.fire(Events.GenericEvent.get());
        Renderer.end(this.matrices);
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void ruby$freecamHands(
            CameraRenderState cameraState,
            PlayerRenderState playerState,
            GpuTextureView lightmap,
            CallbackInfo ci
    ) {
        Freecam freecam = Modules.getByClass(Freecam.class);
        if (freecam != null && freecam.enabled() && !freecam.renderHands()) {
            ci.cancel();
        }
    }
}
