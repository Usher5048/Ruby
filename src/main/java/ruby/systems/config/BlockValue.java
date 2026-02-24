package ruby.systems.config;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class BlockValue extends Value<Block> {
    protected BlockValue(
            String name, String description,
            IFlagHandler flagHandler, Block defaultValue
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

        for(Block block : Registries.BLOCK) {
            if(!str.equals(block.getName().getString())) continue;
            return true;
        }

        return false;
    }

    @Override
    public int serialize(ByteArrayOutputStream stream) {
        String blockID = Registries.BLOCK.getId(this.value).toString();

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

        this.value = Registries.BLOCK.get(Identifier.tryParse(blockID));
    }

    public static class Builder extends Value.Builder<Block, Builder> {
        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected BlockValue buildValue() {
            return new BlockValue(
                    this.name, this.description,
                    this.flagHandler, this.defaultValue
            );
        }
    }
}
