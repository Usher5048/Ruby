package ruby.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin {
    @Redirect(
            method = "setValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/SimpleOption$Callbacks;validate(Ljava/lang/Object;)Ljava/util/Optional;"
            )
    )
    private Optional<?> forceValid(@Coerce Object callbacks, Object value) {
        return Optional.of(value);
    }
}
