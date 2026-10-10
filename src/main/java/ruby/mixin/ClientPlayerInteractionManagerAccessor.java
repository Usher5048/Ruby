package ruby.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("ensureHasSentCarriedItem")
    void ruby$syncSelectedSlot();

    @Accessor("carriedIndex")
    int ruby$getLastSelectedSlot();

    @Accessor("carriedIndex")
    void ruby$setLastSelectedSlot(int slot);

    @Accessor("destroyProgress")
    float ruby$getBreakingProgress();

    @Accessor("destroyProgress")
    void ruby$setBreakingProgress(float newProgress);

    @Accessor("destroyBlockPos")
    BlockPos ruby$getBreakingPos();

    @Accessor("destroyDelay")
    int ruby$getBlockBreakingCooldown();

    @Accessor("destroyDelay")
    void ruby$setBlockBreakingCooldown(int cooldown);
}
