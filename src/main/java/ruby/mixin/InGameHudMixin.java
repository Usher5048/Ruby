package ruby.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.player.AutoTool;

@Mixin(Hud.class)
public class InGameHudMixin {
    @Shadow private int toolHighlightTimer;

    @Inject(method = "extractItemHotbar", at = @At("HEAD"))
    private void ruby$beforeRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        AutoTool.AutoToolVisualContext.enter();
    }

    @Redirect(
            method = "extractItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 1
            )
    )
    private void ruby$renderHotbarSelection(GuiGraphicsExtractor context, RenderPipeline pipeline, Identifier texture,
            int x, int y, int width, int height) {
        AutoTool.renderHotbarSelection(context, pipeline, texture, x, y, width, height);
    }

    @Inject(method = "extractItemHotbar", at = @At("RETURN"))
    private void ruby$afterRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        AutoTool.AutoToolVisualContext.exit();
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void ruby$suppressHeldItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (AutoTool.shouldSuppressVanillaHeldItemTooltip()) ci.cancel();
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void ruby$clearHeldItemTooltipFade(CallbackInfo ci) {
        if (AutoTool.shouldSuppressVanillaHeldItemTooltip()) {
            this.toolHighlightTimer = 0;
        }
    }
}
