package ruby.systems.config;

import ruby.RubyClient;
import ruby.systems.accounts.AccountStorage;
import ruby.systems.accounts.AccountsManager;
import ruby.systems.social.FriendsManager;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ConfigManager {
    public static final int VERSION_ERROR = -1;
    private static final int VERSION = 3;
    private static final HashMap<Integer, Function<ByteArrayInputStream, Boolean>> loaders = new HashMap<>();

    private static final HashMap<String, int[]> panelPositions = new HashMap<>();
    public static int[] getPanelPosition(String panelID) {
        return ConfigManager.panelPositions.get(panelID);
    }
    public static void setPanelPosition(String panelID, int x, int y) {
        ConfigManager.panelPositions.put(panelID, new int[] {x, y});
    }

    static {
        ConfigManager.loaders.put(1, ConfigManager::loadV1);
        ConfigManager.loaders.put(2, ConfigManager::loadV2);
        ConfigManager.loaders.put(3, ConfigManager::loadV3);
    }

    private static File configFile() {
        return new File(RubyClient.client.gameDirectory, "config/" + RubyClient.MOD_ID);
    }

    public static int readShort(ByteArrayInputStream s) {
        return (s.read() << 8) | s.read();
    }
    public static void writeShort(ByteArrayOutputStream s, int v) {
        s.write((v >> 8) & 0xFF);
        s.write(v & 0xFF);
    }

    public static int readInt(ByteArrayInputStream s) {
        return (s.read() << 24) |
                (s.read() << 16) |
                (s.read() << 8) |
                s.read();
    }

    public static void writeInt(ByteArrayOutputStream s, int v) {
        s.write((v >> 24) & 0xFF);
        s.write((v >> 16) & 0xFF);
        s.write((v >>  8) & 0xFF);
        s.write(v & 0xFF);
    }

    public static void writeString(ByteArrayOutputStream s, String str) {
        if(str == null) {
            ConfigManager.writeShort(s, 0);
            return;
        }

        byte[] bytes = str.getBytes();

        ConfigManager.writeShort(s, bytes.length);
        s.writeBytes(bytes);
    }

    public static String readString(ByteArrayInputStream s) {
        int len = ConfigManager.readShort(s);
        byte[] bytes = new byte[len];

        s.readNBytes(bytes, 0, len);
        return new String(bytes);
    }

    public static void writeUtf8(ByteArrayOutputStream stream, String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        ConfigManager.writeShort(stream, bytes.length);
        stream.writeBytes(bytes);
    }

    public static String readUtf8(ByteArrayInputStream stream) {
        int len = ConfigManager.readShort(stream);
        if (len < 0 || len > stream.available()) return "";
        byte[] bytes = new byte[len];
        stream.readNBytes(bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int savedPanelHeight = -1;

    public static int panelHeight() {
        return ConfigManager.savedPanelHeight;
    }

    public static void setPanelHeight(int height) {
        ConfigManager.savedPanelHeight = height;
    }

    /** Stops when the input runs out, instead of spinning on a short or corrupt stream. */
    public static byte[] inflate(byte[] input) {
        if (input == null) return null;
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(input);
            byte[] buf = new byte[8192];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                if (count <= 0) break;
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            inflater.end();
        }
    }

    public static void configToBytes(ByteArrayOutputStream stream, Configuration config) {
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();

        int modifiedCount = (int) config.getAll()
                .stream()
                .map(config::get)
                .filter(Predicate.not(Value::isDefault))
                .count();

        stream.write(modifiedCount);
        for(String key : config.getAll()) {
            Value<?> value = config.get(key);
            if(value.isDefault()) continue;

            ConfigManager.writeString(stream, key);

            valueStream.reset();
            int valueLen = value.serialize(valueStream);

            ConfigManager.writeShort(stream, valueLen);
            stream.writeBytes(valueStream.toByteArray());
        }
    }

    public static void bytesToConfig(ByteArrayInputStream stream, Configuration config) {
        if(config != null) config.resetToDefaults();

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

    public static void moduleToBytes(ByteArrayOutputStream stream, Module module) {
        ConfigManager.writeString(stream, module.name());
        ConfigManager.writeInt(stream, module.keybind.serialize());
        ConfigManager.configToBytes(stream, module.config);

        int flags = 0;
        if(module.enabled())     flags |= 0x01;
        if(module.showsToasts()) flags |= 0x02;

        stream.write(flags);
    }

    public static void bytesToModule(ByteArrayInputStream stream) {
        String name = ConfigManager.readString(stream);
        Module module = Modules.getByName(name);

        if(module == null) {
            ConfigManager.readInt(stream);
            ConfigManager.bytesToConfig(stream, null);
            int ignored = stream.read();
            return;
        }

        module.keybind.deserialize(ConfigManager.readInt(stream));
        ConfigManager.bytesToConfig(stream, module.config);

        int flags = stream.read();
        Modules.setEnabledSilently(module, (flags & 0x01) != 0);
        module.showsToasts((flags & 0x02) != 0);
    }

    /** Named values with a 16-bit count. The older blob stores that count in one byte. */
    public static void writeConfig(ByteArrayOutputStream stream, Configuration config) {
        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
        int modified = 0;
        for (String key : config.getAll()) {
            if (!config.get(key).isDefault()) modified++;
        }
        ConfigManager.writeShort(stream, modified);
        for (String key : config.getAll()) {
            Value<?> value = config.get(key);
            if (value.isDefault()) continue;
            ConfigManager.writeUtf8(stream, key);
            valueStream.reset();
            value.serialize(valueStream);
            byte[] bytes = valueStream.toByteArray();
            ConfigManager.writeShort(stream, bytes.length);
            stream.writeBytes(bytes);
        }
    }

    public static void readConfig(ByteArrayInputStream stream, Configuration config) {
        if (config != null) config.resetToDefaults();
        int count = ConfigManager.readShort(stream);
        for (int i = 0; i < count; i++) {
            String key = ConfigManager.readUtf8(stream);
            int len = ConfigManager.readShort(stream);
            if (len > stream.available()) return;
            byte[] raw = new byte[len];
            stream.readNBytes(raw, 0, len);
            if (config == null || config.get(key) == null) continue;
            config.get(key).deserialize(new ByteArrayInputStream(raw));
        }
    }

    public static void writeModules(ByteArrayOutputStream stream) {
        List<Module> modules = Modules.getModules();
        ConfigManager.writeShort(stream, modules.size());
        for (Module module : modules) {
            ConfigManager.writeUtf8(stream, module.name());
            ConfigManager.writeInt(stream, module.keybind.serialize());
            ConfigManager.writeConfig(stream, module.config);
            int flags = 0;
            if (module.enabled()) flags |= 0x01;
            if (module.showsToasts()) flags |= 0x02;
            stream.write(flags);
        }
    }

    public static void readModules(ByteArrayInputStream stream) {
        int count = ConfigManager.readShort(stream);
        for (int i = 0; i < count; i++) {
            String name = ConfigManager.readUtf8(stream);
            int bind = ConfigManager.readInt(stream);
            Module module = Modules.getByName(name);
            if (module == null) {
                ConfigManager.readConfig(stream, null);
                stream.read();
                continue;
            }
            module.keybind.deserialize(bind);
            ConfigManager.readConfig(stream, module.config);
            int flags = stream.read();
            Modules.setEnabledSilently(module, (flags & 0x01) != 0);
            module.showsToasts((flags & 0x02) != 0);
        }
    }

    public static void saveState() {
        try {
            String active = ProfileManager.activeProfile();
            if (active != null) ProfileManager.saveProfile(active);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            // Last active account
            ConfigManager.writeString(stream, AccountsManager.getLastAccount());

            // All logged in accounts
            List<AccountStorage> accounts = AccountsManager.getStoredAccounts();
            ConfigManager.writeShort(stream, accounts.size());
            for(AccountStorage stored : accounts) {
                ConfigManager.writeString(stream, stored.name);
                ConfigManager.writeString(stream, stored.type);
                ConfigManager.writeString(stream, stored.username);
                ConfigManager.writeString(stream, stored.uuid);
            }

            // Friends
            List<String> friends = FriendsManager.getFriends();
            ConfigManager.writeShort(stream, friends.size());
            for(String friend : friends) ConfigManager.writeString(stream, friend);

            // Panel positions
            ConfigManager.writeShort(stream, ConfigManager.panelPositions.size());
            for(Map.Entry<String, int[]> entry : ConfigManager.panelPositions.entrySet()) {
                ConfigManager.writeString(stream, entry.getKey());
                ConfigManager.writeInt(stream, entry.getValue()[0]);
                ConfigManager.writeInt(stream, entry.getValue()[1]);
            }

            // Profiles
            List<String> profiles = ProfileManager.profiles();
            ConfigManager.writeShort(stream, profiles.size());
            for(String name : profiles) {
                ConfigManager.writeString(stream, name);
                stream.writeBytes(ProfileManager.profile(name));
            }

            // Main client configs
            ConfigManager.configToBytes(stream, RubyClient.config);

            // Current active profile
            ConfigManager.writeString(stream, ProfileManager.activeProfile());

            // Compress it all
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

            // Prepend the version
            ByteArrayOutputStream finalStream = new ByteArrayOutputStream();
            finalStream.write(ConfigManager.VERSION);
            compressedStream.writeTo(finalStream);

            // Write to file
            File configFile = ConfigManager.configFile();
            Files.createDirectories(configFile.toPath().getParent());
            Files.write(configFile.toPath(), finalStream.toByteArray());
        } catch(Exception e) {
            e.printStackTrace();
            RubyClient.log("Failed to save client configs! " + e.getMessage());
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
            String panelID = ConfigManager.readString(stream);
            int x = ConfigManager.readInt(stream);
            int y = ConfigManager.readInt(stream);

            ConfigManager.setPanelPosition(panelID, x, y);
        }

        return true;
    }

    public static boolean loadV2(ByteArrayInputStream stream) {
        byte[] data = ConfigManager.inflate(stream.readAllBytes());
        if (data == null) return false;
        return ConfigManager.loadV1(new ByteArrayInputStream(data));
    }

    public static boolean loadV3(ByteArrayInputStream stream) {
        byte[] data = ConfigManager.inflate(stream.readAllBytes());
        if (data == null) return false;
        return ConfigManager.readClient(new ByteArrayInputStream(data));
    }

    public static boolean loadV4(ByteArrayInputStream stream) {
        return ConfigManager.loadV3(stream);
    }

    private static boolean readClient(ByteArrayInputStream inner) {
        ConfigManager.bytesToConfig(inner, RubyClient.config);

        // Current active profile
        ProfileManager.setActiveProfile(ConfigManager.readString(inner));

        return true;
    }

    public static int loadState() {
        try {
            File configFile = ConfigManager.configFile();
            if(!configFile.exists()) return ConfigManager.VERSION_ERROR;

            byte[] data = Files.readAllBytes(configFile.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);

            int version = stream.read();
            if(!ConfigManager.loaders.containsKey(version)) return ConfigManager.VERSION_ERROR;
            if(!ConfigManager.loaders.get(version).apply(stream)) return ConfigManager.VERSION_ERROR;

            return version;
        } catch(Exception e) {
            e.printStackTrace();
            return ConfigManager.VERSION_ERROR;
        }
    }
}
