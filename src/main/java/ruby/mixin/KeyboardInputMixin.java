package ruby.mixin;

import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ruby.helpers.RotationManager;
import ruby.systems.modules.combat.Criticals;
import ruby.systems.modules.combat.Velocity;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Redirect(
            method = "tick",
            at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;")
    )
    private PlayerInput ruby$silentInput(
            boolean forward, boolean backward, boolean left, boolean right,
            boolean jump, boolean sneak, boolean sprint
    ) {
        if(Criticals.shouldForceJump()) jump = true;

        Velocity velocity = ruby.systems.modules.Modules.getByClass(Velocity.class);
        if (velocity != null && velocity.shouldJumpReset()) jump = true;

        PlayerInput raw = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
        return RotationManager.transformInput(raw);
    }
}
