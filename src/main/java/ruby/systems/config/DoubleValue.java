package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class DoubleValue extends Value<Double> {
    protected final double minValue;
    protected final double maxValue;
    protected final double step;

    protected DoubleValue(
            String name, String description,
            Consumer<Double> changed, Callable<Boolean> visible,
            double defaultValue, double minValue, double maxValue,
            double step
    ) {
        super(name, description, changed, visible, defaultValue);

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
    }

    // Allows auto converting integers to doubles without casts
    public void setValue(double val) {
        this.setValue((Double) val);
    }

    public double min() {
        return this.minValue;
    }
    public double max() {
        return this.maxValue;
    }
    public double step() {
        return this.step;
    }

    @Override
    public String toString() {
        return this.value().toString();
    }

    @Override
    public boolean fromString(String str) {
        try {
            this.setValue(Double.parseDouble(str));
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        long bits = Double.doubleToRawLongBits(this.value());
        stream.writeBytes(new byte[] {
                (byte) (bits >> 56),
                (byte) (bits >> 48),
                (byte) (bits >> 40),
                (byte) (bits >> 32),
                (byte) (bits >> 24),
                (byte) (bits >> 16),
                (byte) (bits >>  8),
                (byte) bits
        });

        return 8;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        this.setValue(Double.longBitsToDouble(
                (long) stream.read() << 56 |
                (long) stream.read() << 48 |
                (long) stream.read() << 40 |
                (long) stream.read() << 32 |
                (long) stream.read() << 24 |
                (long) stream.read() << 16 |
                (long) stream.read() << 8 |
                (long) stream.read()
        ));
    }

    public static class Builder extends Value.Builder<Double, Builder> {
        public Builder(String name) {
            super(name);
        }

        private double min  = -Double.MAX_VALUE;
        private double max  =  Double.MAX_VALUE;
        private double step = 0.1;

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder step(double step) {
            this.step = step;
            return this;
        }

        public Builder range(double min, double max) {
            this.min = min;
            this.max = max;
            return this;
        }

        public Builder range(double min, double max, double step) {
            this.min = min;
            this.max = max;
            this.step = step;
            return this;
        }

        // Allows auto converting integers to doubles without casts
        public Builder defaultValue(double defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected DoubleValue buildValue() {
            return new DoubleValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue,
                    this.min, this.max, this.step
            );
        }
    }
}
