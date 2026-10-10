package ruby.systems.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

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
        return value.getDescription().getString();
    }

    @Override
    protected EntityType<?> fromStringElement(String str) {
        if(str == null) return null;

        for(EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if(!str.equals(type.getDescription().getString())) continue;
            return type;
        }

        return null;
    }

    @Override
    protected int serializeElement(ByteArrayOutputStream stream, EntityType<?> value) {
        String typeID = BuiltInRegistries.ENTITY_TYPE.getKey(value).toString();

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

        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(typeID));
    }

    public static class Builder extends ListValue.Builder<EntityType<?>, ruby.systems.config.EntityTypeListValue.Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected ruby.systems.config.EntityTypeListValue.Builder self() {
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
