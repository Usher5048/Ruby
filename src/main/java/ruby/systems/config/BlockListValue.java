package ruby.systems.config;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class BlockListValue extends ListValue<Block> {

    protected BlockListValue(
            String name, String description,
            Consumer<List<Block>> changed, Callable<Boolean> visible,
            List<Block> defaultValue
    ) {
        super(name, description, changed, visible, defaultValue);
    }

    @Override
    protected String toStringElement(Block value) {
        return value.getName().getString();
    }

    @Override
    protected Block fromStringElement(String str) {
        if(str == null) return null;

        for(Block block : Registries.BLOCK) {
            if(!str.equals(block.getName().getString())) continue;
            return block;
        }

        return null;
    }

    @Override
    protected int serializeElement(ByteArrayOutputStream stream, Block value) {
        String blockID = Registries.BLOCK.getId(value).toString();

        byte[] bytes = blockID.getBytes();
        short len = (short) bytes.length;

        stream.write(len >> 8);
        stream.write(len & 0xFF);
        stream.writeBytes(bytes);

        return 2 + len;
    }

    @Override
    protected Block deserializeElement(ByteArrayInputStream stream) {
        int length = stream.read() << 8 | stream.read();
        byte[] bytes = new byte[length];

        stream.readNBytes(bytes, 0, length);
        String blockID = new String(bytes);

        return Registries.BLOCK.get(Identifier.tryParse(blockID));
    }

    public static class Builder extends ListValue.Builder<Block, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected BlockListValue buildValue() {
            return new BlockListValue(
                    this.name, this.description,
                    this.changed, this.visible,
                    this.defaultValue
            );
        }
    }
}
