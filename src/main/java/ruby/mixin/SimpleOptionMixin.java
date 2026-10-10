package ruby.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import net.minecraft.client.OptionInstance;

@Mixin(OptionInstance.class)
public class SimpleOptionMixin {
    @Redirect(
            method = "set",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance$ValueSet;validateValue(Ljava/lang/Object;)Ljava/util/Optional;"
            )
    )
    private Optional<?> forceValid(@Coerce Object callbacks, Object value) {
        return Optional.of(value);
    }
}
