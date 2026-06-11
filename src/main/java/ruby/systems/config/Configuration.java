package ruby.systems.config;

import java.util.LinkedHashMap;
import java.util.Set;


public class Configuration {
    private final LinkedHashMap<String, Value<?>> options = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public <T, V extends Value<T>> V create(Value<T> value) {
        this.options.put(value.name(), value);
        return (V) value;
    }

    public Set<String> getAll() {
        return this.options.keySet();
    }
    public Value<?> get(String key) {
        return this.options.get(key);
    }

    @SuppressWarnings("unchecked")
    public void resetToDefaults() {
        for (String key : this.getAll()) {
            Value<Object> value = (Value<Object>) this.get(key);
            value.setValue(value.defaultValue());
        }
    }
}
