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
    private final int[] scissorSpace = new int[] {-1, -1, -1, -1};
    private int layoutStamp;
    private int x;
    private int y;

    protected boolean handleChildren;
    /** Child that took the button down. Drags stay with it after the cursor leaves. */
    private Window pointerChild;
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

    protected void onRenderForeground(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {}

    /** Header and clipped regions can reject children that have been scrolled underneath them. */
    protected boolean isChildInteractive(Window child, double localX, double localY) {
        return true;
    }

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
        this.scissorSpace[0] = -1;
        this.scissorSpace[1] = -1;
        this.scissorSpace[2] = -1;
        this.scissorSpace[3] = -1;
    }
    public void enableCutout(int x1, int y1, int x2, int y2) {
        this.scissorSpace[0] = x1;
        this.scissorSpace[1] = y1;
        this.scissorSpace[2] = x2;
        this.scissorSpace[3] = y2;
    }

    /** Bumped when this window's height changes so parents can skip a layout walk. */
    public int layoutStamp() {
        return this.layoutStamp;
    }

    protected void bumpLayout() {
        this.layoutStamp++;
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
        int scale = RubyClient.client.getWindow().getGuiScale();
        float inv = 1f / scale;
        double invD = 1.0 / scale;

        context.pose().pushMatrix();
        context.pose().scale(inv, inv);

        int sMouseX = mouseX * scale;
        int sMouseY = mouseY * scale;
        double sX = this.x * invD;
        double sY = this.y * invD;

        context.pose().translate(this.x, this.y);
        this.onRender(context, sMouseX - this.x, sMouseY - this.y, dt);
        context.pose().popMatrix();

        if(this.handleChildren) {
            context.pose().pushMatrix();
            context.pose().translate((float) sX, (float) sY);

            int cutL = this.scissorSpace[0];
            int cutT = this.scissorSpace[1];
            int cutR = this.scissorSpace[2];
            int cutB = this.scissorSpace[3];
            boolean scissorEnabled = cutL != -1 && cutT != -1 && cutR != -1 && cutB != -1;

            if(scissorEnabled) {
                context.enableScissor(cutL / scale, cutT / scale, cutR / scale, cutB / scale);
            }

            int childMouseX = (int) (mouseX - sX);
            int childMouseY = (int) (mouseY - sY);
            if (this.blockChildHover(sMouseX - this.x, sMouseY - this.y)) {
                childMouseX = -100000;
                childMouseY = -100000;
            }
            for(int i = 0, n = this.children.size(); i < n; i++) {
                this.children.get(i).extractRenderState(context, childMouseX, childMouseY, dt);
            }

            if(scissorEnabled) context.disableScissor();
            context.pose().popMatrix();
        }

        context.pose().pushMatrix();
        context.pose().scale(inv, inv);
        context.pose().translate(this.x, this.y);
        this.onRenderForeground(context, sMouseX - this.x, sMouseY - this.y, dt);
        context.pose().popMatrix();
    }

    /** True while the pointer is on this window's own chrome, so children do not hover underneath. */
    protected boolean blockChildHover(int localX, int localY) {
        return false;
    }

    private int guiScale() {
        return RubyClient.client.getWindow().getGuiScale();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        int scale = this.guiScale();
        double sMouseX = Math.round(click.x() * scale);
        double sMouseY = Math.round(click.y() * scale);
        double sDeltaX = Math.round(deltaX * scale);
        double sDeltaY = Math.round(deltaY * scale);

        double localX = sMouseX - this.x;
        double localY = sMouseY - this.y;

        if (this.pointerChild != null && this.children.contains(this.pointerChild)) {
            this.pointerChild.mouseDragged(this.childClick(click), deltaX, deltaY);
            if (this.isDragging() && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                this.x += (int) sDeltaX;
                this.y += (int) sDeltaY;
            }
            return true;
        }

        if(this.handleChildren) {
            for(int i = this.children.size() - 1; i >= 0; i--) {
                Window window = this.children.get(i);
                if(!this.isChildInteractive(window, localX, localY)) continue;
                if(!this.containsChild(window, localX, localY)) continue;

                if(this.focusWindow(window).mouseDragged(this.childClick(click), deltaX, deltaY)) return true;
                break;
            }
        }

        if(this.onMouseDragged(new MouseButtonEvent(
                localX,
                localY,
                click.buttonInfo()
        ), deltaX, deltaY)) return true;

        if(this.isDragging() && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.x += (int) sDeltaX;
            this.y += (int) sDeltaY;
        }

        return false;
    }

    public void tick() {
        if(this.handleChildren) {
            for(int i = this.children.size() - 1; i >= 0; i--) {
                this.children.get(i).tick();
            }
        }

        this.onTick();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int scale = this.guiScale();
        double sMouseX = Math.round(click.x() * scale);
        double sMouseY = Math.round(click.y() * scale);

        double sX = this.x / (double) scale;
        double sY = this.y / (double) scale;
        double localX = sMouseX - this.x;
        double localY = sMouseY - this.y;

        this.pointerChild = null;
        if(this.handleChildren) {
            for(int i = this.children.size() - 1; i >= 0; i--) {
                Window window = this.children.get(i);
                if(!this.isChildInteractive(window, localX, localY)) continue;
                if(!this.containsChild(window, localX, localY)) continue;

                if(this.focusWindow(window).mouseClicked(this.childClick(click), doubled)) {
                    this.pointerChild = window;
                    return true;
                }
                break;
            }
        }

        if(this.onMouseDown(new MouseButtonEvent(
                localX,
                localY,
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
        int scale = this.guiScale();
        double sMouseX = Math.round(click.x() * scale);
        double sMouseY = Math.round(click.y() * scale);
        double localX = sMouseX - this.x;
        double localY = sMouseY - this.y;

        if (this.pointerChild != null) {
            Window held = this.pointerChild;
            this.pointerChild = null;
            if (this.children.contains(held)) held.mouseReleased(this.childClick(click));
            this.onMouseUp(new MouseButtonEvent(localX, localY, click.buttonInfo()));
            this.setDragging(false);
            return true;
        }

        if(this.handleChildren) {
            for(int i = this.children.size() - 1; i >= 0; i--) {
                Window window = this.children.get(i);
                if(!this.isChildInteractive(window, localX, localY)) continue;
                if(!this.containsChild(window, localX, localY)) continue;

                if(window.mouseReleased(this.childClick(click))) return true;
                break;
            }
        }

        if(this.onMouseUp(new MouseButtonEvent(
                localX,
                localY,
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
        int scale = this.guiScale();
        double sMouseX = Math.round(mouseX * scale);
        double sMouseY = Math.round(mouseY * scale);

        double sX = this.x / (double) scale;
        double sY = this.y / (double) scale;
        double localX = sMouseX - this.x;
        double localY = sMouseY - this.y;

        if(this.handleChildren) {
            for(int i = this.children.size() - 1; i >= 0; i--) {
                Window window = this.children.get(i);
                if(!this.isChildInteractive(window, localX, localY)) continue;
                if(!this.containsChild(window, localX, localY)) continue;

                if(window.mouseScrolled(
                        mouseX - sX,
                        mouseY - sY,
                        horizontalAmount,
                        verticalAmount
                )) return true;
                break;
            }
        }

        return this.onMouseScrolled(localX, localY, horizontalAmount, verticalAmount);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int scale = this.guiScale();
        double sMouseX = Math.round(mouseX * scale);
        double sMouseY = Math.round(mouseY * scale);

        double sX = this.x / (double) scale;
        double sY = this.y / (double) scale;
        double localX = sMouseX - this.x;
        double localY = sMouseY - this.y;

        if(this.handleChildren) {
            for(int i = this.children.size() - 1; i >= 0; i--) {
                Window window = this.children.get(i);
                if(!this.isChildInteractive(window, localX, localY)) continue;
                if(!this.containsChild(window, localX, localY)) continue;

                window.onMouseMoved(mouseX - sX, mouseY - sY);
                break;
            }
        }

        this.onMouseMoved(localX, localY);
    }

    private MouseButtonEvent childClick(MouseButtonEvent click) {
        int scale = this.guiScale();
        double sX = this.x / (double) scale;
        double sY = this.y / (double) scale;
        return new MouseButtonEvent(click.x() - sX, click.y() - sY, click.buttonInfo());
    }

    private boolean containsChild(Window window, double localX, double localY) {
        int x = window.getX();
        int y = window.getY();
        return localX >= x && localY >= y && localX < x + window.getWidth() && localY < y + window.getHeight();
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
