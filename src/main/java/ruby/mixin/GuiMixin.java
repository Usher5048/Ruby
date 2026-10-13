package ruby.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ruby.systems.gui.PerspectiveBoard;

@Mixin(Gui.class)
public class GuiMixin {
    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
            ),
            index = 0
    )
    private GuiGraphicsExtractor ruby$perspectiveExtractor(GuiGraphicsExtractor graphics) {
        return PerspectiveBoard.extractor(graphics);
    }

    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
            ),
            index = 1
    )
    private int ruby$perspectiveMouseX(int mouseX) {
        return PerspectiveBoard.mouseX(mouseX);
    }

    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
            ),
            index = 2
    )
    private int ruby$perspectiveMouseY(int mouseY) {
        return PerspectiveBoard.mouseY(mouseY);
    }
}
