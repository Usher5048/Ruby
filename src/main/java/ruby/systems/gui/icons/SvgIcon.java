package ruby.systems.gui.icons;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** A Lucide-style SVG: stroked (and occasionally filled) shapes in a square view box. */
public final class SvgIcon {
    private static final int SUPERSAMPLE = 4;

    private final double viewX;
    private final double viewY;
    private final double viewWidth;
    private final double viewHeight;
    private final List<Layer> layers;

    private SvgIcon(double viewX, double viewY, double viewWidth, double viewHeight, List<Layer> layers) {
        this.viewX = viewX;
        this.viewY = viewY;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.layers = layers;
    }

    public static SvgIcon parse(String svg) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)))
                    .getDocumentElement();

            double[] view = SvgIcon.viewBox(root);
            Style style = Style.from(root, Style.ROOT);
            List<Layer> layers = new ArrayList<>();
            SvgIcon.collect(root, style, layers);
            return new SvgIcon(view[0], view[1], view[2], view[3], layers);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse SVG", e);
        }
    }

    /** True when a stroke is a fixed color and must not pick up the blit tint. */
    public boolean hasFixedStroke() {
        for (Layer layer : this.layers) {
            if (layer.stroke && layer.strokeFixed) return true;
        }
        return false;
    }

    /** White icon with coverage in the alpha channel, so a blit tint supplies the color. */
    public BufferedImage raster(int size) {
        return this.raster(size, false);
    }

    /** Fixed-color stroke only, also stored as white coverage. Blit it untinted. */
    public BufferedImage rasterFixedStroke(int size) {
        return this.raster(size, true);
    }

    private BufferedImage raster(int size, boolean fixedStroke) {
        if (size <= 0) throw new IllegalArgumentException("size");
        int big = size * SvgIcon.SUPERSAMPLE;
        BufferedImage raster = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = raster.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double fit = big / Math.max(this.viewWidth, this.viewHeight);
        double ox = (big - this.viewWidth * fit) / 2 - this.viewX * fit;
        double oy = (big - this.viewHeight * fit) / 2 - this.viewY * fit;
        AffineTransform transform = new AffineTransform();
        transform.translate(ox, oy);
        transform.scale(fit, fit);

        for (Layer layer : this.layers) {
            var shape = transform.createTransformedShape(layer.path);
            if (!fixedStroke && layer.fill) {
                g.setColor(Color.WHITE);
                g.fill(shape);
            }
            boolean drawStroke = layer.stroke && layer.strokeWidth > 0 && layer.strokeFixed == fixedStroke;
            if (drawStroke) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(
                        (float) (layer.strokeWidth * fit),
                        layer.cap,
                        layer.join,
                        4f
                ));
                g.draw(shape);
            }
        }
        g.dispose();
        return SvgIcon.downsample(raster, size);
    }

    private static BufferedImage downsample(BufferedImage source, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int block = SvgIcon.SUPERSAMPLE;
        int area = block * block;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int alpha = 0;
                for (int by = 0; by < block; by++) {
                    for (int bx = 0; bx < block; bx++) {
                        alpha += source.getRGB(x * block + bx, y * block + by) >>> 24;
                    }
                }
                alpha /= area;
                out.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        return out;
    }

    private static void collect(Element parent, Style inherited, List<Layer> layers) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element element = (Element) node;
            Style style = Style.from(element, inherited);
            switch (element.getTagName()) {
                case "g" -> SvgIcon.collect(element, style, layers);
                case "path" -> layers.add(new Layer(SvgPath.parse(element.getAttribute("d")), style));
                case "line" -> layers.add(new Layer(SvgIcon.line(element), style));
                case "polyline" -> layers.add(new Layer(SvgIcon.poly(element.getAttribute("points"), false), style));
                case "polygon" -> layers.add(new Layer(SvgIcon.poly(element.getAttribute("points"), true), style));
                case "circle" -> layers.add(new Layer(SvgIcon.ellipse(
                        SvgIcon.attr(element, "cx"), SvgIcon.attr(element, "cy"),
                        SvgIcon.attr(element, "r"), SvgIcon.attr(element, "r")
                ), style));
                case "ellipse" -> layers.add(new Layer(SvgIcon.ellipse(
                        SvgIcon.attr(element, "cx"), SvgIcon.attr(element, "cy"),
                        SvgIcon.attr(element, "rx"), SvgIcon.attr(element, "ry")
                ), style));
                case "rect" -> layers.add(new Layer(SvgIcon.rect(element), style));
                default -> {}
            }
        }
    }

    private static Path2D line(Element element) {
        Path2D path = new Path2D.Double();
        path.moveTo(SvgIcon.attr(element, "x1"), SvgIcon.attr(element, "y1"));
        path.lineTo(SvgIcon.attr(element, "x2"), SvgIcon.attr(element, "y2"));
        return path;
    }

    private static Path2D poly(String points, boolean close) {
        Path2D path = new Path2D.Double();
        double[] coords = SvgIcon.numbers(points);
        for (int i = 0; i + 1 < coords.length; i += 2) {
            if (i == 0) path.moveTo(coords[i], coords[i + 1]);
            else path.lineTo(coords[i], coords[i + 1]);
        }
        if (close) path.closePath();
        return path;
    }

    private static Path2D ellipse(double cx, double cy, double rx, double ry) {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2), false);
        return path;
    }

    private static Path2D rect(Element element) {
        double x = SvgIcon.attr(element, "x");
        double y = SvgIcon.attr(element, "y");
        double w = SvgIcon.attr(element, "width");
        double h = SvgIcon.attr(element, "height");
        double rx = SvgIcon.attr(element, "rx");
        double ry = SvgIcon.attr(element, "ry");
        if (rx == 0 && ry == 0) {
            Path2D path = new Path2D.Double();
            path.moveTo(x, y);
            path.lineTo(x + w, y);
            path.lineTo(x + w, y + h);
            path.lineTo(x, y + h);
            path.closePath();
            return path;
        }
        if (rx == 0) rx = ry;
        if (ry == 0) ry = rx;
        Path2D path = new Path2D.Double();
        path.append(new RoundRectangle2D.Double(x, y, w, h, rx * 2, ry * 2), false);
        return path;
    }

    private static double[] viewBox(Element root) {
        String view = root.getAttribute("viewBox");
        if (view != null && !view.isBlank()) {
            double[] values = SvgIcon.numbers(view);
            if (values.length >= 4 && values[2] > 0 && values[3] > 0) {
                return new double[] {values[0], values[1], values[2], values[3]};
            }
        }
        double width = SvgIcon.attr(root, "width");
        double height = SvgIcon.attr(root, "height");
        if (width <= 0) width = 24;
        if (height <= 0) height = 24;
        return new double[] {0, 0, width, height};
    }

    private static double attr(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) return 0;
        return Double.parseDouble(value);
    }

    private static double[] numbers(String text) {
        List<Double> values = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ',' || c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                i++;
                continue;
            }
            int start = i;
            if (c == '+' || c == '-') i++;
            boolean digits = false;
            while (i < text.length()) {
                char d = text.charAt(i);
                if (d < '0' || d > '9') break;
                digits = true;
                i++;
            }
            if (i < text.length() && text.charAt(i) == '.') {
                i++;
                while (i < text.length()) {
                    char d = text.charAt(i);
                    if (d < '0' || d > '9') break;
                    digits = true;
                    i++;
                }
            }
            if (!digits) break;
            values.add(Double.parseDouble(text.substring(start, i)));
        }
        double[] out = new double[values.size()];
        for (int n = 0; n < values.size(); n++) out[n] = values.get(n);
        return out;
    }

    private record Layer(Path2D path, boolean fill, boolean stroke, boolean strokeFixed, float strokeWidth, int cap, int join) {
        Layer(Path2D path, Style style) {
            this(path, style.fill, style.stroke, style.strokeFixed, style.strokeWidth, style.cap, style.join);
        }
    }

    private record Style(boolean fill, boolean stroke, boolean strokeFixed, float strokeWidth, int cap, int join) {
        static final Style ROOT = new Style(false, true, false, 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        static Style from(Element element, Style inherited) {
            boolean fill = SvgIcon.paint(element.getAttribute("fill"), inherited.fill);
            String strokeAttr = element.getAttribute("stroke");
            boolean stroke;
            boolean strokeFixed;
            if (strokeAttr == null || strokeAttr.isBlank()) {
                stroke = inherited.stroke;
                strokeFixed = inherited.strokeFixed;
            } else if (strokeAttr.equals("none")) {
                stroke = false;
                strokeFixed = false;
            } else if (SvgIcon.fixedWhite(strokeAttr)) {
                stroke = true;
                strokeFixed = true;
            } else {
                stroke = true;
                strokeFixed = false;
            }
            float width = inherited.strokeWidth;
            String widthAttr = element.getAttribute("stroke-width");
            if (widthAttr != null && !widthAttr.isBlank()) width = Float.parseFloat(widthAttr);
            return new Style(fill, stroke, strokeFixed, width, SvgIcon.cap(element.getAttribute("stroke-linecap"), inherited.cap),
                    SvgIcon.join(element.getAttribute("stroke-linejoin"), inherited.join));
        }
    }

    private static boolean fixedWhite(String value) {
        return switch (value) {
            case "white", "#fff", "#FFF", "#ffffff", "#FFFFFF" -> true;
            default -> false;
        };
    }

    private static boolean paint(String value, boolean inherited) {
        if (value == null || value.isBlank()) return inherited;
        return !value.equals("none");
    }

    private static int cap(String value, int inherited) {
        return switch (value) {
            case "butt" -> BasicStroke.CAP_BUTT;
            case "square" -> BasicStroke.CAP_SQUARE;
            case "round" -> BasicStroke.CAP_ROUND;
            default -> inherited;
        };
    }

    private static int join(String value, int inherited) {
        return switch (value) {
            case "miter" -> BasicStroke.JOIN_MITER;
            case "bevel" -> BasicStroke.JOIN_BEVEL;
            case "round" -> BasicStroke.JOIN_ROUND;
            default -> inherited;
        };
    }
}
