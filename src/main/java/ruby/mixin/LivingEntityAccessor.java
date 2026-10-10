package ruby.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("noJumpDelay")
    int ruby$getJumpingCooldown();

    @Accessor("noJumpDelay")
    void ruby$setJumpingCooldown(int cooldown);
}
