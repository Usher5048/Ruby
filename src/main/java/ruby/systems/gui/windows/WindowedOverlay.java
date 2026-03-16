package ruby.systems.gui.windows;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import ruby.RubyClient;

import java.util.ArrayList;
import java.util.List;

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
    public void onRender(DrawContext context, int mouseX, int mouseY) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        this.parent.render(context, mouseX, mouseY, tickDelta);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(
                1f / (float) RubyClient.client.getWindow().getScaleFactor(),
                1f / (float) RubyClient.client.getWindow().getScaleFactor()
        );

        this.onRender(context, mouseX, mouseY);

        context.getMatrices().popMatrix();

        for(Window window : this.windows())
            window.render(context, mouseX, mouseY, tickDelta);
    }

    public void onTick() {}

    @Override
    public void tick() {
        for(int i = this.windows.size() - 1; i >= 0; i--)
            this.windows.get(i).tick();

        this.onTick();
    }
}
