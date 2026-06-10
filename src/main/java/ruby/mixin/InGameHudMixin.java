package ruby.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.player.AutoTool;
import ruby.systems.modules.player.AutoToolVisualContext;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow private int heldItemTooltipFade;

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void ruby$beforeRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AutoToolVisualContext.enter();
    }

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 1))
    private void ruby$renderHotbarSelection(DrawContext context, RenderPipeline pipeline, Identifier texture,
            int x, int y, int width, int height) {
        AutoTool.renderHotbarSelection(context, pipeline, texture, x, y, width, height);
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void ruby$afterRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AutoToolVisualContext.exit();
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void ruby$suppressHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        if (AutoTool.shouldSuppressVanillaHeldItemTooltip()) ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ruby$clearHeldItemTooltipFade(CallbackInfo ci) {
        if (AutoTool.shouldSuppressVanillaHeldItemTooltip()) {
            this.heldItemTooltipFade = 0;
        }
    }
}
