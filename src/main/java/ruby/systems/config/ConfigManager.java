package ruby.systems.config;

import ruby.RubyClient;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class ConfigManager {
    private static final int VERSION = 1;
    private static final File configFile = new File(RubyClient.client.runDirectory.getAbsolutePath() + "/." + RubyClient.MOD_ID + "/config");

    private static byte[] configToBytes(Configuration config) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();

        stream.write(config.getAll().size());
        for(String key : config.getAll()) {
            Value<?> value = config.get(key);

            stream.write(key.length() >> 8);
            stream.write(key.length() & 0xFF);
            stream.writeBytes(key.getBytes());

            valueStream.reset();
            int valueLen = value.serialize(valueStream);

            stream.write((valueLen >> 8) & 0xFF);
            stream.write((valueLen     ) & 0xFF);
            stream.writeBytes(valueStream.toByteArray());
        }

        return stream.toByteArray();
    }

    public static void saveState() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(ConfigManager.VERSION);


    }

    public static void loadState() {

    }
}
