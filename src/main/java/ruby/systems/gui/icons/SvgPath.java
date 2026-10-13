package ruby.systems.gui.icons;

import java.awt.geom.Path2D;

/** SVG path `d` parser. Covers the commands Lucide icons use, including arcs. */
final class SvgPath {
    private final String data;
    private int index;
    private double cx;
    private double cy;
    private double startX;
    private double startY;
    private double lastCubicX;
    private double lastCubicY;
    private double lastQuadX;
    private double lastQuadY;
    private char command;

    private SvgPath(String data) {
        this.data = data;
    }

    static Path2D parse(String data) {
        SvgPath parser = new SvgPath(data == null ? "" : data);
        Path2D path = new Path2D.Double();
        parser.parseInto(path);
        return path;
    }

    private void parseInto(Path2D path) {
        this.skipSeparators();
        while (this.index < this.data.length()) {
            int mark = this.index;
            char c = this.data.charAt(this.index);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                this.command = c;
                this.index++;
            }
            switch (this.command) {
                case 'M', 'm' -> this.move(path, this.command == 'm');
                case 'L', 'l' -> this.line(path, this.command == 'l');
                case 'H', 'h' -> this.horizontal(path, this.command == 'h');
                case 'V', 'v' -> this.vertical(path, this.command == 'v');
                case 'C', 'c' -> this.cubic(path, this.command == 'c');
                case 'S', 's' -> this.smoothCubic(path, this.command == 's');
                case 'Q', 'q' -> this.quad(path, this.command == 'q');
                case 'T', 't' -> this.smoothQuad(path, this.command == 't');
                case 'A', 'a' -> this.arc(path, this.command == 'a');
                case 'Z', 'z' -> this.close(path);
                default -> throw new IllegalArgumentException("Unknown SVG path command: " + this.command);
            }
            if (this.index == mark) {
                throw new IllegalArgumentException("SVG path stalled at " + mark);
            }
            this.skipSeparators();
        }
    }

    private void move(Path2D path, boolean relative) {
        boolean first = true;
        while (this.hasNumber()) {
            double x = this.number();
            double y = this.number();
            if (relative) {
                x += this.cx;
                y += this.cy;
            }
            if (first) {
                path.moveTo(x, y);
                this.startX = x;
                this.startY = y;
                first = false;
            } else {
                path.lineTo(x, y);
            }
            this.setPoint(x, y);
            this.clearControls();
        }
    }

    private void line(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double x = this.number();
            double y = this.number();
            if (relative) {
                x += this.cx;
                y += this.cy;
            }
            path.lineTo(x, y);
            this.setPoint(x, y);
            this.clearControls();
        }
    }

    private void horizontal(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double x = this.number();
            if (relative) x += this.cx;
            path.lineTo(x, this.cy);
            this.setPoint(x, this.cy);
            this.clearControls();
        }
    }

    private void vertical(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double y = this.number();
            if (relative) y += this.cy;
            path.lineTo(this.cx, y);
            this.setPoint(this.cx, y);
            this.clearControls();
        }
    }

    private void cubic(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double x1 = this.number();
            double y1 = this.number();
            double x2 = this.number();
            double y2 = this.number();
            double x = this.number();
            double y = this.number();
            if (relative) {
                x1 += this.cx;
                y1 += this.cy;
                x2 += this.cx;
                y2 += this.cy;
                x += this.cx;
                y += this.cy;
            }
            path.curveTo(x1, y1, x2, y2, x, y);
            this.lastCubicX = x2;
            this.lastCubicY = y2;
            this.lastQuadX = x;
            this.lastQuadY = y;
            this.setPoint(x, y);
        }
    }

    private void smoothCubic(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double x1 = this.reflectedCubicX();
            double y1 = this.reflectedCubicY();
            double x2 = this.number();
            double y2 = this.number();
            double x = this.number();
            double y = this.number();
            if (relative) {
                x2 += this.cx;
                y2 += this.cy;
                x += this.cx;
                y += this.cy;
            }
            path.curveTo(x1, y1, x2, y2, x, y);
            this.lastCubicX = x2;
            this.lastCubicY = y2;
            this.lastQuadX = x;
            this.lastQuadY = y;
            this.setPoint(x, y);
        }
    }

    private void quad(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double x1 = this.number();
            double y1 = this.number();
            double x = this.number();
            double y = this.number();
            if (relative) {
                x1 += this.cx;
                y1 += this.cy;
                x += this.cx;
                y += this.cy;
            }
            path.quadTo(x1, y1, x, y);
            this.lastQuadX = x1;
            this.lastQuadY = y1;
            this.lastCubicX = x;
            this.lastCubicY = y;
            this.setPoint(x, y);
        }
    }

    private void smoothQuad(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double x1 = this.reflectedQuadX();
            double y1 = this.reflectedQuadY();
            double x = this.number();
            double y = this.number();
            if (relative) {
                x += this.cx;
                y += this.cy;
            }
            path.quadTo(x1, y1, x, y);
            this.lastQuadX = x1;
            this.lastQuadY = y1;
            this.lastCubicX = x;
            this.lastCubicY = y;
            this.setPoint(x, y);
        }
    }

    private void arc(Path2D path, boolean relative) {
        while (this.hasNumber()) {
            double rx = this.number();
            double ry = this.number();
            double rotation = this.number();
            boolean large = this.flag() == 1;
            boolean sweep = this.flag() == 1;
            double x = this.number();
            double y = this.number();
            if (relative) {
                x += this.cx;
                y += this.cy;
            }
            this.arcTo(path, this.cx, this.cy, rx, ry, rotation, large, sweep, x, y);
            this.setPoint(x, y);
            this.clearControls();
        }
    }

    private void close(Path2D path) {
        path.closePath();
        this.setPoint(this.startX, this.startY);
        this.clearControls();
    }

    private void setPoint(double x, double y) {
        this.cx = x;
        this.cy = y;
    }

    private void clearControls() {
        this.lastCubicX = this.cx;
        this.lastCubicY = this.cy;
        this.lastQuadX = this.cx;
        this.lastQuadY = this.cy;
    }

    private double reflectedCubicX() {
        return this.commandReflectsCubic() ? 2 * this.cx - this.lastCubicX : this.cx;
    }

    private double reflectedCubicY() {
        return this.commandReflectsCubic() ? 2 * this.cy - this.lastCubicY : this.cy;
    }

    private double reflectedQuadX() {
        return this.commandReflectsQuad() ? 2 * this.cx - this.lastQuadX : this.cx;
    }

    private double reflectedQuadY() {
        return this.commandReflectsQuad() ? 2 * this.cy - this.lastQuadY : this.cy;
    }

    private boolean commandReflectsCubic() {
        return this.command == 'C' || this.command == 'c' || this.command == 'S' || this.command == 's';
    }

    private boolean commandReflectsQuad() {
        return this.command == 'Q' || this.command == 'q' || this.command == 'T' || this.command == 't';
    }

    private void arcTo(
            Path2D path, double x1, double y1, double rx, double ry, double rotationDeg,
            boolean large, boolean sweep, double x2, double y2
    ) {
        if (x1 == x2 && y1 == y2) return;
        if (rx == 0 || ry == 0) {
            path.lineTo(x2, y2);
            return;
        }

        double phi = Math.toRadians(rotationDeg % 360);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        double dx = (x1 - x2) / 2;
        double dy = (y1 - y2) / 2;
        double x1p = cos * dx + sin * dy;
        double y1p = -sin * dx + cos * dy;

        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double rxsq = rx * rx;
        double rysq = ry * ry;
        double x1psq = x1p * x1p;
        double y1psq = y1p * y1p;
        double lambda = x1psq / rxsq + y1psq / rysq;
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
            rxsq = rx * rx;
            rysq = ry * ry;
        }

        double numerator = rxsq * rysq - rxsq * y1psq - rysq * x1psq;
        double denominator = rxsq * y1psq + rysq * x1psq;
        double coef = denominator == 0 ? 0 : Math.sqrt(Math.max(0, numerator / denominator));
        if (large == sweep) coef = -coef;
        double cxp = coef * rx * y1p / ry;
        double cyp = coef * -ry * x1p / rx;
        double centerX = cos * cxp - sin * cyp + (x1 + x2) / 2;
        double centerY = sin * cxp + cos * cyp + (y1 + y2) / 2;

        double theta1 = SvgPath.vectorAngle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double delta = SvgPath.vectorAngle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry);
        if (!sweep && delta > 0) delta -= Math.PI * 2;
        else if (sweep && delta < 0) delta += Math.PI * 2;

        int segments = Math.max(1, (int) Math.ceil(Math.abs(delta) / (Math.PI / 2)));
        double step = delta / segments;
        for (int i = 0; i < segments; i++) {
            SvgPath.curveArc(path, centerX, centerY, rx, ry, cos, sin, theta1 + step * i, theta1 + step * (i + 1));
        }
    }

    private static void curveArc(
            Path2D path, double cx, double cy, double rx, double ry,
            double cos, double sin, double theta1, double theta2
    ) {
        double step = theta2 - theta1;
        double halfTan = Math.tan(step / 2);
        double alpha = Math.sin(step) * (Math.sqrt(4 + 3 * halfTan * halfTan) - 1) / 3;

        double[] start = SvgPath.ellipsePoint(cx, cy, rx, ry, cos, sin, theta1);
        double[] end = SvgPath.ellipsePoint(cx, cy, rx, ry, cos, sin, theta2);
        double[] d1 = SvgPath.ellipseDerivative(rx, ry, cos, sin, theta1);
        double[] d2 = SvgPath.ellipseDerivative(rx, ry, cos, sin, theta2);

        path.curveTo(
                start[0] + alpha * d1[0], start[1] + alpha * d1[1],
                end[0] - alpha * d2[0], end[1] - alpha * d2[1],
                end[0], end[1]
        );
    }

    private static double[] ellipsePoint(double cx, double cy, double rx, double ry, double cos, double sin, double theta) {
        double x = rx * Math.cos(theta);
        double y = ry * Math.sin(theta);
        return new double[] {cx + cos * x - sin * y, cy + sin * x + cos * y};
    }

    private static double[] ellipseDerivative(double rx, double ry, double cos, double sin, double theta) {
        double dx = -rx * Math.sin(theta);
        double dy = ry * Math.cos(theta);
        return new double[] {cos * dx - sin * dy, sin * dx + cos * dy};
    }

    private static double vectorAngle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double lengths = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        double angle = Math.acos(Math.clamp(lengths == 0 ? 0 : dot / lengths, -1, 1));
        if (ux * vy - uy * vx < 0) angle = -angle;
        return angle;
    }

    private void skipSeparators() {
        while (this.index < this.data.length()) {
            char c = this.data.charAt(this.index);
            if (c == ',' || c == ' ' || c == '\n' || c == '\r' || c == '\t') this.index++;
            else break;
        }
    }

    private boolean hasNumber() {
        this.skipSeparators();
        if (this.index >= this.data.length()) return false;
        char c = this.data.charAt(this.index);
        return c == '+' || c == '-' || c == '.' || (c >= '0' && c <= '9');
    }

    private double number() {
        this.skipSeparators();
        int start = this.index;
        if (this.index < this.data.length()) {
            char sign = this.data.charAt(this.index);
            if (sign == '+' || sign == '-') this.index++;
        }
        boolean digits = false;
        while (this.index < this.data.length()) {
            char c = this.data.charAt(this.index);
            if (c < '0' || c > '9') break;
            digits = true;
            this.index++;
        }
        if (this.index < this.data.length() && this.data.charAt(this.index) == '.') {
            this.index++;
            while (this.index < this.data.length()) {
                char c = this.data.charAt(this.index);
                if (c < '0' || c > '9') break;
                digits = true;
                this.index++;
            }
        }
        if (this.index < this.data.length()) {
            char exp = this.data.charAt(this.index);
            if (exp == 'e' || exp == 'E') {
                int mark = this.index;
                this.index++;
                if (this.index < this.data.length()) {
                    char sign = this.data.charAt(this.index);
                    if (sign == '+' || sign == '-') this.index++;
                }
                int expStart = this.index;
                while (this.index < this.data.length()) {
                    char c = this.data.charAt(this.index);
                    if (c < '0' || c > '9') break;
                    this.index++;
                }
                if (this.index == expStart) this.index = mark;
            }
        }
        if (!digits || start == this.index) {
            throw new IllegalArgumentException("Expected a number in SVG path at " + start);
        }
        return Double.parseDouble(this.data.substring(start, this.index));
    }

    private int flag() {
        this.skipSeparators();
        if (this.index >= this.data.length()) throw new IllegalArgumentException("Expected an arc flag");
        char c = this.data.charAt(this.index);
        if (c != '0' && c != '1') throw new IllegalArgumentException("Expected an arc flag");
        this.index++;
        return c - '0';
    }
}
