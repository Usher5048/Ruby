package ruby.systems.gui.text;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import ruby.RubyClient;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;

public class GlyphSheet {
    public record Glyph(
            int advanceX,
            int bearingX, int bearingY,
            int renderWidth, int renderHeight,
            float u, float v
    ) {}

    private static final int CELL_PAD = 2;

    private final HashMap<Character, Glyph> charMap = new HashMap<>();
    private final String identifier;
    private final Font font;

    private int imageWidth;
    private int imageHeight;
    private BufferedImage imageBuffer;
    private int fontHeight;

    public GlyphSheet(String identifier, Font font) {
        this.identifier = sanitizeIdentifier(identifier);
        this.font = font;
    }

    private static String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "font";
        }

        String normalized = identifier.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-';
            out.append(valid ? c : '_');
        }

        return out.toString();
    }

    protected void generateSheet(char[] chrs) {
        AffineTransform transform = new AffineTransform();
        FontRenderContext context = new FontRenderContext(transform, true, true);
        Graphics2D probeG = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        probeG.setFont(this.font);
        FontMetrics probe = probeG.getFontMetrics();
        probeG.dispose();
        int ascent = probe.getAscent();
        this.fontHeight = probe.getHeight();

        int maxCellW = 0;
        int maxCellH = 0;
        Rectangle[] pixelBounds = new Rectangle[chrs.length];

        for (int i = 0; i < chrs.length; i++) {
            GlyphVector gv = this.font.createGlyphVector(context, String.valueOf(chrs[i]));
            Rectangle pb = gv.getPixelBounds(context, GlyphSheet.CELL_PAD, GlyphSheet.CELL_PAD + ascent);
            if (pb.width <= 0 || pb.height <= 0) {
                pb = new Rectangle(GlyphSheet.CELL_PAD, GlyphSheet.CELL_PAD, Math.max(1, probe.charWidth(chrs[i])), this.fontHeight);
            }
            pixelBounds[i] = pb;
            maxCellW = Math.max(maxCellW, pb.x + pb.width + GlyphSheet.CELL_PAD);
            maxCellH = Math.max(maxCellH, pb.y + pb.height + GlyphSheet.CELL_PAD);
        }

        int glyphWidth = Math.max(1, maxCellW);
        int glyphHeight = Math.max(1, maxCellH);
        int charCount = chrs.length;

        int cols = (int) Math.ceil(Math.sqrt(charCount));
        int rows = (int) Math.ceil((float) charCount / cols);

        this.imageWidth = cols * glyphWidth;
        this.imageHeight = rows * glyphHeight;

        this.imageBuffer = new BufferedImage(this.imageWidth, this.imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = this.imageBuffer.createGraphics();
        g.setFont(this.font);
        FontMetrics metrics = g.getFontMetrics();

        for (int i = 0; i < chrs.length; i++) {
            int cellX = (i % cols) * glyphWidth;
            int cellY = (i / cols) * glyphHeight;

            BufferedImage cell = new BufferedImage(glyphWidth, glyphHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cellG = cell.createGraphics();
            cellG.setFont(this.font);
            cellG.setColor(new Color(0, 0, 0, 0));
            cellG.fillRect(0, 0, glyphWidth, glyphHeight);
            cellG.setColor(Color.WHITE);
            cellG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            cellG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cellG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            cellG.drawString(Character.toString(chrs[i]), GlyphSheet.CELL_PAD, GlyphSheet.CELL_PAD + metrics.getAscent());
            cellG.dispose();

            g.drawImage(cell, cellX, cellY, null);

            Rectangle pb = pixelBounds[i];
            int advance = Math.max(1, metrics.charWidth(chrs[i]));
            int bearingX = pb.x - GlyphSheet.CELL_PAD;
            int bearingY = pb.y - GlyphSheet.CELL_PAD;

            this.charMap.put(chrs[i], new Glyph(
                    advance, bearingX, bearingY,
                    pb.width, pb.height,
                    cellX + pb.x, cellY + pb.y
            ));
        }

        g.dispose();
    }

    protected void readyTexture() {
        NativeImage image = new NativeImage(this.imageWidth, this.imageHeight, true);
        for (int y = 0; y < this.imageHeight; y++) {
            for (int x = 0; x < this.imageWidth; x++) {
                image.setColor(x, y, this.imageBuffer.getRGB(x, y));
            }
        }

        RubyClient.client.getTextureManager().registerTexture(Identifier.of(
                RubyClient.MOD_ID,
                "fonts/" + this.identifier
        ), new NativeImageBackedTexture(() -> this.identifier, image));
    }

    protected Identifier getTexture() {
        return Identifier.of(RubyClient.MOD_ID, "fonts/" + this.identifier);
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
