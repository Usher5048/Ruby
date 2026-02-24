package ruby.systems.gui.windows;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.systems.gui.text.FontRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Window extends AbstractParentElement implements Drawable, Selectable {
    private final List<Window> children = new ArrayList<>();
    private float textScale = 1;
    private int x;
    private int y;

    protected boolean handleChildren;
    protected int[] draggableBounds;
    protected int width;
    protected int height;

    public Window(int x, int y, int width, int height) {
        super();

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.handleChildren = true;
        this.draggableBounds = new int[]{
                0, 0,
                this.width, this.height
        };
    }

    public void onRender(DrawContext context, int mouseX, int mouseY) {}

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

    public void onFocused() {}
    public void onFocusRemoved() {}

    public void drawText(FontRenderer font, DrawContext context, String text, int x, int y, int color) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(this.textScale, this.textScale);

        font.draw(context, text, 0, 0, color);

        context.getMatrices().popMatrix();
    }

    public void drawCenteredText(FontRenderer font, DrawContext context, String text, int x, int y, int color) {
        this.drawText(
                font, context,
                text,
                (int) (x - this.getTextWidth(font, text) / 2),
                (int) (y - this.getTextHeight(font) / 2),
                color
        );
    }

    public double getTextHeight(FontRenderer font) {
        return font.fontHeight * this.textScale;
    }
    public double getTextWidth(FontRenderer font, String text) {
        return font.getWidth(text) * this.textScale;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(
                1f / (float) RubyClient.client.getWindow().getScaleFactor(),
                1f / (float) RubyClient.client.getWindow().getScaleFactor()
        );

        context.getMatrices().translate(this.x, this.y);
        this.onRender(context, mouseX, mouseY);
        context.getMatrices().popMatrix();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(
                this.x / (float) RubyClient.client.getWindow().getScaleFactor(),
                this.y / (float) RubyClient.client.getWindow().getScaleFactor()
        );

        if(!this.handleChildren) {
            context.getMatrices().popMatrix();
            return;
        }

        for(Element element : this.children().toArray(new Element[0])) {
            Window window = (Window) element;

            window.render(
                    context,
                    mouseX - window.getX(),
                    mouseY - window.getY(),
                    deltaTicks
            );
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getScaleFactor());
        double sDeltaX = Math.round(deltaX * RubyClient.client.getWindow().getScaleFactor());
        double sDeltaY = Math.round(deltaY * RubyClient.client.getWindow().getScaleFactor());

        double sX = this.x / (double) RubyClient.client.getWindow().getScaleFactor();
        double sY = this.y / (double) RubyClient.client.getWindow().getScaleFactor();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(this.focusWindow(window).mouseDragged(new Click(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ), deltaX, deltaY)) return true;
            break;
        }

        if(this.onMouseDragged(new Click(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ), deltaX, deltaY)) return true;

        if(this.isDragging() && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.x += sDeltaX;
            this.y += sDeltaY;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getScaleFactor());

        double sX = this.x / (double) RubyClient.client.getWindow().getScaleFactor();
        double sY = this.y / (double) RubyClient.client.getWindow().getScaleFactor();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(this.focusWindow(window).mouseClicked(new Click(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ), doubled)) return true;
            break;
        }

        if(this.onMouseDown(new Click(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ), doubled)) return true;

        if(
                click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT &&
                sMouseX > this.x + this.draggableBounds[0] &&
                sMouseX < this.x + this.draggableBounds[2] &&
                sMouseY > this.y + this.draggableBounds[1] &&
                sMouseY < this.y + this.draggableBounds[3]
        ) this.setDragging(true);

        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getScaleFactor());

        double sX = this.x / (double) RubyClient.client.getWindow().getScaleFactor();
        double sY = this.y / (double) RubyClient.client.getWindow().getScaleFactor();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(window.mouseReleased(new Click(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ))) return true;
            break;
        }

        if(this.onMouseUp(new Click(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ))) return true;

        this.setDragging(false);
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return this.runFocused(win -> win.keyPressed(input)) ||
                this.onKeyPress(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        return this.runFocused(win -> win.keyReleased(input)) ||
                this.onKeyRelease(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double sMouseX = Math.round(mouseX * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(mouseY * RubyClient.client.getWindow().getScaleFactor());

        double sX = this.x / (double) RubyClient.client.getWindow().getScaleFactor();
        double sY = this.y / (double) RubyClient.client.getWindow().getScaleFactor();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(window.onMouseScrolled(
                    mouseX - sX,
                    mouseY - sY,
                    horizontalAmount,
                    verticalAmount
            )) return true;
            break;
        }

        return this.onMouseScrolled(
                sMouseX - this.x,
                sMouseY - this.y,
                horizontalAmount,
                verticalAmount
        );
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double sMouseX = Math.round(mouseX * RubyClient.client.getWindow().getScaleFactor());
        double sMouseY = Math.round(mouseY * RubyClient.client.getWindow().getScaleFactor());

        double sX = this.x / (double) RubyClient.client.getWindow().getScaleFactor();
        double sY = this.y / (double) RubyClient.client.getWindow().getScaleFactor();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            window.onMouseMoved(
                    mouseX - sX,
                    mouseY - sY
            );

            break;
        }

        this.onMouseMoved(
                sMouseX - this.x,
                sMouseY - this.y
        );
    }

    @Override
    public boolean charTyped(CharInput input) {
        return this.runFocused(win -> win.charTyped(input)) ||
                this.onCharTyped(input);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }
    public float getTextScale() {
        return this.textScale;
    }

    public int getX() {
        return this.x;
    }
    public int getY() {
        return this.y;
    }
    public int getWidth() {
        return this.width;
    }
    public int getHeight() {
        return this.height;
    }

    public Window addWindow(Window child) {
        this.children.add(child);
        return child;
    }

    private boolean runFocused(Function<Window, Boolean> action) {
        if(this.children.isEmpty()) return false;
        return action.apply(this.children.getLast());
    }

    private void propagateFocusRemoved() {
        this.onFocusRemoved();
        for(Window window : this.children)
            window.propagateFocusRemoved();
    }

    private Window focusWindow(Window window) {
        if(!this.children.remove(window)) return null;
        if(!this.children.isEmpty())
            this.children.getLast().propagateFocusRemoved();

        this.children.add(window);
        window.onFocused();

        return window;
    }

    public List<Window> windows() {
        return this.children;
    }

    @Override
    public List<? extends Element> children() {
        return this.children;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}
}
