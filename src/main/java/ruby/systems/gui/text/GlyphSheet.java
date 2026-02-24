package ruby.systems.gui.text;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import ruby.RubyClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

public class GlyphSheet {
    public record Glyph(
            int advanceX, int ascent,
            int width, int height,
            float u, float v
    ) {}

    private final HashMap<Character, Glyph> charMap = new HashMap<>();
    private final String identifier;
    private final Font font;

    private int imageWidth;
    private int imageHeight;
    private BufferedImage imageBuffer;
    private int fontHeight;

    public GlyphSheet(String identifier, Font font) {
        this.identifier = identifier;
        this.font = font;
    }

    protected void generateSheet(char[] chrs) {
        double maxWidth = -1;
        double maxHeight = -1;

        AffineTransform transform = new AffineTransform();
        FontRenderContext context = new FontRenderContext(transform, true, true);

        for(char c : chrs) {
            Rectangle2D bounds = this.font.getStringBounds(Character.toString(c), context);
            if(maxWidth < bounds.getWidth()) maxWidth = bounds.getWidth();
            if(maxHeight < bounds.getHeight()) maxHeight = bounds.getHeight();
        }

        maxWidth++;
        maxHeight++;

        int glyphWidth  = (int) maxWidth;
        int glyphHeight = (int) maxHeight;
        int charCount = chrs.length;

        int cols = (int) Math.ceil(Math.sqrt(charCount));
        int rows = (int) Math.ceil((float) charCount / cols);

        this.imageWidth  = cols * glyphWidth;
        this.imageHeight = rows * glyphHeight;

        this.imageBuffer = new BufferedImage(this.imageWidth, this.imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = this.imageBuffer.createGraphics();
        g.setFont(this.font);

        FontMetrics metrics = g.getFontMetrics();
        this.fontHeight = metrics.getHeight();
        for(int i = 0; i < chrs.length; i++) {
            int x = (i % cols) * glyphWidth;
            int y = (i / cols) * glyphHeight;

            BufferedImage cell = new BufferedImage(glyphWidth, glyphHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cellG = cell.createGraphics();

            cellG.setFont(this.font);

            cellG.setColor(new Color(0, 0, 0, 0));
            cellG.fillRect(0, 0, glyphWidth, glyphHeight);

            cellG.setColor(Color.WHITE);

            cellG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            cellG.setRenderingHint(RenderingHints.KEY_ANTIALIASING     , RenderingHints.VALUE_ANTIALIAS_OFF        );
            cellG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON    );

            cellG.drawString(Character.toString(chrs[i]), 0, metrics.getAscent());
            cellG.dispose();

            g.drawImage(cell, x, y, null);

            Glyph glyph = new Glyph(
                    metrics.charWidth(chrs[i]), metrics.getAscent(),
                    glyphWidth, glyphHeight,
                    x, y
            );

            this.charMap.put(chrs[i], glyph);
        }

        g.dispose();
    }

    protected void readyTexture() {
        NativeImage image = new NativeImage(this.imageWidth, this.imageHeight, true);
        for(int y = 0; y < this.imageHeight; y++) {
            for(int x = 0; x < this.imageWidth; x++) {
                int color = this.imageBuffer.getRGB(x, y);
                image.setColor(x, y, color);
            }
        }

        try {
            ImageIO.write(this.imageBuffer, "png", new File("test_texture_" + this.identifier + ".png"));
        } catch(Exception ignored) {}

        RubyClient.client.getTextureManager().registerTexture(Identifier.of(
                RubyClient.MOD_ID,
                "fonts/" + this.identifier
        ), new NativeImageBackedTexture(() -> this.identifier, image));
    }

    protected Identifier getTexture() {
        return Identifier.of(
                RubyClient.MOD_ID,
                "fonts/" + this.identifier
        );
    }

    protected int getTextureWidth() {
        return this.imageWidth;
    }
    protected int getTextureHeight() {
        return this.imageHeight;
    }

    protected int getFontHeight() {
        return this.fontHeight;
    }
    protected Glyph glyph(char c) {
        return this.charMap.get(c);
    }
}
