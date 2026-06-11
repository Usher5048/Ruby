package ruby.systems.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

/**
 * Anchor-based HUD element bounds, matching Meteor Client's {@code HudBox}.
 *
 * @see <a href="https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/hud/HudBox.java">HudBox.java</a>
 */
public class HudBox {
    public HudAnchor.X xAnchor = HudAnchor.X.Right;
    public HudAnchor.Y yAnchor = HudAnchor.Y.Top;

    public int x;
    public int y;
    private int width;
    private int height;

    public void setSize(double width, double height) {
        if (width >= 0) this.width = (int) Math.ceil(width);
        if (height >= 0) this.height = (int) Math.ceil(height);
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getRenderX() {
        return this.getRenderX(null);
    }

    public int getRenderY() {
        return this.getRenderY(null);
    }

    public int getRenderX(DrawContext context) {
        Window window = MinecraftClient.getInstance().getWindow();
        int windowWidth = window.getWidth();
        int offset = this.scaledOffset(this.x, window);
        return switch (this.xAnchor) {
            case Left -> offset;
            case Center -> windowWidth / 2 - this.width / 2 + offset;
            case Right -> windowWidth - this.width + offset;
        };
    }

    public int getRenderY(DrawContext context) {
        Window window = MinecraftClient.getInstance().getWindow();
        int windowHeight = window.getHeight();
        int offset = this.scaledOffset(this.y, window);
        return switch (this.yAnchor) {
            case Top -> offset;
            case Center -> windowHeight / 2 - this.height / 2 + offset;
            case Bottom -> windowHeight - this.height + offset;
        };
    }

    /**
     * Fabric HUD layers draw in framebuffer pixels (see {@link ruby.systems.modules.render.Hud}),
     * while offsets are configured in GUI-scaled units.
     */
    private int scaledOffset(int offset, Window window) {
        return (int) Math.round(offset * window.getScaleFactor());
    }

    /**
     * Horizontal alignment of a row within the element, using the box anchor when {@code Auto}.
     */
    public double alignX(double rowWidth, HudAlignment alignment) {
        HudAnchor.X anchor = this.xAnchor;

        if (alignment == HudAlignment.Left) anchor = HudAnchor.X.Left;
        else if (alignment == HudAlignment.Center) anchor = HudAnchor.X.Center;
        else if (alignment == HudAlignment.Right) anchor = HudAnchor.X.Right;

        return switch (anchor) {
            case Left -> 0;
            case Center -> this.width / 2.0 - rowWidth / 2.0;
            case Right -> this.width - rowWidth;
        };
    }
}
