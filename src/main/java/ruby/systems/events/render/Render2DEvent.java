package ruby.systems.events.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import ruby.systems.events.Event;

public class Render2DEvent extends Event {
    private final DrawContext context;
    private final RenderTickCounter tickCounter;

    public Render2DEvent(DrawContext context, RenderTickCounter tickCounter) {
        this.context = context;
        this.tickCounter = tickCounter;
    }

    public DrawContext getContext() {
        return this.context;
    }
    public RenderTickCounter getTickCounter() {
        return this.tickCounter;
    }
}
