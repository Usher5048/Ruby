package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class BooleanValue extends Value<Boolean> {
    protected BooleanValue(
            String name, String description,
            IFlagHandler flagHandler, boolean defaultValue
    ) {
        super(name, description, flagHandler, defaultValue);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public boolean fromString(String str) {
        try {
            this.value = Boolean.parseBoolean(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        stream.write(this.value ? 1 : 0);
        return 1;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        this.value = stream.read() == 1;
    }

    public static class Builder extends Value.Builder<Boolean, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected BooleanValue buildValue() {
            return new BooleanValue(
                    this.name, this.description,
                    this.flagHandler, this.defaultValue
            );
        }
    }
}
