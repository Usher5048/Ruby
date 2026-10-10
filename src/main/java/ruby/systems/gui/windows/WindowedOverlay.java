package ruby.systems.gui.windows;

import ruby.RubyClient;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;

public class WindowedOverlay extends Overlay {
    private final List<Window> windows = new ArrayList<>();
    private final Screen parent;

    protected WindowedOverlay(Screen parent) {
        this.parent = parent;
    }

    public Window addWindow(Window window) {
        this.windows.add(window);
        return window;
    }

    public List<Window> windows() {
        return this.windows;
    }
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        this.parent.extractRenderState(context, mouseX, mouseY, tickDelta);

        context.pose().pushMatrix();
        context.pose().scale(
                1f / (float) RubyClient.client.getWindow().getGuiScale(),
                1f / (float) RubyClient.client.getWindow().getGuiScale()
        );

        this.onRender(context, mouseX, mouseY);

        context.pose().popMatrix();

        for(Window window : this.windows())
            window.extractRenderState(context, mouseX, mouseY, tickDelta);
    }

    public void onTick() {}

    @Override
    public void tick() {
        for(int i = this.windows.size() - 1; i >= 0; i--)
            this.windows.get(i).tick();

        this.onTick();
    }
}
