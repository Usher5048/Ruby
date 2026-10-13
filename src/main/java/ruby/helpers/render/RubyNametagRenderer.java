package ruby.helpers.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.text.FontRenderer;
import ruby.systems.gui.windows.ModuleTypeWindow;

/** One nametag plate, drawn with the same panel as the rest of the interface. */
public final class RubyNametagRenderer {

    private static final int PAD_X = 8;
    private static final int PAD_Y = 5;
    private static final int ITEM = 16;
    private static final int GAP = 6;
    private static final int TAB_PAD_X = 6;
    private static final int TAB_PAD_Y = 1;
    private static final int SHOULDER = 8;
    private static final int STAT_GAP = 5;

    private RubyNametagRenderer() {}

    public record TagData(
            String name,
            Integer health,
            Integer pingMs,
            Integer distanceM,
            boolean friend
    ) {}

    public static void draw(
            GuiGraphicsExtractor ctx,
            GUIStyle style,
            double centerX,
            double anchorY,
            double scale,
            TagData data,
            int nameColor,
            int panelBg,
            int panelBorder,
            ItemStack heldItem
    ) {
        // One font pixel is one screen pixel at scale 1. Stretching up to GUI size made the plate huge and soft.
        // Below about half size the 13px glyphs stop reading, even when filtered.
        float factor = (float) Math.clamp(scale, 0.5, 3);
        float originX = Math.round((float) centerX);
        float originY = Math.round((float) anchorY);
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(originX, originY);
        if (Math.abs(factor - 1f) > 0.001f) pose.scale(factor, factor);
        try {
            paint(ctx, style, 0, 0, data, nameColor, panelBg, panelBorder, heldItem);
        } finally {
            pose.popMatrix();
        }
    }

    private static void paint(
            GuiGraphicsExtractor ctx,
            GUIStyle style,
            double centerX,
            double anchorY,
            TagData data,
            int nameColor,
            int panelBg,
            int panelBorder,
            ItemStack heldItem
    ) {
        FontRenderer nameFont = style.bodyFont();
        FontRenderer metaFont = style.labelFont();
        boolean item = heldItem != null && !heldItem.isEmpty();
        String health = data.health() == null ? null : Integer.toString(data.health());
        String ping = data.pingMs() == null ? null : data.pingMs() + "ms";
        String distance = data.distanceM() == null ? null : data.distanceM() + "m";

        FontRenderer.TextBounds nameBounds = nameFont.measure(data.name());
        int nameW = nameBounds.width();
        int contentH = nameFont.fontHeight;
        if (item) contentH = Math.max(contentH, ITEM);

        int innerW = nameW;
        if (item) innerW += ITEM + GAP;
        int panelW = PAD_X * 2 + innerW;

        int stats = 0;
        int statsW = 0;
        if (health != null) {
            stats++;
            statsW += metaFont.getWidth(health);
        }
        if (ping != null) {
            stats++;
            statsW += metaFont.getWidth(ping);
        }
        if (distance != null) {
            stats++;
            statsW += metaFont.getWidth(distance);
        }
        if (stats > 1) statsW += STAT_GAP * (stats - 1);
        int tabW = stats == 0 ? 0 : statsW + TAB_PAD_X * 2;
        if (tabW > 0) panelW = Math.max(panelW, tabW + SHOULDER * 2);

        int panelH = PAD_Y * 2 + contentH;
        int left = (int) Math.round(centerX - panelW / 2.0);
        int top = (int) Math.round(anchorY - panelH);

        ModuleTypeWindow.drawRoundedPanel(ctx, left, top, left + panelW, top + panelH, GUIStyle.RADIUS_ROW, panelBg, panelBorder);

        int itemX = left + PAD_X;
        if (item) {
            int itemY = top + (panelH - ITEM) / 2;
            ctx.item(heldItem, itemX, itemY);
        }

        int nameAreaL = item ? itemX + ITEM + GAP : left;
        int nameAreaR = left + panelW;
        int nameX = nameAreaL + (nameAreaR - nameAreaL - nameW) / 2 - nameBounds.left();
        if (item) nameX = Math.max(nameX, itemX + ITEM + GAP - nameBounds.left());

        int resolvedName = data.friend() ? style.ruby() : nameColor;
        nameFont.draw(ctx, data.name(), nameX, top + (panelH - nameFont.fontHeight) / 2, resolvedName);

        if (stats == 0) return;

        int tabX = left + (panelW - tabW) / 2;
        int tabH = metaFont.fontHeight + TAB_PAD_Y * 2;
        int tabTop = top - tabH;
        int radius = Math.min(GUIStyle.RADIUS_BADGE, tabW / 2);
        ModuleTypeWindow.fillTopRoundedRect(ctx, tabX, tabTop, tabX + tabW, top + 1, radius, panelBorder);
        ModuleTypeWindow.fillTopRoundedRect(ctx, tabX + 1, tabTop + 1, tabX + tabW - 1, top + 1, Math.max(0, radius - 1), panelBg);

        int textY = tabTop + TAB_PAD_Y;
        int cursor = tabX + TAB_PAD_X;
        int rule = GUIStyle.withAlpha(style.textMuted(), 0.45f);
        int muted = style.textMuted();
        if (health != null) {
            int healthColor = data.health() <= 8 ? style.ruby() : style.text();
            metaFont.draw(ctx, health, cursor, textY, healthColor);
            cursor += metaFont.getWidth(health);
            if (ping != null || distance != null) cursor = tick(ctx, cursor, tabTop, top, rule);
        }
        if (ping != null) {
            metaFont.draw(ctx, ping, cursor, textY, muted);
            cursor += metaFont.getWidth(ping);
            if (distance != null) cursor = tick(ctx, cursor, tabTop, top, rule);
        }
        if (distance != null) metaFont.draw(ctx, distance, cursor, textY, muted);
    }

    /** A short divider between stats inside the folder tab. */
    private static int tick(GuiGraphicsExtractor ctx, int x, int tabTop, int panelTop, int color) {
        int gap = (STAT_GAP - 1) / 2;
        int y1 = tabTop + 3;
        int y2 = panelTop - 2;
        ctx.fill(x + gap, y1, x + gap + 1, y2, color);
        return x + STAT_GAP;
    }
}
