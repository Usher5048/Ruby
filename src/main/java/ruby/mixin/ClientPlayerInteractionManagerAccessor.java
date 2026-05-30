package ruby.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
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
}
