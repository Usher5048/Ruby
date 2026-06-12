package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.helpers.RotationManager;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.combat.Criticals;

public class Sprint extends Module {
    public enum Mode { Strict, Rage }

    private final EnumValue<Mode> mode;

    public Sprint() {
        super("Sprint", "Automatically sprints for you.", ModuleType.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Sprinting mode.")
                .defaultValue(Mode.Strict)
                .build());

        config.create(new BooleanValue.Builder("Keep Sprint")
                .description("Keeps sprinting even after being hit.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player == null) return;

        if(Criticals.blocksSprintInput()) {
            player.setSprinting(false);
            return;
        }

        if(RotationManager.hasRotation()) return;

        if(shouldSprint(player)) player.setSprinting(true);
    }

    private boolean shouldSprint(ClientPlayerEntity player) {
        if(player.forwardSpeed <= 0) return false;
        if(mode.value() == Mode.Rage) return true;
        return !player.isSneaking()
                && !player.isUsingItem()
                && player.getHungerManager().getFoodLevel() > 6;
    }

    @Override
    public void onDisable() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player != null) player.setSprinting(false);
    }
}
