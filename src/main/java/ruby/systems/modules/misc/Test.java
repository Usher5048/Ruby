package ruby.systems.modules.misc;

import ruby.systems.events.Events;
import ruby.systems.events.Render2DEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class Test extends Module {
    public Test() {
        super("Test", "dw abt it", ModuleType.MISC);

        Events.PACKET.register(PacketEvents.RECEIVE, event -> {
            if(!this.enabled()) return;

        });
    }

    @Override
    public void tick() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void render2D(Render2DEvent event) {

    }

    @Override
    public void render3D() {

    }
}
