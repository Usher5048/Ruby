package ruby.systems.config;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ColorValue extends Value<Integer> {
    protected ColorValue(
            String name, String description,
            Consumer<Integer> changed, Callable<Boolean> visible,
            int defaultValue
    ) {
        super(name, description, changed, visible, defaultValue | 0xFF000000);
    }

    public int opaque() {
        return this.value() | 0xFF000000;
    }

    public float[] hsb() {
        int color = this.opaque();
        return Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
    }

    public void setHsb(float hue, float saturation, float brightness) {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        this.setValue(0xFF000000 | (rgb & 0x00FFFFFF));
    }

    @Override
    public String toString() {
        return String.format("#%06X", this.opaque() & 0xFFFFFF);
    }

    @Override
    public boolean fromString(String str) {
        if (str == null || str.isBlank()) return false;
        String hex = str.startsWith("#") ? str.substring(1) : str;
        if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        try {
            this.setValue(0xFF000000 | (Integer.parseInt(hex, 16) & 0xFFFFFF));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        int bits = this.opaque();
        stream.writeBytes(new byte[] {
                (byte) (bits >> 24),
                (byte) (bits >> 16),
                (byte) (bits >> 8),
                (byte) bits
        });
        return 4;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        this.setValue(stream.read() << 24 |
                stream.read() << 16 |
                stream.read() << 8 |
                stream.read());
    }

    public static class Builder extends Value.Builder<Integer, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected ColorValue buildValue() {
            return new ColorValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
