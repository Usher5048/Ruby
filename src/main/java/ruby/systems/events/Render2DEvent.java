package ruby.systems.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent extends Event {
    private final GuiGraphicsExtractor context;
    private final DeltaTracker tickCounter;

    public Render2DEvent(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        this.context = context;
        this.tickCounter = tickCounter;
    }

    public GuiGraphicsExtractor getContext() {
        return this.context;
    }
    public DeltaTracker getTickCounter() {
        return this.tickCounter;
    }
}
