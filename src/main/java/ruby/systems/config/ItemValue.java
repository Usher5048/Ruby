package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        return this.value().getName(new ItemStack(this.value())).getString();
    }

    @Override
    public boolean fromString(String str) {
        if(str == null) return false;

        for(Item item : BuiltInRegistries.ITEM) {
            if(!str.equals(item.getName(new ItemStack(item)).getString())) continue;
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        String itemID = BuiltInRegistries.ITEM.getKey(this.value()).toString();

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

        this.setValue(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(itemID)));
    }

    public static class Builder extends Value.Builder<Item, ruby.systems.config.ItemValue.Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected ruby.systems.config.ItemValue.Builder self() {
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
