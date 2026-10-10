package ruby.systems.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class BlockValue extends Value<Block> {
    protected BlockValue(
            String name, String description,
            Consumer<Block> changed, Callable<Boolean> visible,
            Block defaultValue
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

        for(Block block : BuiltInRegistries.BLOCK) {
            if(!str.equals(block.getName().getString())) continue;
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        String blockID = BuiltInRegistries.BLOCK.getKey(this.value()).toString();

        byte[] bytes = blockID.getBytes();
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
        String blockID = new String(bytes);

        this.setValue(BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(blockID)));
    }

    public static class Builder extends Value.Builder<Block, ruby.systems.config.BlockValue.Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected ruby.systems.config.BlockValue.Builder self() {
            return this;
        }

        @Override
        protected BlockValue buildValue() {
            return new BlockValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
