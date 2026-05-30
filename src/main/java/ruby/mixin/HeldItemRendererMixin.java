package ruby.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.player.AutoToolVisualContext;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void ruby$beforeUpdateHeldItems(CallbackInfo ci) {
        AutoToolVisualContext.enter();
    }

    @Inject(method = "updateHeldItems", at = @At("RETURN"))
    private void ruby$afterUpdateHeldItems(CallbackInfo ci) {
        AutoToolVisualContext.exit();
    }

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD")
    )
    private void ruby$beforeRenderFirstPersonItem(float tickProgress, MatrixStack matrices,
                                                  OrderedRenderCommandQueue queue, ClientPlayerEntity player,
                                                  int light, CallbackInfo ci) {
        AutoToolVisualContext.enter();
    }

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("RETURN")
    )
    private void ruby$afterRenderFirstPersonItem(float tickProgress, MatrixStack matrices,
                                                 OrderedRenderCommandQueue queue, ClientPlayerEntity player,
                                                 int light, CallbackInfo ci) {
        AutoToolVisualContext.exit();
    }
}
