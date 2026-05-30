package ruby.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.player.AutoTool;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getMainHandStack", at = @At("RETURN"), cancellable = true)
    private void ruby$spoofMainHandStack(CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof ClientPlayerEntity player)) return;
        if (!AutoTool.shouldSpoofVisualSlot()) return;

        cir.setReturnValue(player.getInventory().getStack(AutoTool.visualSlot));
    }
}
