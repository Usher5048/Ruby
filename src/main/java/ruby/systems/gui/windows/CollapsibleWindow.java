package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public abstract class CollapsibleWindow extends Window {
    private final int headerHeight;
    private final int minWidth;

    protected boolean expanded = true;
    private float expandProgress = 0;

    public CollapsibleWindow(int x, int y, int minWidth, int headerHeight) {
        super(x, y, minWidth, headerHeight);
        this.headerHeight = headerHeight;
        this.minWidth = minWidth;
        this.draggableBounds = new int[] {0, 0, minWidth, headerHeight};
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2 * t + 2, 3) / 2f;
    }

    public float expandProgress() {
        return this.expandProgress;
    }

    protected int getExpandedHeight() {
        this.height = this.headerHeight;
        for (Window child : this.windows()) {
            this.height += child.getHeight();
        }
        return this.height;
    }

    @Override
    public int getHeight() {
        if(this.expandProgress <= 0f) return this.getHeaderHeight();
        if(this.expandProgress >= 1f) return this.getExpandedHeight();
        int expanded = this.getExpandedHeight();
        float eased = easeInOutCubic(this.expandProgress);
        return this.getHeaderHeight() + (int) ((expanded - this.getHeaderHeight()) * eased);
    }

    public void updateAnimation(float delta) {
        this.expandProgress = Math.clamp(
                this.expandProgress + (this.expanded ? delta : -delta) * 0.02f,
                0, 1
        );
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
        this.expanded = !this.expanded;
        return true;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        this.drawHeader(context);

        this.handleChildren = this.expandProgress > 0;
        this.expandProgress = Math.clamp(this.expandProgress + (
                this.expanded ? dt : -dt
        ) * 0.2f, 0, 1);

        this.enableCutout(0, 0, this.getWidth(), this.getHeight());
    }
}
