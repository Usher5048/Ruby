package ruby.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.player.AutoTool;
import ruby.systems.modules.player.AutoToolServerSlot;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Inject(method = "getSelectedSlot", at = @At("RETURN"), cancellable = true)
    private void ruby$spoofSelectedSlot(CallbackInfoReturnable<Integer> cir) {
        if (AutoTool.shouldSpoofVisualSlot()) {
            cir.setReturnValue(AutoTool.visualSlot);
        }
    }

    @Inject(method = "getSelectedStack", at = @At("RETURN"), cancellable = true)
    private void ruby$spoofSelectedStack(CallbackInfoReturnable<ItemStack> cir) {
        if (AutoTool.shouldSpoofVisualSlot()) {
            cir.setReturnValue(((PlayerInventory) (Object) this).getStack(AutoTool.visualSlot));
        }
    }

    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void ruby$onSetSelectedSlot(int slot, CallbackInfo ci) {
        if (AutoToolServerSlot.isApplyingMiningSlot()) return;
        if (!AutoTool.shouldUseMiningSlot()) return;
        if (slot == AutoTool.miningSlot) return;

        AutoTool.visualSlot = slot;
        ci.cancel();
    }
}
