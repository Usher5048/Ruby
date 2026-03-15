package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import ruby.RubyClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class WindowedScreen extends Screen {
    private final List<Window> windows = new ArrayList<>();

    public WindowedScreen(String title) {
        super(Text.of(title));
    }

    public Window addWindow(Window window) {
        this.windows.add(window);
        return window;
    }

    private boolean runFocused(Function<Window, Boolean> action) {
        if(this.windows.isEmpty()) return false;
        return action.apply(this.windows.getLast());
    }

    public List<Window> windows() {
        return this.windows;
    }
    private Window focusWindow(Window window) {
        if(!this.windows.remove(window)) return null;
        if(!this.windows.isEmpty())
            this.windows.getLast().onFocusRemoved();

        this.windows.add(window);
        window.onFocused();

        return window;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        // Always dispatch to focused (last) window — prevents drag loss on fast mouse
        if (!this.windows.isEmpty()) {
            this.windows.getLast().mouseDragged(click, deltaX, deltaY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        return this.runFocused(win -> win.mouseReleased(click));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getScaleFactor());

        for(int i = this.windows.size() - 1; i >= 0; i--) {
            Window window = this.windows.get(i);

            if(sMouseX < window.getX()) continue;
            if(sMouseY < window.getY()) continue;
            if(sMouseX >= window.getX() + window.getWidth()) continue;
            if(sMouseY >= window.getY() + window.getHeight()) continue;

            this.focusWindow(window).mouseClicked(click, doubled);
            break;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double sMouseX = Math.round(mouseX * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(mouseY * RubyClient.client.getWindow().getScaleFactor());

        for(int i = this.windows.size() - 1; i >= 0; i--) {
            Window window = this.windows.get(i);

            if(sMouseX < window.getX()) continue;
            if(sMouseY < window.getY()) continue;
            if(sMouseX >= window.getX() + window.getWidth()) continue;
            if(sMouseY >= window.getY() + window.getHeight()) continue;

            window.mouseScrolled(
                    mouseX, mouseY,
                    horizontalAmount,
                    verticalAmount
            );

            break;
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.runFocused(win -> win.keyPressed(input))) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        return this.runFocused(win -> win.keyReleased(input));
    }

    @Override
    public boolean charTyped(CharInput input) {
        return this.runFocused(win -> win.charTyped(input));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double sMouseX = Math.round(mouseX * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(mouseY * RubyClient.client.getWindow().getScaleFactor());

        for(int i = this.windows.size() - 1; i >= 0; i--) {
            Window window = this.windows.get(i);

            if(sMouseX < window.getX()) continue;
            if(sMouseY < window.getY()) continue;
            if(sMouseX >= window.getX() + window.getWidth()) continue;
            if(sMouseY >= window.getY() + window.getHeight()) continue;

            window.mouseMoved(mouseX, mouseY);
            break;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        for(Window window : this.windows())
            window.render(context, mouseX, mouseY, tickDelta);
    }
}
