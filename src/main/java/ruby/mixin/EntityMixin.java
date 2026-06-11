package ruby.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.combat.Hitboxes;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("HEAD"), cancellable = true)
    private void ruby$onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        Hitboxes hitboxes = Modules.getByClass(Hitboxes.class);
        if(hitboxes == null || !hitboxes.enabled()) return;

        double value = hitboxes.expand.value();
        if(value != 0) cir.setReturnValue((float) value);
    }
}
