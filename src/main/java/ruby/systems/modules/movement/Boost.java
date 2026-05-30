package ruby.systems.modules.movement;

import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class Boost extends Module {
    private final DoubleValue speed = config.create(new DoubleValue.Builder("Speed")
            .description("Boost speed")
            .defaultValue(1.0).min(0.1).max(3.0).step(0.1)
            .build());
    public Boost() {
        super("Boost", "Boosts you in the direction you are looking.", ModuleType.MOVEMENT);
        this.keybind.togglesOnRelease(true);
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null) return;
        float yaw = RubyClient.client.player.getYaw();
        float pitch = RubyClient.client.player.getPitch();

        Vec3d looking = Vec3d.fromPolar(pitch,yaw).multiply(this.speed.value());
        RubyClient.client.player.setVelocity(looking);
    }
}
