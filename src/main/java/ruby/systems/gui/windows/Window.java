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
    private Window focusedChild = null;
    private float textScale = 1;
    private int x;
    private int y;

    protected boolean handleChildren;
    protected boolean reorderChildren = true;
    protected boolean clipChildren = false;
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
        double sf = scaleFactor();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(1f / (float) sf, 1f / (float) sf);
        context.getMatrices().translate(this.x, this.y);

        // Convert mouse from scaled-relative to pixel-local
        int localMouseX = (int) Math.round(mouseX * sf) - this.x;
        int localMouseY = (int) Math.round(mouseY * sf) - this.y;
        this.onRender(context, localMouseX, localMouseY);
        context.getMatrices().popMatrix();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(
                this.x / (float) sf,
                this.y / (float) sf
        );

        if(!this.handleChildren) {
            context.getMatrices().popMatrix();
            return;
        }

        // Pass scaled coords relative to this window's position
        double sX = this.x / sf;
        double sY = this.y / sf;

        if (this.clipChildren) {
            context.enableScissor(
                    0,
                    0,
                    (int) Math.ceil(this.getWidth() / sf),
                    (int) Math.ceil(this.getHeight() / sf)
            );
        }

        for(Element element : this.children().toArray(new Element[0])) {
            Window window = (Window) element;

            window.render(
                    context,
                    (int) (mouseX - sX),
                    (int) (mouseY - sY),
                    deltaTicks
            );
        }

        if (this.clipChildren) {
            context.disableScissor();
        }

        context.getMatrices().popMatrix();
    }

    private static double scaleFactor() {
        return RubyClient.client.getWindow().getScaleFactor();
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double sf = scaleFactor();
        double sMouseX = Math.round(click.x() * sf);
        double sMouseY = Math.round(click.y() * sf);

        double sX = this.x / sf;
        double sY = this.y / sf;

        // Always dispatch drag to focused child (no bounds check, handles fast mouse)
        if (this.handleChildren && this.focusedChild != null) {
            if (this.focusedChild.mouseDragged(new Click(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ), deltaX, deltaY)) return true;
        }

        if(this.onMouseDragged(new Click(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ), deltaX, deltaY)) return true;

        if(this.isDragging() && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.x += (int) Math.round(deltaX * scaleFactor());
            this.y += (int) Math.round(deltaY * scaleFactor());
        }

        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double sf = scaleFactor();
        double sMouseX = Math.round(click.x() * sf);
        double sMouseY = Math.round(click.y() * sf);

        double sX = this.x / sf;
        double sY = this.y / sf;

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
        double sf = scaleFactor();
        double sMouseX = Math.round(click.x() * sf);
        double sMouseY = Math.round(click.y() * sf);

        double sX = this.x / sf;
        double sY = this.y / sf;

        // Always dispatch release to focused child (handles release outside bounds)
        if (this.handleChildren && this.focusedChild != null) {
            if (this.focusedChild.mouseReleased(new Click(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ))) return true;
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
        double sf = scaleFactor();
        double sMouseX = Math.round(mouseX * sf);
        double sMouseY = Math.round(mouseY * sf);

        double sX = this.x / sf;
        double sY = this.y / sf;

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
        double sf = scaleFactor();
        double sMouseX = Math.round(mouseX * sf);
        double sMouseY = Math.round(mouseY * sf);

        double sX = this.x / sf;
        double sY = this.y / sf;

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
        if (this.focusedChild != null && this.children.contains(this.focusedChild))
            return action.apply(this.focusedChild);
        if(this.children.isEmpty()) return false;
        return action.apply(this.children.getLast());
    }

    private void propagateFocusRemoved() {
        this.onFocusRemoved();
        for(Window window : this.children)
            window.propagateFocusRemoved();
    }

    private Window focusWindow(Window window) {
        if (this.focusedChild != null && this.focusedChild != window) {
            this.focusedChild.propagateFocusRemoved();
        }
        if (this.reorderChildren) {
            if (!this.children.remove(window)) return null;
            this.children.add(window);
        }
        this.focusedChild = window;
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
