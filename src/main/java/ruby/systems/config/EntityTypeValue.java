package ruby.systems.config;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EntityTypeValue extends Value<EntityType<?>> {
    protected EntityTypeValue(
            String name, String description,
            IFlagHandler flagHandler, EntityType<?> defaultValue
    ) {
        super(name, description, flagHandler, defaultValue);
    }


    @Override
    public String toString() {
        return this.value.getName().getString();
    }

    @Override
    public boolean fromString(String str) {
        if(str == null) return false;

        for(EntityType<?> type : Registries.ENTITY_TYPE) {
            if(!str.equals(type.getName().getString())) continue;
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        String typeID = Registries.ENTITY_TYPE.getId(this.value).toString();

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

        this.value = Registries.ENTITY_TYPE.get(Identifier.tryParse(typeID));
    }

    public static class Builder extends Value.Builder<EntityType<?>, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected EntityTypeValue buildValue() {
            return new EntityTypeValue(
                    this.name, this.description,
                    this.flagHandler, this.defaultValue
            );
        }
    }
}
