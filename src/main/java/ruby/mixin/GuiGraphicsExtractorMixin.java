package ruby.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ruby.systems.gui.ClickGUI;
import ruby.systems.gui.PerspectiveBoard;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
    @ModifyVariable(method = "innerFill", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int ruby$fadeFill(int color) {
        return ClickGUI.applyOpacity(color);
    }

    @ModifyVariable(
            method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 4
    )
    private int ruby$fadeBlit(int color) {
        return ClickGUI.applyOpacity(color);
    }

    @ModifyArg(
            method = "<init>(Lnet/minecraft/client/Minecraft;Lorg/joml/Matrix3x2fStack;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;<init>(IIII)V"
            ),
            index = 2
    )
    private static int ruby$perspectiveScissorWidth(int width) {
        if (!PerspectiveBoard.captureScissor()) return width;
        return width * PerspectiveBoard.supersample();
    }

    @ModifyArg(
            method = "<init>(Lnet/minecraft/client/Minecraft;Lorg/joml/Matrix3x2fStack;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/navigation/ScreenRectangle;<init>(IIII)V"
            ),
            index = 3
    )
    private static int ruby$perspectiveScissorHeight(int height) {
        if (!PerspectiveBoard.captureScissor()) return height;
        return height * PerspectiveBoard.supersample();
    }
}
