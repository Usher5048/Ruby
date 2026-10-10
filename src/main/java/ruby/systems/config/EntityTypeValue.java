package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class EntityTypeValue extends Value<EntityType<?>> {
    protected EntityTypeValue(
            String name, String description,
            Consumer<EntityType<?>> changed, Callable<Boolean> visible,
            EntityType<?> defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
    }


    @Override
    public String toString() {
        return this.value().getDescription().getString();
    }

    @Override
    public boolean fromString(String str) {
        if(str == null) return false;

        for(EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if(!str.equals(type.getDescription().getString())) continue;
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        String typeID = BuiltInRegistries.ENTITY_TYPE.getKey(this.value()).toString();

        byte[] bytes = typeID.getBytes();
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
        String typeID = new String(bytes);

        this.setValue(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(typeID)));
    }

    public static class Builder extends Value.Builder<EntityType<?>, ruby.systems.config.EntityTypeValue.Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected ruby.systems.config.EntityTypeValue.Builder self() {
            return this;
        }

        @Override
        protected EntityTypeValue buildValue() {
            return new EntityTypeValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
