package ruby.mixin;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.player.AutoTool;

@Mixin(FirstPersonHandsAndItems.class)
public class HeldItemRendererMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void ruby$beforeUpdateHeldItems(LocalPlayer player, CallbackInfo ci) {
        AutoTool.AutoToolVisualContext.enter();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void ruby$afterUpdateHeldItems(LocalPlayer player, CallbackInfo ci) {
        AutoTool.AutoToolVisualContext.exit();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void ruby$beforeRenderFirstPersonItem(
            LocalPlayer player,
            float partialTick,
            FirstPersonHandsAndItemsRenderState state,
            CallbackInfo ci
    ) {
        AutoTool.AutoToolVisualContext.enter();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void ruby$afterRenderFirstPersonItem(
            LocalPlayer player,
            float partialTick,
            FirstPersonHandsAndItemsRenderState state,
            CallbackInfo ci
    ) {
        AutoTool.AutoToolVisualContext.exit();
    }
}
