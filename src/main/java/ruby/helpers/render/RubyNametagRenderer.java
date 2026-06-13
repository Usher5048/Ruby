package ruby.helpers.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.text.FontRenderer;
import ruby.systems.gui.windows.ModuleTypeWindow;

/** Ruby-styled entity nametag panels. */
public final class RubyNametagRenderer {

    private static final int SHADOW_COLOR = 0x44000000;

    private static final int PAD_X = 6;
    private static final int PAD_Y = 4;
    private static final int ACCENT_W = 2;
    private static final int ACCENT_GAP = 5;
    private static final int INNER_GAP = 6;
    private static final int BADGE_GAP = 4;
    private static final int BADGE_PAD_X = 6;
    private static final int BADGE_PAD_Y = 1;

    private RubyNametagRenderer() {}

    public record TagData(
            String name,
            Integer health,
            Integer pingMs,
            Integer distanceM,
            boolean friend
    ) {}

    public static void draw(
            DrawContext ctx,
            GUIStyle style,
            double centerX,
            double anchorY,
            double scale,
            TagData data,
            int nameColor,
            int panelBg,
            int panelBorder,
            boolean shadow,
            ItemStack heldItem
    ) {
        FontRenderer nameFont = style.bodyFont();
        FontRenderer badgeFont = style.monospaceFont();

        int badgeTextH = badgeFont.fontHeight;
        int badgeH = badgeTextH + BADGE_PAD_Y * 2;
        int nameH = nameFont.fontHeight;
        int contentH = Math.max(nameH, badgeH);
        int panelH = s(contentH + PAD_Y * 2, scale);

        int nameW = nameFont.getWidth(data.name());
        int cursorX = ACCENT_W + ACCENT_GAP + nameW;

        int healthW = 0;
        int pingW = 0;
        int distW = 0;
        String healthText = null;
        String pingText = null;
        String distText = null;

        if (data.health() != null) {
            healthText = Integer.toString(data.health());
            healthW = badgeWidth(badgeFont, healthText);
            cursorX += INNER_GAP + healthW;
        }
        if (data.pingMs() != null) {
            pingText = data.pingMs() + "ms";
            pingW = badgeWidth(badgeFont, pingText);
            cursorX += (healthW > 0 ? BADGE_GAP : INNER_GAP) + pingW;
        }
        if (data.distanceM() != null) {
            distText = data.distanceM() + "m";
            distW = badgeWidth(badgeFont, distText);
            cursorX += ((healthW > 0 || pingW > 0) ? BADGE_GAP : INNER_GAP) + distW;
        }

        int contentW = cursorX;
        int panelW = s(contentW + PAD_X * 2, scale);

        double itemSize = 16 * scale;
        double itemGap = heldItem != null && !heldItem.isEmpty() ? 3 * scale : 0;
        double totalW = panelW + itemGap + (itemGap > 0 ? itemSize : 0);

        double panelLeft = centerX - totalW / 2.0;
        double panelTop = anchorY - panelH;
        int px1 = (int) Math.round(panelLeft);
        int py1 = (int) Math.round(panelTop);
        int px2 = px1 + panelW;
        int py2 = py1 + panelH;

        float opacity = ((panelBg >> 24) & 0xFF) / 255f;
        int shadowColor = GUIStyle.withAlpha(SHADOW_COLOR, opacity);
        int radius = Math.max(1, s(GUIStyle.RADIUS_PILL, scale));

        ModuleTypeWindow.fillSmoothRoundedRect(ctx, px1 + s(1, scale), py1 + s(2, scale),
                px2 + s(1, scale), py2 + s(2, scale), radius, shadowColor);
        ModuleTypeWindow.drawRoundedPanel(ctx, px1, py1, px2, py2, radius, panelBg, panelBorder);

        int accentTop = py1 + s(PAD_Y, scale);
        int accentBottom = py2 - s(PAD_Y, scale);
        int accentX1 = px1 + s(PAD_X, scale);
        ctx.fill(accentX1, accentTop, accentX1 + s(ACCENT_W, scale), accentBottom,
                data.friend() ? style.ruby() : GUIStyle.withAlpha(style.ruby(), 0.65f));

        double textY = panelTop + s(PAD_Y, scale) + (s(contentH, scale) - s(nameH, scale)) / 2.0;
        double textX = panelLeft + s(PAD_X + ACCENT_W + ACCENT_GAP, scale);
        int resolvedNameColor = data.friend() ? style.ruby() : nameColor;
        drawText(ctx, nameFont, data.name(), textX, textY, resolvedNameColor, shadow, scale);

        double badgeX = panelLeft + s(PAD_X + ACCENT_W + ACCENT_GAP + nameW, scale);
        if (healthW > 0) {
            badgeX += s(INNER_GAP, scale);
            badgeX = drawBadge(ctx, style, badgeFont, healthText, badgeX, panelTop, panelH, contentH,
                    style.rubyBg(), style.ruby(), panelBorder, shadow, scale);
            badgeX += s(BADGE_GAP, scale);
        }
        if (pingW > 0) {
            if (healthW == 0) badgeX += s(INNER_GAP, scale);
            badgeX = drawBadge(ctx, style, badgeFont, pingText, badgeX, panelTop, panelH, contentH,
                    style.bgHover(), style.textMuted(), panelBorder, shadow, scale);
            badgeX += s(BADGE_GAP, scale);
        }
        if (distW > 0) {
            if (healthW == 0 && pingW == 0) badgeX += s(INNER_GAP, scale);
            drawBadge(ctx, style, badgeFont, distText, badgeX, panelTop, panelH, contentH,
                    style.bgHover(), style.textMuted(), panelBorder, shadow, scale);
        }

        if (heldItem != null && !heldItem.isEmpty()) {
            int itemX = (int) Math.round(panelLeft + panelW + itemGap);
            int itemY = (int) Math.round(panelTop + (panelH - itemSize) / 2.0);
            Matrix3x2fStack matrices = ctx.getMatrices();
            matrices.pushMatrix();
            matrices.translate(itemX, itemY);
            matrices.scale((float) scale, (float) scale);
            ctx.drawItemWithoutEntity(heldItem, 0, 0);
            matrices.popMatrix();
        }
    }

    private static double drawBadge(
            DrawContext ctx,
            GUIStyle style,
            FontRenderer font,
            String text,
            double x,
            double panelTop,
            int panelH,
            int contentH,
            int bg,
            int textColor,
            int border,
            boolean shadow,
            double scale
    ) {
        int badgeW = s(badgeWidth(font, text), scale);
        int badgeH = s(font.fontHeight + BADGE_PAD_Y * 2, scale);
        int bx1 = (int) Math.round(x);
        int by1 = (int) Math.round(panelTop + (panelH - badgeH) / 2.0);
        int bx2 = bx1 + badgeW;
        int by2 = by1 + badgeH;

        ModuleTypeWindow.drawRoundedBadge(ctx, bx1, by1, bx2, by2, bg, border);

        double textX = bx1 + s(BADGE_PAD_X, scale);
        double textY = panelTop + s(PAD_Y, scale) + (s(contentH, scale) - s(font.fontHeight, scale)) / 2.0;
        drawText(ctx, font, text, textX, textY, textColor, shadow, scale);
        return bx1 + badgeW;
    }

    private static int badgeWidth(FontRenderer font, String text) {
        return font.getWidth(text) + BADGE_PAD_X * 2;
    }

    private static void drawText(
            DrawContext ctx,
            FontRenderer font,
            String text,
            double x,
            double y,
            int color,
            boolean shadow,
            double scale
    ) {
        if (text.isEmpty()) return;

        x += 0.5 * scale;
        y += 0.5 * scale;

        Matrix3x2fStack matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.scale((float) scale, (float) scale);

        float drawX = (float) (x / scale);
        float drawY = (float) (y / scale);

        if (shadow) {
            font.draw(ctx, text, (int) drawX + 1, (int) drawY + 1, 0x80000000);
        }
        font.draw(ctx, text, (int) drawX, (int) drawY, color);

        matrices.popMatrix();
    }

    private static int s(int value, double scale) {
        return Math.max(1, (int) Math.round(value * scale));
    }
}
