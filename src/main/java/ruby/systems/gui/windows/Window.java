package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.*;
import ruby.RubyClient;
import ruby.systems.gui.text.FontRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Window extends AbstractContainerEventHandler implements GuiEventListener, Renderable {
    private final List<Window> children = new ArrayList<>();
    private int[] scissorSpace;
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
        this.draggableBounds = new int[] {0, 0, this.width, this.height};

        this.disableCutout();
    }

    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {}

    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        return false;
    }
    public boolean onMouseUp(MouseButtonEvent click) {
        return false;
    }
    public boolean onMouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        return false;
    }
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }
    public void onMouseMoved(double mouseX, double mouseY) {}

    public boolean onKeyPress(KeyEvent input) {
        return false;
    }
    public boolean onKeyRelease(KeyEvent input) {
        return false;
    }
    public boolean onCharTyped(CharacterEvent input) {
        return false;
    }

    public void onTick() {}

    public boolean onFocused() {
        return true;
    }
    public boolean onFocusRemoved() {
        return true;
    }

    public void disableCutout() {
        this.scissorSpace = new int[] {-1, -1, -1, -1};
    }
    public void enableCutout(int x1, int y1, int x2, int y2) {
        this.scissorSpace = new int[] {x1, y1, x2, y2};
    }

    public void drawText(FontRenderer font, GuiGraphicsExtractor context, String text, int x, int y, int color) {
        font.draw(context, text, x, y, color);
    }

    public void drawCenteredText(FontRenderer font, GuiGraphicsExtractor context, String text, int x, int y, int color) {
        this.drawText(
                font, context,
                text,
                (int) (x - this.getTextWidth(font, text) / 2),
                (int) (y - this.getTextHeight(font) / 2),
                color
        );
    }

    public void drawTextInRect(
            FontRenderer font, GuiGraphicsExtractor context, String text,
            int x1, int y1, int x2, int y2, int color
    ) {
        FontRenderer.TextBounds bounds = font.measure(text);
        int tx = x1 + (x2 - x1 - bounds.width()) / 2 - bounds.left();
        int ty = y1 + (y2 - y1 - bounds.height()) / 2 - bounds.top();
        font.draw(context, text, tx, ty, color);
    }

    public void drawBorder(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color, int thickness) {
        context.fill(x1, y1, x2, y1 + thickness, color);
        context.fill(x1, y2, x2, y2 - thickness, color);
        context.fill(x1, y1, x1 + thickness, y2, color);
        context.fill(x2, y1, x2 - thickness, y2, color);
    }

    public void drawRoundedRect(
            GuiGraphicsExtractor context,
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        context.pose().pushMatrix();
        context.pose().scale(
                1f / (float) RubyClient.client.getWindow().getGuiScale(),
                1f / (float) RubyClient.client.getWindow().getGuiScale()
        );

        int sMouseX = mouseX * RubyClient.client.getWindow().getGuiScale();
        int sMouseY = mouseY * RubyClient.client.getWindow().getGuiScale();

        double sX = this.x / (double) RubyClient.client.getWindow().getGuiScale();
        double sY = this.y / (double) RubyClient.client.getWindow().getGuiScale();

        context.pose().translate(this.x, this.y);
        this.onRender(context, sMouseX - this.x, sMouseY - this.y, dt);
        context.pose().popMatrix();

        context.pose().pushMatrix();
        context.pose().translate((float) sX, (float) sY);

        if(!this.handleChildren) {
            context.pose().popMatrix();
            return;
        }

        boolean scissorEnabled =
                this.scissorSpace[0] != -1 && this.scissorSpace[1] != -1 &&
                this.scissorSpace[2] != -1 && this.scissorSpace[3] != -1;

        if(scissorEnabled) {
            context.enableScissor(
                    this.scissorSpace[0] / RubyClient.client.getWindow().getGuiScale(),
                    this.scissorSpace[1] / RubyClient.client.getWindow().getGuiScale(),
                    this.scissorSpace[2] / RubyClient.client.getWindow().getGuiScale(),
                    this.scissorSpace[3] / RubyClient.client.getWindow().getGuiScale()
            );
        }

        for(Window window : this.windows()) {
            window.extractRenderState(
                    context,
                    (int) (mouseX - sX),
                    (int) (mouseY - sY),
                    dt
            );
        }

        if(scissorEnabled) context.disableScissor();
        context.pose().popMatrix();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getGuiScale());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getGuiScale());
        double sDeltaX = Math.round(deltaX * RubyClient.client.getWindow().getGuiScale());
        double sDeltaY = Math.round(deltaY * RubyClient.client.getWindow().getGuiScale());

        double sX = this.x / (double) RubyClient.client.getWindow().getGuiScale();
        double sY = this.y / (double) RubyClient.client.getWindow().getGuiScale();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(this.focusWindow(window).mouseDragged(new MouseButtonEvent(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ), deltaX, deltaY)) return true;
            break;
        }

        if(this.onMouseDragged(new MouseButtonEvent(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ), deltaX, deltaY)) return true;

        if(this.isDragging() && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.x += (int) sDeltaX;
            this.y += (int) sDeltaY;
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
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getGuiScale());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getGuiScale());

        double sX = this.x / (double) RubyClient.client.getWindow().getGuiScale();
        double sY = this.y / (double) RubyClient.client.getWindow().getGuiScale();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(this.focusWindow(window).mouseClicked(new MouseButtonEvent(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ), doubled)) return true;
            break;
        }

        if(this.onMouseDown(new MouseButtonEvent(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ), doubled)) return true;

        if(
                click.button() == InputConstants.MOUSE_BUTTON_LEFT &&
                sMouseX > this.x + this.draggableBounds[0] &&
                sMouseX < this.x + this.draggableBounds[2] &&
                sMouseY > this.y + this.draggableBounds[1] &&
                sMouseY < this.y + this.draggableBounds[3]
        ) this.setDragging(true);

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        double sMouseX = Math.round(click.x() * RubyClient.client.getWindow().getGuiScale());
        double sMouseY = Math.round(click.y() * RubyClient.client.getWindow().getGuiScale());

        double sX = this.x / (double) RubyClient.client.getWindow().getGuiScale();
        double sY = this.y / (double) RubyClient.client.getWindow().getGuiScale();

        for(int i = this.children.size() - 1; i >= 0; i--) {
            if(!this.handleChildren) continue;
            Window window = this.children.get(i);

            if(sMouseX - this.x < window.getX()) continue;
            if(sMouseY - this.y < window.getY()) continue;
            if(sMouseX - this.x >= window.getX() + window.getWidth()) continue;
            if(sMouseY - this.y >= window.getY() + window.getHeight()) continue;

            if(window.mouseReleased(new MouseButtonEvent(
                    click.x() - sX,
                    click.y() - sY,
                    click.buttonInfo()
            ))) return true;
            break;
        }

        if(this.onMouseUp(new MouseButtonEvent(
                sMouseX - this.x,
                sMouseY - this.y,
                click.buttonInfo()
        ))) return true;

        this.setDragging(false);
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if(this.runFocused(win -> win.keyPressed(input))) return true;
        return this.onKeyPress(input);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if(this.runFocused(win -> win.keyReleased(input))) return true;
        return this.onKeyRelease(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double sMouseX = Math.round(mouseX * RubyClient.client.getWindow().getGuiScale());
        double sMouseY = Math.round(mouseY * RubyClient.client.getWindow().getGuiScale());

        double sX = this.x / (double) RubyClient.client.getWindow().getGuiScale();
        double sY = this.y / (double) RubyClient.client.getWindow().getGuiScale();

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
        double sMouseX = Math.round(mouseX * RubyClient.client.getWindow().getGuiScale());
        double sMouseY = Math.round(mouseY * RubyClient.client.getWindow().getGuiScale());

        double sX = this.x / (double) RubyClient.client.getWindow().getGuiScale();
        double sY = this.y / (double) RubyClient.client.getWindow().getGuiScale();

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
    public boolean charTyped(CharacterEvent input) {
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
    public List<? extends GuiEventListener> children() {
        return this.children;
    }
}
