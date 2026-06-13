package ruby.systems.modules.movement;

import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * LiquidBounce {@code NoJumpDelay} port. Logic lives in {@link ruby.mixin.LivingEntityMixin}.
 */
public class NoJumpDelay extends Module {
    public static NoJumpDelay INSTANCE;

    public NoJumpDelay() {
        super("No Jump Delay", "Removes the delay between jumps.", ModuleType.MOVEMENT);
        INSTANCE = this;
    }
}
