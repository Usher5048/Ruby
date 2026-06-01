package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class EnumValue<T extends Enum<?>> extends Value<T> {
    private final T[] enumValues;

    @SuppressWarnings("unchecked")
    protected EnumValue(
            String name, String description,
            Consumer<T> changed, Callable<Boolean> visible,
            T defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
        this.enumValues = (T[]) this.defaultValue().getDeclaringClass().getEnumConstants();
    }


    public T[] values() {
        return this.enumValues;
    }

    public void cycle() {
        int idx = this.value().ordinal() + 1;
        if (idx >= this.enumValues.length) idx = 0;
        this.setValue(this.enumValues[idx]);
    }

    @Override
    public String toString() {
        return this.value().toString();
    }

    @Override
    public boolean fromString(String str) {
        for(T val : this.enumValues) {
            if(!str.equalsIgnoreCase(val.toString())) continue;

            this.setValue(val);
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        stream.write(this.value().ordinal());
        return 1;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        this.setValue(this.enumValues[stream.read()]);
    }

    public static class Builder<T extends Enum<?>> extends Value.Builder<T, Builder<T>> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder<T> self() {
            return this;
        }

        @Override
        protected EnumValue<T> buildValue() {
            return new EnumValue<>(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
