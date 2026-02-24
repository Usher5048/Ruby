package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ListValue<T> extends Value<List<T>> {
    protected ListValue(
            String name, String description,
            IFlagHandler flagHandler, List<T> defaultValue
    ) {
        super(name, description, flagHandler, defaultValue);
    }

    protected abstract String toStringElement(T value);
    protected abstract T fromStringElement(String str);

    @Override
    public String toString() {
        return this.value.stream()
                .map(this::toStringElement)
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean fromString(String str) {
        this.value.clear();

        for(String s : str.split(", ")) {
            T val = this.fromStringElement(s);
            if(val == null) return false;

            this.value.add(val);
        }

        return true;
    }

    protected abstract int serializeElement(ByteArrayOutputStream stream, T value);
    protected abstract T deserializeElement(ByteArrayInputStream stream);

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        int size = this.value.size();
        stream.writeBytes(new byte[] {
                (byte) (size >> 8),
                (byte) size
        });

        int elmSize = 0;
        for(T val : this.value)
            elmSize += this.serializeElement(stream, val);

        return 2 + size + elmSize;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        int size = stream.read() << 24 |
                stream.read() << 16 |
                stream.read() << 8 |
                stream.read();

        this.value.clear();
        for(int i = 0; i < size; i++)
            this.value.add(this.deserializeElement(stream));
    }

    protected static abstract class Builder<T, B extends Builder<T, B>> extends Value.Builder<List<T>, B> {
        public Builder(String name) {
            super(name);
        }
        public B defaultValue(T ...a) {
            this.defaultValue = new ArrayList<>(List.of(a));
            return this.self();
        }
    }
}
