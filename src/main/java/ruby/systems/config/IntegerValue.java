package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class IntegerValue extends Value<Integer> {
    protected final int minValue;
    protected final int maxValue;

    protected IntegerValue(
            String name, String description, Value.IFlagHandler flagHandler,
            int defaultValue, int minValue, int maxValue
    ) {
        super(name, description, flagHandler, defaultValue);

        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int min() {
        return this.minValue;
    }
    public int max() {
        return this.maxValue;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public boolean fromString(String str) {
        try {
            this.value = Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        int bits = this.value;
        stream.writeBytes(new byte[] {
                (byte) (bits >> 24),
                (byte) (bits >> 16),
                (byte) (bits >>  8),
                (byte) bits
        });

        return 4;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        this.value = stream.read() << 24 |
                stream.read() << 16 |
                stream.read() << 8 |
                stream.read();
    }

    public static class Builder extends Value.Builder<Integer, Builder> {
        public Builder(String name) {
            super(name);
        }

        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder range(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected IntegerValue buildValue() {
            return new IntegerValue(
                    this.name, this.description, this.flagHandler,
                    this.defaultValue, this.min, this.max
            );
        }
    }
}
