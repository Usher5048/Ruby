package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

// this is so much better than the old DynamicValue class
public abstract class Value<T> {
    public interface IFlagHandler { boolean isVisible(); }

    private final String name;
    private final String description;
    private final IFlagHandler flagHandler;
    private final T defaultValue;

    protected T value;

    protected Value(String name, String description, IFlagHandler flagHandler, T defaultValue) {
        this.name = name;
        this.description = description;
        this.flagHandler = flagHandler;
        this.defaultValue = defaultValue;

        this.value = defaultValue;
    }

    public String name() {
        return this.name;
    }
    public String description() {
        return this.description;
    }
    public boolean isVisible() {
        return this.flagHandler.isVisible();
    }
    public T defaultValue() {
        return this.defaultValue;
    }

    public void value(T value) {
        this.value = value;
    }
    public T value() {
        return this.value;
    }

    @Override
    public abstract String toString();
    public abstract boolean fromString(String str); // returns false if string is invalid

    public abstract int serialize(ByteArrayOutputStream stream);
    public abstract void deserialize(ByteArrayInputStream stream);

    protected static abstract class Builder<T, B extends Builder<T, B>> {
        protected String name;
        protected String description       = "";
        protected IFlagHandler flagHandler = () -> true;
        protected T defaultValue           = null;

        public Builder(String name) {
            this.name = name;
        }
        public B name(String n) {
            this.name = n;
            return this.self();
        }

        public B description(String d) {
            this.description = d;
            return this.self();
        }

        public B visible(IFlagHandler f) {
            this.flagHandler = f;
            return this.self();
        }

        public B defaultValue(T v) {
            this.defaultValue = v;
            return this.self();
        }

        protected abstract B self();
        protected abstract Value<T> buildValue();

        public Value<T> build() {
            if(this.name == null || this.name.isBlank()) throw new IllegalStateException("Attempt to build Value without name");
            if(this.defaultValue == null) throw new IllegalStateException("Attempt to build Value without default value");

            if(this.description == null) this.description = "";
            return this.buildValue();
        }
    }
}
