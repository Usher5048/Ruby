package ruby.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Invoker("canStartSprinting")
    boolean ruby$canStartSprinting();

    @Invoker("shouldStopSprinting")
    boolean ruby$shouldStopSprinting();
}
