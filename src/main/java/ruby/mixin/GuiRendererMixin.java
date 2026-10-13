package ruby.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.gui.PerspectiveBoard;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @ModifyArg(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;II)V"
            ),
            index = 1
    )
    private RenderTarget ruby$perspectiveTarget(RenderTarget original) {
        RenderTarget board = PerspectiveBoard.renderTarget();
        if (PerspectiveBoard.suppressBlur() && board != null && (Object) this == PerspectiveBoard.guiRenderer()) {
            return board;
        }
        return original;
    }

    @ModifyArg(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V"
            ),
            index = 2
    )
    private float ruby$perspectiveOrthoWidth(float width) {
        if ((Object) this != PerspectiveBoard.guiRenderer()) return width;
        return width * PerspectiveBoard.supersample();
    }

    @ModifyArg(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V"
            ),
            index = 3
    )
    private float ruby$perspectiveOrthoHeight(float height) {
        if ((Object) this != PerspectiveBoard.guiRenderer()) return height;
        return height * PerspectiveBoard.supersample();
    }

    @Inject(
            method = "enableScissor(Lnet/minecraft/client/gui/navigation/ScreenRectangle;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ruby$perspectiveScissor(ScreenRectangle rect, RenderPass pass, CallbackInfo ci) {
        int sample = PerspectiveBoard.supersample();
        if (sample <= 1 || (Object) this != PerspectiveBoard.guiRenderer()) return;
        var window = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
        int scale = Math.max(1, window.guiScale);
        int targetW = window.width * sample;
        int targetH = window.height * sample;
        int left = rect.left() * scale;
        int top = rect.top() * scale;
        int right = Math.min(rect.right() * scale, targetW);
        int bottom = Math.min(rect.bottom() * scale, targetH);
        pass.enableScissor(left, Math.max(0, targetH - bottom), Math.max(0, right - left), Math.max(0, bottom - top));
        ci.cancel();
    }
}
