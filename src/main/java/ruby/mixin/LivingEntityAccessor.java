package ruby.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumpingCooldown")
    int ruby$getJumpingCooldown();

    @Accessor("jumpingCooldown")
    void ruby$setJumpingCooldown(int cooldown);
}
