package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

// this is so much better than the old DynamicValue class
public abstract class Value<T> {
    private final String name;
    private final String description;
    private final Callable<Boolean> visible;
    private final Consumer<T> changed;
    private final T defaultValue;

    private T value;

    protected Value(
            String name, String description,
            Consumer<T> changed, Callable<Boolean> visible,
            T defaultValue
    ) {
        this.name = name;
        this.description = description;
        this.changed = changed;
        this.visible = visible;
        this.defaultValue = defaultValue;

        this.value = defaultValue;
    }

    public String name() {
        return this.name;
    }
    public String description() {
        return this.description;
    }
    public boolean visible() {
        try { return this.visible.call(); } catch(Exception ignored) { return true; }
    }
    public T defaultValue() {
        return this.defaultValue;
    }
    public boolean isDefault() {
        return this.defaultValue.equals(this.value);
    }

    public T value() {
        return this.value;
    }
    public void setValue(T value) {
        this.value = value;
        this.changed.accept(value);
    }

    protected void setValueSilent(T value) {
        this.value = value;
    }

    @Override
    public abstract String toString();
    public abstract boolean fromString(String str); // returns false if string is invalid

    public abstract int serialize(ByteArrayOutputStream stream);
    public abstract void deserialize(ByteArrayInputStream stream);

    protected static abstract class Builder<T, B extends Builder<T, B>> {
        protected String name;
        protected String description        = "";
        protected Consumer<T> changed       = t -> {};
        protected Callable<Boolean> visible = () -> true;
        protected T defaultValue            = null;

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

        public B visible(Callable<Boolean> v) {
            this.visible = v;
            return this.self();
        }

        public B changed(Consumer<T> c) {
            this.changed = c;
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
