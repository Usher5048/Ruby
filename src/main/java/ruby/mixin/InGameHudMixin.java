package ruby.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.player.AutoToolVisualSlotSpoof;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    private int ruby$savedSlot = -1;

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void ruby$beforeRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ruby$savedSlot = AutoToolVisualSlotSpoof.beginVisualSwap();
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void ruby$afterRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AutoToolVisualSlotSpoof.endVisualSwap(ruby$savedSlot);
        ruby$savedSlot = -1;
    }
}
