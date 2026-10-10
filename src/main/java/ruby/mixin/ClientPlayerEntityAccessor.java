package ruby.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface ClientPlayerEntityAccessor {
    @Invoker("canStartSprinting")
    boolean ruby$canStartSprinting();

    @Invoker("shouldStopRunSprinting")
    boolean ruby$shouldStopRunSprinting();

    @Invoker("shouldStopSwimSprinting")
    boolean ruby$shouldStopSwimSprinting();
}
