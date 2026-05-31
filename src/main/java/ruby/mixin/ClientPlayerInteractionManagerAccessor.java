package ruby.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("syncSelectedSlot")
    void ruby$syncSelectedSlot();

    @Accessor("lastSelectedSlot")
    int ruby$getLastSelectedSlot();

    @Accessor("lastSelectedSlot")
    void ruby$setLastSelectedSlot(int slot);

    @Accessor("currentBreakingProgress")
    float ruby$getBreakingProgress();

    @Accessor("currentBreakingProgress")
    void ruby$setBreakingProgress(float newProgress);

    @Accessor("currentBreakingPos")
    BlockPos ruby$getBreakingPos();
}
