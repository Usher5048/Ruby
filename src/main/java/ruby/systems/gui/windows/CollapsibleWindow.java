package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.systems.gui.Motion;
import ruby.systems.gui.GuiTime;

public abstract class CollapsibleWindow extends Window {
    private final int headerHeight;
    private final int minWidth;

    protected boolean expanded = true;
    private float expandProgress = 0;
    private boolean heightCached;
    private int cachedHeight;
    private float cachedProgress = Float.NaN;
    private int cachedHeader = Integer.MIN_VALUE;
    private int cachedStamp;

    public CollapsibleWindow(int x, int y, int minWidth, int headerHeight) {
        super(x, y, minWidth, headerHeight);
        this.headerHeight = headerHeight;
        this.minWidth = minWidth;
        this.draggableBounds = new int[] {0, 0, minWidth, headerHeight};
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
        int header = this.getHeaderHeight();
        if(this.expandProgress <= 0f) {
            if(this.heightCached && this.cachedProgress == 0f && this.cachedHeader == header) return this.cachedHeight;
            this.rememberHeight(header, 0f, header, 0);
            return header;
        }

        if(this.heightCached && this.cachedProgress == this.expandProgress && this.cachedHeader == header) {
            int stamp = this.contentStamp();
            if(stamp == this.cachedStamp) return this.cachedHeight;
        }

        int height;
        if(this.expandProgress >= 1f) {
            height = this.getExpandedHeight();
        } else {
            int expanded = this.getExpandedHeight();
            float eased = Motion.standard(this.expandProgress);
            height = header + (int) ((expanded - header) * eased);
        }
        this.rememberHeight(height, this.expandProgress, header, this.contentStamp());
        return height;
    }

    private void rememberHeight(int height, float progress, int header, int stamp) {
        this.heightCached = true;
        this.cachedHeight = height;
        this.cachedProgress = progress;
        this.cachedHeader = header;
        this.cachedStamp = stamp;
    }

    private int contentStamp() {
        int stamp = 1;
        var children = this.windows();
        for(int i = 0, n = children.size(); i < n; i++) {
            stamp = stamp * 31 + children.get(i).layoutStamp();
        }
        return stamp;
    }

    public void updateAnimation(float delta) {
        float step = GuiTime.toSeconds(delta) / Motion.EXPAND;
        this.expandProgress = Math.clamp(
                this.expandProgress + (this.expanded ? step : -step),
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

    public abstract void drawHeader(GuiGraphicsExtractor context);

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if(click.button() != InputConstants.MOUSE_BUTTON_RIGHT) return false;
        if(click.y() >= this.headerHeight) return false;
        this.expanded = !this.expanded;
        return true;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        this.drawHeader(context);

        this.handleChildren = this.expandProgress > 0;
        float step = ruby.systems.gui.ClientSettings.expandStep(dt);
        this.expandProgress = Math.clamp(this.expandProgress + (this.expanded ? step : -step), 0, 1);

        this.enableCutout(0, 0, this.getWidth(), this.getHeight());
    }
}
