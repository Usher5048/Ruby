package ruby.systems.config;

import ruby.RubyClient;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

// TODO: Compress the save file
public class ConfigManager {
    private interface ConfigLoader {
        boolean load(ByteArrayInputStream stream);
    }

    private static final int VERSION = 1;
    private static final File configFile = new File(RubyClient.client.runDirectory.getAbsolutePath() + "/config/" + RubyClient.MOD_ID);
    private static final HashMap<Integer, ConfigLoader> loaders = new HashMap<>();

    static {
        ConfigManager.loaders.put(1, ConfigManager::loadV1);
    }

    private static final Map<String, int[]> panelPositions = new HashMap<>();
    public static Map<String, int[]> getPanelPositions() {
        return ConfigManager.panelPositions;
    }

    private static int readShort(ByteArrayInputStream s) {
        return (s.read() << 8) | s.read();
    }
    private static void writeShort(ByteArrayOutputStream s, int v) {
        s.write((v >> 8) & 0xFF);
        s.write(v & 0xFF);
    }

    private static int readInt(ByteArrayInputStream s) {
        return (s.read() << 24) |
                (s.read() << 16) |
                (s.read() << 8) |
                s.read();
    }

    private static void writeInt(ByteArrayOutputStream s, int v) {
        s.write((v >> 24) & 0xFF);
        s.write((v >> 16) & 0xFF);
        s.write((v >>  8) & 0xFF);
        s.write(v & 0xFF);
    }

    private static void writeString(ByteArrayOutputStream s, String str) {
        byte[] bytes = str.getBytes();

        ConfigManager.writeShort(s, bytes.length);
        s.writeBytes(bytes);
    }

    private static String readString(ByteArrayInputStream s) {
        int len = ConfigManager.readShort(s);
        byte[] bytes = new byte[len];

        s.readNBytes(bytes, 0, len);
        return new String(bytes);
    }

    private static void configToBytes(ByteArrayOutputStream stream, Configuration config) {
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();

        stream.write(config.getAll().size());
        for(String key : config.getAll()) {
            Value<?> value = config.get(key);

            ConfigManager.writeString(stream, key);

            valueStream.reset();
            int valueLen = value.serialize(valueStream);

            ConfigManager.writeShort(stream, valueLen);
            stream.writeBytes(valueStream.toByteArray());
        }
    }

    private static void bytesToConfig(ByteArrayInputStream stream, Configuration config) {
        int count = stream.read();
        for(int i = 0; i < count; i++) {
            String key = ConfigManager.readString(stream);
            int len = ConfigManager.readShort(stream);

            if(config == null || config.get(key) == null) {
                stream.readNBytes(new byte[len], 0, len);
                continue;
            }

            config.get(key).deserialize(stream);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveState() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(ConfigManager.VERSION);

            ConfigManager.configToBytes(stream, RubyClient.config);

            ConfigManager.writeShort(stream, Modules.getModules().size());
            for(Module module : Modules.getModules()) {
                ConfigManager.writeString(stream, module.name());
                ConfigManager.writeInt(stream, module.keybind.serialize());
                ConfigManager.configToBytes(stream, module.config);
            }

            ConfigManager.writeShort(stream, ConfigManager.panelPositions.size());
            for(Map.Entry<String, int[]> entry : ConfigManager.panelPositions.entrySet()) {
                ConfigManager.writeString(stream, entry.getKey());
                ConfigManager.writeInt(stream, entry.getValue()[0]);
                ConfigManager.writeInt(stream, entry.getValue()[1]);
            }

            ConfigManager.configFile.getParentFile().mkdirs();
            Files.write(ConfigManager.configFile.toPath(), stream.toByteArray());
        } catch(Exception ignored) {}
    }

    public static boolean loadV1(ByteArrayInputStream stream) {
        ConfigManager.bytesToConfig(stream, RubyClient.config);

        int moduleCount = ConfigManager.readShort(stream);
        for(int i = 0; i < moduleCount; i++) {
            String name = ConfigManager.readString(stream);
            Module module = Modules.getByName(name);

            if(module == null) {
                ConfigManager.readInt(stream);
                ConfigManager.bytesToConfig(stream, null);
                continue;
            }

            module.keybind.deserialize(ConfigManager.readInt(stream));
            ConfigManager.bytesToConfig(stream, module.config);
        }

        ConfigManager.panelPositions.clear();

        int panelCount = ConfigManager.readShort(stream);
        for(int i = 0; i < panelCount; i++) {
            String panelName = ConfigManager.readString(stream);
            int x = ConfigManager.readInt(stream);
            int y = ConfigManager.readInt(stream);

            ConfigManager.panelPositions.put(panelName, new int[] {x, y});
        }

        return true;
    }

    public static boolean loadState() {
        try {
            byte[] data = Files.readAllBytes(ConfigManager.configFile.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);

            int version = stream.read();
            if(!ConfigManager.loaders.containsKey(version))
                return false;

            return ConfigManager.loaders.get(version).load(stream);
        } catch(Exception e) {
            return false;
        }
    }
}
