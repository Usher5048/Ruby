package ruby.systems.config;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class EntityTypeListValue extends ListValue<EntityType<?>> {

    protected EntityTypeListValue(
            String name, String description,
            Consumer<List<EntityType<?>>> changed, Callable<Boolean> visible,
            List<EntityType<?>> defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
    }

    @Override
    protected String toStringElement(EntityType<?> value) {
        return value.getName().getString();
    }

    @Override
    protected EntityType<?> fromStringElement(String str) {
        if(str == null) return null;

        for(EntityType<?> type : Registries.ENTITY_TYPE) {
            if(!str.equals(type.getName().getString())) continue;
            return type;
        }

        return null;
    }

    @Override
    protected int serializeElement(ByteArrayOutputStream stream, EntityType<?> value) {
        String typeID = Registries.ENTITY_TYPE.getId(value).toString();

        byte[] bytes = typeID.getBytes();
        short len = (short) bytes.length;

        stream.write(len >> 8);
        stream.write(len & 0xFF);
        stream.writeBytes(bytes);

        return 2 + len;
    }

    @Override
    protected EntityType<?> deserializeElement(ByteArrayInputStream stream) {
        int length = stream.read() << 8 | stream.read();
        byte[] bytes = new byte[length];

        stream.readNBytes(bytes, 0, length);
        String typeID = new String(bytes);

        return Registries.ENTITY_TYPE.get(Identifier.tryParse(typeID));
    }

    public static class Builder extends ListValue.Builder<EntityType<?>, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected EntityTypeListValue buildValue() {
            return new EntityTypeListValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
