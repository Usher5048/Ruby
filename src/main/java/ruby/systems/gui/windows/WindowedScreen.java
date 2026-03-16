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

public class WindowedScreen extends Screen {
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
        if(!this.windows.isEmpty() && this.windows.getLast() == window)
            return window;

        if(!this.windows.remove(window)) return null;
        if(!this.windows.isEmpty())
            this.windows.getLast().onFocusRemoved();

        this.windows.add(window);
        window.onFocused();

        return window;
    }

    public boolean onMouseDown(Click click, boolean doubled) {
        return false;
    }
    public boolean onMouseUp(Click click) {
        return false;
    }
    public boolean onMouseDragged(Click click, double deltaX, double deltaY) {
        return false;
    }
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }
    public void onMouseMoved(double mouseX, double mouseY) {}

    public boolean onKeyPress(KeyInput input) {
        return false;
    }
    public boolean onKeyRelease(KeyInput input) {
        return false;
    }
    public boolean onCharTyped(CharInput input) {
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        // only good change made by ai
        // Always dispatch to focused (last) window — prevents drag loss on fast mouse
        if(!this.windows.isEmpty()) {
            if(this.windows.getLast().mouseDragged(click, deltaX, deltaY))
                return true;
        }

        return this.onMouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if(this.runFocused(win -> win.mouseReleased(click))) return true;
        return this.onMouseUp(click);
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

            if(this.focusWindow(window).mouseClicked(click, doubled))
                return true;

            break;
        }

        return this.onMouseDown(click, doubled);
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

            if(window.mouseScrolled(
                    mouseX, mouseY,
                    horizontalAmount,
                    verticalAmount
            )) return true;

            break;
        }

        return this.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if(this.runFocused(win -> win.keyPressed(input))) return true;
        if(this.onKeyPress(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if(this.runFocused(win -> win.keyReleased(input))) return true;
        return this.onKeyRelease(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if(this.runFocused(win -> win.charTyped(input))) return true;
        return this.onCharTyped(input);
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

        this.onMouseMoved(mouseX, mouseY);
    }

    public void onRender(DrawContext context, int mouseX, int mouseY) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
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

    @Override
    protected void clearChildren() {
        this.windows.clear();
        super.clearChildren();
    }
}
