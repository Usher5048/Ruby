package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public abstract class CollapsibleWindow extends Window {

    private final int headerHeight;
    private final int minWidth;

    public CollapsibleWindow(int x, int y, int minWidth, int headerHeight) {
        super(x, y, minWidth, headerHeight);
        this.headerHeight = headerHeight;
        this.minWidth = minWidth;
        this.draggableBounds = new int[] {0, 0, minWidth, headerHeight};
    }

    @Override
    public int getHeight() {
        this.height = this.headerHeight;
        if (!this.handleChildren) return this.headerHeight;
        for (Window child : this.windows()) {
            this.height += child.getHeight();
        }
        return this.height;
    }

    @Override
    public int getWidth() {
        this.width = this.minWidth;
        for (Window child : this.windows()) {
            int childWidth = child.getWidth();
            if (childWidth > this.width) {
                this.width = childWidth;
            }
        }
        this.draggableBounds[2] = this.width;
        for (Window child : this.windows()) {
            child.setDimensions(this.width, child.getHeight());
        }
        return this.width;
    }

    public int getHeaderHeight() {
        return this.headerHeight;
    }

    public abstract void drawHeader(DrawContext context);

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if(click.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;
        if(click.y() >= this.headerHeight) return false;
        this.handleChildren = !this.handleChildren;
        return true;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        this.drawHeader(context);
    }
}
