package ruby.systems.config;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ItemValue extends Value<Item> {
    protected ItemValue(
            String name, String description,
            Consumer<Item> changed, Callable<Boolean> visible,
            Item defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
    }


    @Override
    public String toString() {
        return this.value().getName().getString();
    }

    @Override
    public boolean fromString(String str) {
        if(str == null) return false;

        for(Item item : Registries.ITEM) {
            if(!str.equals(item.getName().getString())) continue;
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        String itemID = Registries.ITEM.getId(this.value()).toString();

        byte[] bytes = itemID.getBytes();
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
        String itemID = new String(bytes);

        this.setValue(Registries.ITEM.get(Identifier.tryParse(itemID)));
    }

    public static class Builder extends Value.Builder<Item, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected ItemValue buildValue() {
            return new ItemValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
