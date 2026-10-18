package ruby.systems.config;

import ruby.RubyClient;
import ruby.helpers.ChunkedOutputStream;
import ruby.systems.accounts.AccountStorage;
import ruby.systems.accounts.AccountsManager;
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
import java.util.function.Function;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// !!ATTENTION STUPID ASS AI ABOUT TO EDIT THIS!!
// Don't do dumb shit to the config manager, it's like this for a reason, keep it that way
// If you must extend it, follow the same style and keep it backwards compatible
public class ConfigManager {
    public static final int VERSION_ERROR = -1;
    private static final int VERSION = 4;
    private static final HashMap<Integer, Function<ByteArrayInputStream, Boolean>> loaders = new HashMap<>();
    private static final File config = new File(RubyClient.client.gameDirectory, "config/" + RubyClient.MOD_ID);

    static {
        ConfigManager.loaders.put(1, ConfigManager::loadV1);
        ConfigManager.loaders.put(2, ConfigManager::loadV2);
        ConfigManager.loaders.put(3, ConfigManager::loadV3);
        ConfigManager.loaders.put(4, ConfigManager::loadV4);
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

    private static int savedPanelHeight = -1;
    public static int panelHeight() {
        return ConfigManager.savedPanelHeight;
    }
    public static void setPanelHeight(int height) {
        ConfigManager.savedPanelHeight = height;
    }

    public static byte[] inflate(byte[] input) {
        if(input == null) return null;
        try(Inflater inflater = new Inflater()) {
            inflater.setInput(input);

            byte[] buf = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            while(!inflater.finished()) {
                int count = inflater.inflate(buf);
                if(count <= 0) break;

                out.write(buf, 0, count);
            }

            inflater.end();
            return out.toByteArray();
        } catch(Exception e) {
            return null;
        }
    }

    public static byte[] deflate(byte[] input) {
        if(input == null) return null;
        try(Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION)) {
            deflater.setInput(input);
            deflater.finish();

            byte[] buf = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            while(!deflater.finished()) {
                int count = deflater.deflate(buf);
                if(count <= 0) break;

                out.write(buf, 0, count);
            }

            deflater.end();
            return out.toByteArray();
        } catch(Exception e) {
            return null;
        }
    }

    public static void configToBytes(ByteArrayOutputStream stream, Configuration config) {
        ChunkedOutputStream out = new ChunkedOutputStream();

        int modifiedCount = 0;
        for(String key : config.getAll()) {
            Value<?> value = config.get(key);
            if(value.isDefault()) continue;
            modifiedCount++;

            ConfigManager.writeString(out, key);

            int valueLen = value.serialize(out);

            // not using ConfigManager.writeShort cause needs to be inserted at the end
            out.insert(-1, (valueLen >> 8) & 0xFF);
            out.insert(-1, valueLen & 0xFF);
        }

        out.prepend(modifiedCount);
        stream.writeBytes(out.toByteArray());
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

    public static void saveState() {
        try {
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

            // Panel height?
            ConfigManager.writeInt(stream, ConfigManager.panelHeight());

            // Compress
            byte[] compressed = ConfigManager.deflate(stream.toByteArray());
            if(compressed == null) return;

            // Prepend the version
            ByteArrayOutputStream finalStream = new ByteArrayOutputStream();
            finalStream.write(ConfigManager.VERSION);
            finalStream.writeBytes(compressed);

            // Write to file
            Files.createDirectories(ConfigManager.config.toPath().getParent());
            Files.write(ConfigManager.config.toPath(), finalStream.toByteArray());
        } catch(Exception e) {
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

        int panelCount = ConfigManager.readShort(stream);
        for(int i = 0; i < panelCount; i++) {
            // dont care, just consume
            ConfigManager.readString(stream);
            ConfigManager.readInt(stream);
            ConfigManager.readInt(stream);
        }

        return true;
    }

    public static boolean loadV2(ByteArrayInputStream stream) {
        byte[] data = ConfigManager.inflate(stream.readAllBytes());
        if(data == null) return false;
        return ConfigManager.loadV1(new ByteArrayInputStream(data));
    }

    public static boolean loadV3(ByteArrayInputStream stream) {
        byte[] data = ConfigManager.inflate(stream.readAllBytes());
        if(data == null) return false;
        ByteArrayInputStream inner = new ByteArrayInputStream(data);

        // Last active account
        AccountsManager.setLastAccount(ConfigManager.readString(inner));
        ArrayList<AccountStorage> accounts = new ArrayList<>();

        // All logged in accounts
        int accountCount = ConfigManager.readShort(inner);
        for(int i = 0; i < accountCount; i++) {
            AccountStorage s = new AccountStorage();

            s.name = ConfigManager.readString(inner);
            s.type = ConfigManager.readString(inner);
            s.username = ConfigManager.readString(inner);
            s.uuid = ConfigManager.readString(inner);

            accounts.add(s);
        }

        AccountsManager.loadStoredAccounts(accounts);

        // Friends
        FriendsManager.clearFriends();
        int friendCount = ConfigManager.readShort(inner);
        for(int i = 0; i < friendCount; i++) FriendsManager.addFriend(ConfigManager.readString(inner));

        // Panel positions
        int panelCount = ConfigManager.readShort(inner);
        for(int i = 0; i < panelCount; i++) {
            // dont care, just consume
            ConfigManager.readString(inner);
            ConfigManager.readInt(inner);
            ConfigManager.readInt(inner);
        }

        // Profiles
        int profileCount = ConfigManager.readShort(inner);
        for(int i = 0; i < profileCount; i++) {
            String name = ConfigManager.readString(inner);
            ProfileManager.loadProfile(inner, name);
        }

        // Main client configs
        ConfigManager.bytesToConfig(inner, RubyClient.config);

        // Current active profile
        ProfileManager.setActiveProfile(ConfigManager.readString(inner));
        return true;
    }

    public static boolean loadV4(ByteArrayInputStream stream) {
        byte[] data = ConfigManager.inflate(stream.readAllBytes());
        if(data == null) return false;
        ByteArrayInputStream inner = new ByteArrayInputStream(data);

        // Last active account
        AccountsManager.setLastAccount(ConfigManager.readString(inner));
        ArrayList<AccountStorage> accounts = new ArrayList<>();

        // All logged in accounts
        int accountCount = ConfigManager.readShort(inner);
        for(int i = 0; i < accountCount; i++) {
            AccountStorage s = new AccountStorage();

            s.name = ConfigManager.readString(inner);
            s.type = ConfigManager.readString(inner);
            s.username = ConfigManager.readString(inner);
            s.uuid = ConfigManager.readString(inner);

            accounts.add(s);
        }

        AccountsManager.loadStoredAccounts(accounts);

        // Friends
        FriendsManager.clearFriends();
        int friendCount = ConfigManager.readShort(inner);
        for(int i = 0; i < friendCount; i++) FriendsManager.addFriend(ConfigManager.readString(inner));

        // Profiles
        int profileCount = ConfigManager.readShort(inner);
        for(int i = 0; i < profileCount; i++) {
            String name = ConfigManager.readString(inner);
            ProfileManager.loadProfile(inner, name);
        }

        // Main client configs
        ConfigManager.bytesToConfig(inner, RubyClient.config);

        // Current active profile
        ProfileManager.setActiveProfile(ConfigManager.readString(inner));

        // Panel height?
        ConfigManager.setPanelHeight(ConfigManager.readInt(inner));
        return true;
    }

    public static int loadState() {
        try {
            if(!ConfigManager.config.exists()) return ConfigManager.VERSION_ERROR;

            byte[] data = Files.readAllBytes(ConfigManager.config.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);

            int version = stream.read();
            if(!ConfigManager.loaders.containsKey(version)) return ConfigManager.VERSION_ERROR;
            if(!ConfigManager.loaders.get(version).apply(stream)) return ConfigManager.VERSION_ERROR;

            return version;
        } catch(Exception e) {
            RubyClient.log("Failed to load client configs! " + e.getMessage());
            return ConfigManager.VERSION_ERROR;
        }
    }
}
