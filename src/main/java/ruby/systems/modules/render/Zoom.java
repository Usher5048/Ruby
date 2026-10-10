package ruby.systems.modules.render;

import ruby.RubyClient;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class Zoom extends Module {
    private int originalFov;
    private final IntegerValue zoomedFOV = config.create(new IntegerValue.Builder("Zoomed FOV")
            .description("FOV to use for zooming in")
            .defaultValue(20)
            .range(5, 175)
            .build());

    public Zoom() {
        super("Zoom", "Zooms in by lowering your FOV.", ModuleType.RENDER);
    }

    @Override
    public void onEnable() {
        this.originalFov = RubyClient.client.options.fov().get();
    }

    @Override
    public void tick() {
        RubyClient.client.options.fov().set(this.zoomedFOV.value());
    }

    @Override
    public void onDisable() {
        RubyClient.client.options.fov().set(this.originalFov);
    }
}
