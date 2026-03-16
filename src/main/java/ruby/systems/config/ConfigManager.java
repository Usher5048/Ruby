package ruby.systems.config;

import ruby.RubyClient;
import ruby.systems.modules.Keybind;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

//TODO:
// Rip this out and remake it to support compression,
// keybinds, backwards compatibility, store the file
// in the general config area, and save in a binary
// format using the serialize functions on the types

public class ConfigManager {
    private static final int VERSION = 2;
    private static final File configFile = new File(RubyClient.client.runDirectory.getAbsolutePath() + "/config/" + RubyClient.MOD_ID);

    /** Panel positions saved by the ClickGUI — persisted to disk. */
    private static final Map<String, int[]> panelPositions = new HashMap<>();

    public static Map<String, int[]> getPanelPositions() { return panelPositions; }

    private static void writeShort(ByteArrayOutputStream s, int v) {
        s.write((v >> 8) & 0xFF);
        s.write(v & 0xFF);
    }
    private static int readShort(ByteArrayInputStream s) {
        return (s.read() << 8) | s.read();
    }
    private static void writeInt(ByteArrayOutputStream s, int v) {
        s.write((v >> 24) & 0xFF);
        s.write((v >> 16) & 0xFF);
        s.write((v >>  8) & 0xFF);
        s.write(v & 0xFF);
    }
    private static int readInt(ByteArrayInputStream s) {
        return (s.read() << 24) | (s.read() << 16) | (s.read() << 8) | s.read();
    }
    private static void writeString(ByteArrayOutputStream s, String str) {
        byte[] bytes = str.getBytes();
        writeShort(s, bytes.length);
        s.writeBytes(bytes);
    }
    private static String readString(ByteArrayInputStream s) {
        int len = readShort(s);
        byte[] bytes = new byte[len];
        s.readNBytes(bytes, 0, len);
        return new String(bytes);
    }

    private static void configToBytes(ByteArrayOutputStream stream, Configuration config) {
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();

        stream.write(config.getAll().size());
        for (String key : config.getAll()) {
            Value<?> value = config.get(key);

            writeString(stream, key);

            valueStream.reset();
            int valueLen = value.serialize(valueStream);

            writeShort(stream, valueLen);
            stream.writeBytes(valueStream.toByteArray());
        }
    }

    private static void bytesToConfig(ByteArrayInputStream stream, Configuration config) {
        int count = stream.read();
        for (int i = 0; i < count; i++) {
            String key = readString(stream);
            int valueLen = readShort(stream);

            Value<?> value = config.get(key);
            if (value != null) {
                byte[] valueBytes = new byte[valueLen];
                stream.readNBytes(valueBytes, 0, valueLen);
                value.deserialize(new ByteArrayInputStream(valueBytes));
            } else {
                for (int k = 0; k < valueLen; k++) stream.read();
            }
        }
    }

    public static void saveState() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(ConfigManager.VERSION);

            // Global config
            ConfigManager.configToBytes(stream, RubyClient.config);

            // Modules
            var modules = Modules.getModules();
            writeShort(stream, modules.size());
            for (Module module : modules) {
                writeString(stream, module.name());
                stream.write(module.enabled() ? 1 : 0);
                writeInt(stream, module.keybind.getCode());
                configToBytes(stream, module.config);
            }

            // Panel positions
            writeShort(stream, panelPositions.size());
            for (Map.Entry<String, int[]> entry : panelPositions.entrySet()) {
                writeString(stream, entry.getKey());
                writeInt(stream, entry.getValue()[0]);
                writeInt(stream, entry.getValue()[1]);
            }

            configFile.getParentFile().mkdirs();
            Files.write(configFile.toPath(), stream.toByteArray());
            RubyClient.log("Saved config (" + stream.size() + " bytes)");
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean loadState() {
        if(!configFile.exists()) return false;
        try {
            byte[] data = Files.readAllBytes(configFile.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            int version = stream.read();
            if(version != VERSION) return false;

            // Global config
            bytesToConfig(stream, RubyClient.config);

            // Modules
            int moduleCount = readShort(stream);
            for(int i = 0; i < moduleCount; i++) {
                String name = readString(stream);
                boolean enabled = stream.read() == 1;
                int keyCode = readInt(stream);

                Module module = Modules.getByName(name);
                if(module != null) {
                    module.keybind = Keybind.key(keyCode, false);
                    bytesToConfig(stream, module.config);
                    Modules.setEnabled(module, enabled);
                } else {
                    // Skip config for unknown module
                    int cfgCount = stream.read();
                    for(int j = 0; j < cfgCount; j++) {
                        readString(stream);
                        int vLen = readShort(stream);
                        stream.skipNBytes(vLen);
                    }
                }
            }

            // Panel positions
            int posCount = readShort(stream);
            for(int i = 0; i < posCount; i++) {
                String key = readString(stream);
                int px = readInt(stream);
                int py = readInt(stream);
                panelPositions.put(key, new int[]{ px, py });
            }
        } catch(Exception e) {
            return false;
        }

        return true;
    }
}
