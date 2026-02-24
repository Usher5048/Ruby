package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class StringValue extends Value<String> {
    protected StringValue(
            String name, String description,
            IFlagHandler flagHandler, String defaultValue
    ) {
        super(name, description, flagHandler, defaultValue);
    }


    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public boolean fromString(String str) {
        this.value = str;
        return true;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        byte[] bytes = this.value.getBytes();
        short len = (short) bytes.length;

        stream.write(len >> 8);
        stream.write(len & 0xFF);
        stream.writeBytes(bytes);

        return 2 + len;
    }

    @Override
    public void deserialize(ByteArrayInputStream stream) {
        int length = stream.read() << 8 | stream.read();
        byte[] bytes = new byte[length];

        stream.readNBytes(bytes, 0, length);
        this.value = new String(bytes);
    }

    public static class Builder extends Value.Builder<String, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected StringValue buildValue() {
            return new StringValue(
                    this.name, this.description,
                    this.flagHandler, this.defaultValue
            );
        }
    }
}
