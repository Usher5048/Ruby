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

    public void onTick() {}

    public boolean onFocused() {
        return true;
    }
    public boolean onFocusRemoved() {
        return true;
    }

    public void drawText(FontRenderer font, DrawContext context, String text, int x, int y, int color) {
        font.draw(context, text, x, y, color);
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

    public void drawBorder(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
        context.fill(x1, y1, x2, y1 + thickness, color);
        context.fill(x1, y2, x2, y2 - thickness, color);
        context.fill(x1, y1, x1 + thickness, y2, color);
        context.fill(x2, y1, x2 - thickness, y2, color);
    }

    public void drawRoundedRect(
            DrawContext context,
            int x1, int y1,
            int x2, int y2,
            int fillColor, int strokeColor,
            int strokeThickness, int radius,
            boolean fill, boolean stroke
    ) {
        if(!fill && !stroke) return;

        int x1r = x1 + radius;
        int x2r = x2 - radius;
        int y1r = y1 + radius;
        int y2r = y2 - radius;

        int x1s = x1 + strokeThickness;
        int x2s = x2 - strokeThickness;
        int y1s = y1 + strokeThickness;
        int y2s = y2 - strokeThickness;

        if(fill) {
            context.fill(x1 , y1r, x2 , y2r, fillColor);
            context.fill(x1r, y1 , x2r, y1r, fillColor);
            context.fill(x1r, y2r, x2r, y2, fillColor);
        }

        if(stroke) {
            context.fill(x1r, y1 , x2r, y1s, strokeColor);
            context.fill(x1r, y2s, x2r, y2 , strokeColor);
            context.fill(x1,  y1r, x1s, y2r, strokeColor);
            context.fill(x2s, y1r, x2 , y2r, strokeColor);
        }

        int r2 = radius * radius;
        int ir = Math.max(0, radius - strokeThickness);
        int ir2 = ir * ir;

        for(int i = 0; i < radius; i++) {
            int i1 = i + 1;
            int i2 = i * i;

            int leftX = i1 - radius;
            int leftX2 = leftX * leftX;

            double lby = Math.sqrt(r2 - leftX2);
            double lty = radius - lby;
            double rby = Math.sqrt(r2 - i2);
            double rty = radius - rby;

            int tl = (int) Math.floor(y1  + lty);
            int tr = (int) Math.floor(y1  + rty);
            int bl = (int) Math.ceil (y2r + lby);
            int br = (int) Math.ceil (y2r + rby);

            if(fill) {
                context.fill(x1  + i,  tl, x1  + i1, y1r, fillColor);
                context.fill(x2r + i,  tr, x2r + i1, y1r, fillColor);
                context.fill(x1  + i, y2r, x1  + i1,  bl, fillColor);
                context.fill(x2r + i, y2r, x2r + i1,  br, fillColor);
            }

            if(stroke) {
                double lbyi = Math.abs(leftX) <= ir ? Math.sqrt(ir2 - leftX2) : 0;
                double ltyi = radius - lbyi;
                double rbyi = i <= ir ? Math.sqrt(ir2 - i2) : 0;
                double rtyi = radius - rbyi;

                int tli = (int) Math.floor(y1  + ltyi);
                int tri = (int) Math.floor(y1  + rtyi);
                int bli = (int) Math.ceil (y2r + lbyi);
                int bri = (int) Math.ceil (y2r + rbyi);

                if(tli > tl) context.fill(x1  + i,  tl, x1  + i1, tli, strokeColor);
                if(tri > tr) context.fill(x2r + i,  tr, x2r + i1, tri, strokeColor);
                if(bl > bli) context.fill(x1  + i, bli, x1  + i1,  bl, strokeColor);
                if(br > bri) context.fill(x2r + i, bri, x2r + i1,  br, strokeColor);
            }
        }
    }

    public double getTextHeight(FontRenderer font) {
        return font.fontHeight;
    }
    public double getTextWidth(FontRenderer font, String text) {
        return font.getWidth(text);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(
                1f / (float) RubyClient.client.getWindow().getScaleFactor(),
                1f / (float) RubyClient.client.getWindow().getScaleFactor()
        );

        int sMouseX = mouseX * RubyClient.client.getWindow().getScaleFactor();
        int sMouseY = mouseY * RubyClient.client.getWindow().getScaleFactor();

        double sX = this.x / (double) RubyClient.client.getWindow().getScaleFactor();
        double sY = this.y / (double) RubyClient.client.getWindow().getScaleFactor();

        context.getMatrices().translate(this.x, this.y);
        this.onRender(context, sMouseX - this.x, sMouseY - this.y);
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

        for(Window window : this.windows()) {
            window.render(
                    context,
                    (int) (mouseX - sX),
                    (int) (mouseY - sY),
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

    public void tick() {
        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            this.children.get(i).tick();
        }

        this.onTick();
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
        if(this.runFocused(win -> win.keyPressed(input))) return true;
        return this.onKeyPress(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if(this.runFocused(win -> win.keyReleased(input))) return true;
        return this.onKeyRelease(input);
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
        if(this.runFocused(win -> win.charTyped(input))) return true;
        return this.onCharTyped(input);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
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

    private boolean propagateFocusRemoved() {
        if(!this.onFocusRemoved()) return false;
        for(Window window : this.children) {
            if(!window.propagateFocusRemoved())
                return false;
        }

        return true;
    }

    private Window focusWindow(Window window) {
        if(!this.children.isEmpty() && this.children.getLast() == window)
            return window;

        if(!window.onFocused()) return window;

        if(!this.children.isEmpty()) {
            if(!this.children.getLast().propagateFocusRemoved())
                return window;
        }

        if(!this.children.remove(window)) return null;
        this.children.add(window);

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
