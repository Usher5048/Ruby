package ruby.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.player.AutoToolVisualSlotSpoof;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    private int ruby$savedSlot = -1;

    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void ruby$beforeUpdateHeldItems(CallbackInfo ci) {
        ruby$savedSlot = AutoToolVisualSlotSpoof.beginVisualSwap();
    }

    @Inject(method = "updateHeldItems", at = @At("RETURN"))
    private void ruby$afterUpdateHeldItems(CallbackInfo ci) {
        AutoToolVisualSlotSpoof.endVisualSwap(ruby$savedSlot);
        ruby$savedSlot = -1;
    }
}
