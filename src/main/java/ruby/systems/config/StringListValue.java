package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class StringListValue extends ListValue<String> {
    protected StringListValue(
            String name, String description,
            Consumer<List<String>> changed, Callable<Boolean> visible,
            List<String> defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
    }

    @Override
    protected String toStringElement(String value) {
        return value;
    }

    @Override
    protected String fromStringElement(String str) {
        return str;
    }

    @Override
    protected int serializeElement(ByteArrayOutputStream stream, String value) {
        byte[] bytes = value.getBytes();
        short len = (short) bytes.length;

        stream.write(len >> 8);
        stream.write(len & 0xFF);
        stream.writeBytes(bytes);

        return 2 + len;
    }

    @Override
    protected String deserializeElement(ByteArrayInputStream stream) {
        int length = stream.read() << 8 | stream.read();
        byte[] bytes = new byte[length];

        stream.readNBytes(bytes, 0, length);
        return new String(bytes);
    }

    public static class Builder extends ListValue.Builder<String, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected StringListValue buildValue() {
            return new StringListValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
