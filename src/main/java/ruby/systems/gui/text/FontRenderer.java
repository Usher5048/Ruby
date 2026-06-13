package ruby.systems.gui.text;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.io.InputStream;

public class FontRenderer {
    public record TextBounds(int left, int top, int width, int height) {}

    private final GlyphSheet sheet;
    public final int fontHeight;

    private FontRenderer(GlyphSheet sheet) {
        this.sheet = sheet;
        this.fontHeight = this.sheet.getFontHeight();
    }

    public static FontRenderer create(InputStream stream, String identifier, int size) {
        return FontRenderer.create(stream, identifier, size, Font.PLAIN);
    }

    public static FontRenderer create(InputStream stream, String identifier, int size, int style) {
        char[] chrs = new char[0x1000];
        for (int i = 0; i < chrs.length; i++) chrs[i] = (char) i;

        GlyphSheet sheet = null;
        try {
            sheet = new GlyphSheet(
                    identifier,
                    Font.createFont(Font.TRUETYPE_FONT, stream)
                            .deriveFont(style, (float) size)
            );
        } catch (Exception ignored) {}
        if (sheet == null) return null;

        sheet.generateSheet(chrs);
        sheet.readyTexture();

        return new FontRenderer(sheet);
    }

    public void draw(DrawContext context, String text, int x, int y, int color) {
        if(text == null) return;
        for(char c : text.toCharArray()) {
            GlyphSheet.Glyph glyph = this.sheet.glyph(c);
            if(glyph == null) continue;

            int drawX = x + glyph.bearingX();
            int drawY = y + glyph.bearingY();

            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    this.sheet.getTexture(),
                    drawX, drawY,
                    glyph.u(), glyph.v(),
                    glyph.renderWidth(), glyph.renderHeight(),
                    glyph.renderWidth(), glyph.renderHeight(),
                    this.sheet.getTextureWidth(),
                    this.sheet.getTextureHeight(),
                    color
            );

            x += glyph.advanceX();
        }
    }

    public int getWidth(String text) {
        return this.measure(text).width();
    }

    public TextBounds measure(String text) {
        if(text == null || text.isEmpty())
            return new TextBounds(0, 0, 0, 0);

        int penX = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for(int i = 0; i < text.length(); i++) {
            GlyphSheet.Glyph glyph = this.sheet.glyph(text.charAt(i));
            if (glyph == null) continue;

            int gx = penX + glyph.bearingX();
            int gy = glyph.bearingY();
            minX = Math.min(minX, gx);
            minY = Math.min(minY, gy);
            maxX = Math.max(maxX, gx + glyph.renderWidth());
            maxY = Math.max(maxY, gy + glyph.renderHeight());
            penX += glyph.advanceX();
        }

        if(minX == Integer.MAX_VALUE)
            return new TextBounds(0, 0, 0, 0);

        return new TextBounds(minX, minY, maxX - minX, maxY - minY);
    }
}
