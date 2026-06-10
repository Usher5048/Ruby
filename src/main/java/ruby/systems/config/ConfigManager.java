package ruby.systems.config;

import ruby.RubyClient;
import ruby.systems.social.FriendsManager;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ConfigManager {
    private interface ConfigLoader {
        boolean load(ByteArrayInputStream stream);
    }

    public static final int VERSION_ERROR = -1;
    private static final int VERSION = 3;
    private static final HashMap<Integer, ConfigLoader> loaders = new HashMap<>();

    private static String activeProfile = "default";

    static {
        ConfigManager.loaders.put(1, ConfigManager::loadV1);
        ConfigManager.loaders.put(2, ConfigManager::loadV2);
        ConfigManager.loaders.put(3, ConfigManager::loadV3);
    }

    private static final Map<String, int[]> panelPositions = new HashMap<>();
    private static final String CLICK_GUI_KEY = "clickgui";
    private static final int[] DEFAULT_CLICK_GUI_POS = {20, 20};

    public static Map<String, int[]> getPanelPositions() {
        return ConfigManager.panelPositions;
    }

    public static int[] getClickGuiPosition() {
        return ConfigManager.panelPositions.getOrDefault(
                ConfigManager.CLICK_GUI_KEY,
                ConfigManager.DEFAULT_CLICK_GUI_POS
        );
    }

    public static void setClickGuiPosition(int x, int y) {
        ConfigManager.panelPositions.put(ConfigManager.CLICK_GUI_KEY, new int[] {x, y});
    }

    private static File configFile() {
        File legacy = new File(RubyClient.client.runDirectory, "config/" + RubyClient.MOD_ID);
        if (legacy.isFile()) return legacy;

        File modern = new File(legacy, "client.cfg");
        if (modern.isFile()) return modern;

        if (legacy.isDirectory()) return modern;
        return legacy;
    }

    private static void ensureConfigParent(File file) {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
    }

    private static int readShort(ByteArrayInputStream s) {
        return (s.read() << 8) | s.read();
    }

    public static int readShortPublic(ByteArrayInputStream s) {
        return ConfigManager.readShort(s);
    }

    private static void writeShort(ByteArrayOutputStream s, int v) {
        s.write((v >> 8) & 0xFF);
        s.write(v & 0xFF);
    }

    public static void writeShortPublic(ByteArrayOutputStream s, int v) {
        ConfigManager.writeShort(s, v);
    }

    private static int readInt(ByteArrayInputStream s) {
        return (s.read() << 24) |
                (s.read() << 16) |
                (s.read() << 8) |
                s.read();
    }

    public static int readIntPublic(ByteArrayInputStream s) {
        return ConfigManager.readInt(s);
    }

    private static void writeInt(ByteArrayOutputStream s, int v) {
        s.write((v >> 24) & 0xFF);
        s.write((v >> 16) & 0xFF);
        s.write((v >>  8) & 0xFF);
        s.write(v & 0xFF);
    }

    public static void writeIntPublic(ByteArrayOutputStream s, int v) {
        ConfigManager.writeInt(s, v);
    }

    private static void writeString(ByteArrayOutputStream s, String str) {
        byte[] bytes = str.getBytes();

        ConfigManager.writeShort(s, bytes.length);
        s.writeBytes(bytes);
    }

    public static void writeStringPublic(ByteArrayOutputStream s, String str) {
        ConfigManager.writeString(s, str);
    }

    private static String readString(ByteArrayInputStream s) {
        int len = ConfigManager.readShort(s);
        byte[] bytes = new byte[len];

        s.readNBytes(bytes, 0, len);
        return new String(bytes);
    }

    public static String readStringPublic(ByteArrayInputStream s) {
        return ConfigManager.readString(s);
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

    public static void configToBytesPublic(ByteArrayOutputStream stream, Configuration config) {
        ConfigManager.configToBytes(stream, config);
    }

    public static void bytesToConfigPublic(ByteArrayInputStream stream, Configuration config) {
        ConfigManager.bytesToConfig(stream, config);
    }

    public static String getActiveProfile() {
        return ConfigManager.activeProfile;
    }

    public static void setActiveProfile(String profile) {
        ProfileManager.setActiveProfile(profile);
        ConfigManager.activeProfile = ProfileManager.getActiveProfile();
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
            ProfileManager.saveProfile(ProfileManager.getActiveProfile());

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ConfigManager.configToBytes(stream, RubyClient.config);
            ConfigManager.writeString(stream, ProfileManager.getActiveProfile());
            ConfigManager.writeShort(stream, FriendsManager.getFriends().size());
            for (String friend : FriendsManager.getFriends()) {
                ConfigManager.writeString(stream, friend);
            }

            int[] guiPos = ConfigManager.getClickGuiPosition();
            ConfigManager.writeInt(stream, guiPos[0]);
            ConfigManager.writeInt(stream, guiPos[1]);

            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(stream.toByteArray());
            deflater.finish();

            byte[] buf = new byte[1024];
            ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();

            while(!deflater.finished()) {
                int count = deflater.deflate(buf);
                compressedStream.write(buf, 0, count);
            }

            deflater.end();

            ByteArrayOutputStream finalStream = new ByteArrayOutputStream();
            finalStream.write(ConfigManager.VERSION);
            compressedStream.writeTo(finalStream);

            File configFile = ConfigManager.configFile();
            ConfigManager.ensureConfigParent(configFile);
            Files.write(configFile.toPath(), finalStream.toByteArray());
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to save client config", e);
        }
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

    public static boolean loadV2(ByteArrayInputStream stream) {
        Inflater inflater = new Inflater();
        inflater.setInput(stream.readAllBytes());

        byte[] buf = new byte[1024];
        ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();

        while(!inflater.finished()) {
            try {
                int count = inflater.inflate(buf);
                decompressedStream.write(buf, 0, count);
            } catch(Exception ignored) { return false; }
        }

        inflater.end();
        return ConfigManager.loadV1(new ByteArrayInputStream(
                decompressedStream.toByteArray()
        ));
    }

    public static boolean loadV3(ByteArrayInputStream stream) {
        Inflater inflater = new Inflater();
        inflater.setInput(stream.readAllBytes());

        byte[] buf = new byte[1024];
        ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();

        while(!inflater.finished()) {
            try {
                int count = inflater.inflate(buf);
                decompressedStream.write(buf, 0, count);
            } catch(Exception ignored) { return false; }
        }

        inflater.end();

        ByteArrayInputStream inner = new ByteArrayInputStream(decompressedStream.toByteArray());
        ConfigManager.bytesToConfig(inner, RubyClient.config);

        if (inner.available() <= 0) {
            ProfileManager.loadProfile("default");
            return true;
        }

        ConfigManager.activeProfile = ConfigManager.readString(inner);
        int friendCount = ConfigManager.readShort(inner);
        List<String> friends = new ArrayList<>();
        for (int i = 0; i < friendCount; i++) {
            friends.add(ConfigManager.readString(inner));
        }
        FriendsManager.setFriends(friends);
        ProfileManager.setActiveProfile(ConfigManager.activeProfile);
        ProfileManager.refreshProfileList();
        ProfileManager.loadProfile(ConfigManager.activeProfile);

        if (inner.available() >= 8) {
            ConfigManager.setClickGuiPosition(ConfigManager.readInt(inner), ConfigManager.readInt(inner));
        }

        return true;
    }

    public static int loadState() {
        try {
            File configFile = ConfigManager.configFile();
            if (!configFile.exists()) {
                ProfileManager.refreshProfileList();
                ProfileManager.loadProfile("default");
                return ConfigManager.VERSION;
            }

            byte[] data = Files.readAllBytes(configFile.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);

            int version = stream.read();
            if(!ConfigManager.loaders.containsKey(version)) return ConfigManager.VERSION_ERROR;
            if(!ConfigManager.loaders.get(version).load(stream)) return ConfigManager.VERSION_ERROR;

            if (version <= 2) {
                ProfileManager.saveProfile("default");
                ProfileManager.loadProfile("default");
            }

            ProfileManager.refreshProfileList();
            return version;
        } catch(Exception e) {
            return ConfigManager.VERSION_ERROR;
        }
    }
}
