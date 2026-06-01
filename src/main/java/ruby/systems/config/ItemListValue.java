package ruby.systems.config;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ItemListValue extends ListValue<Item> {

    protected ItemListValue(
            String name, String description,
            Consumer<List<Item>> changed, Callable<Boolean> visible,
            List<Item> defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
    }

    @Override
    protected String toStringElement(Item value) {
        return value.getName().getString();
    }

    @Override
    protected Item fromStringElement(String str) {
        if(str == null) return null;

        for(Item item : Registries.ITEM) {
            if(!str.equals(item.getName().getString())) continue;
            return item;
        }

        return null;
    }

    @Override
    protected int serializeElement(ByteArrayOutputStream stream, Item value) {
        String itemID = Registries.ITEM.getId(value).toString();

        byte[] bytes = itemID.getBytes();
        short len = (short) bytes.length;

        stream.write(len >> 8);
        stream.write(len & 0xFF);
        stream.writeBytes(bytes);

        return 2 + len;
    }

    @Override
    protected Item deserializeElement(ByteArrayInputStream stream) {
        int length = stream.read() << 8 | stream.read();
        byte[] bytes = new byte[length];

        stream.readNBytes(bytes, 0, length);
        String itemID = new String(bytes);

        return Registries.ITEM.get(Identifier.tryParse(itemID));
    }

    public static class Builder extends ListValue.Builder<Item, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected ItemListValue buildValue() {
            return new ItemListValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
