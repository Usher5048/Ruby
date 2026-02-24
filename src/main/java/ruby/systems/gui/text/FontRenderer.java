package ruby.systems.gui.text;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontRenderer {
    private final GlyphSheet sheet;
    public final int fontHeight;

    private FontRenderer(GlyphSheet sheet) {
        this.sheet = sheet;
        this.fontHeight = this.sheet.getFontHeight();
    }

    public static FontRenderer create(InputStream stream, String identifier, int size) {
        char[] chrs = new char[127 - 32];
        for(int i = 0; i < chrs.length; i++) chrs[i] = (char) (i + 32);
        chrs[chrs.length - 2] = 0x00C6; // 'Æ'

        GlyphSheet sheet = null;
        try {
            sheet = new GlyphSheet(
                    identifier,
                    Font.createFont(Font.TRUETYPE_FONT, stream)
                            .deriveFont(Font.PLAIN, size)
            );
        } catch(IOException | FontFormatException ignored) {}
        if(sheet == null) return null;

        sheet.generateSheet(chrs);
        sheet.readyTexture();

        return new FontRenderer(sheet);
    }

    public void draw(DrawContext context, String text, int x, int y, int color) {
        for(char c : text.toCharArray()) {
            GlyphSheet.Glyph glyph = this.sheet.glyph(c);
            if(glyph == null) continue;

            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    this.sheet.getTexture(),
                    x, y,
                    glyph.u(), glyph.v(),
                    glyph.width(), glyph.height(),
                    glyph.width(), glyph.height(),
                    this.sheet.getTextureWidth(),
                    this.sheet.getTextureHeight(),
                    color
            );

            x += glyph.advanceX();
        }
    }

    public int getWidth(String text) {
        if(text == null) return 0;

        int width = 0;
        for(int i = 0; i < text.length(); i++) {
            GlyphSheet.Glyph g = this.sheet.glyph(text.charAt(i));
            if(g == null) continue;

            width += g.advanceX();
        }

        return width;
    }
}
