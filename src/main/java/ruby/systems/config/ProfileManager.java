package ruby.systems.config;

import ruby.helpers.ChunkedOutputStream;
import ruby.systems.gui.ClientSettings;
import ruby.systems.gui.ThemeManager;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

// i had to rewrite this whole damn thing
public class ProfileManager {
    private static final int VERSION = 1;
    private static final HashMap<Integer, Consumer<ByteArrayInputStream>> activators = new HashMap<>();
    private static final HashMap<String, byte[]> profiles = new HashMap<>();

    public static final String DEFAULT_PROFILE = "default";
    private static String activeProfile = null;

    static {
        ProfileManager.createProfile(ProfileManager.DEFAULT_PROFILE);

        ProfileManager.activators.put(1, ProfileManager::activateV1);
    }

    private static void activateV1(ByteArrayInputStream stream) {
        Modules.disableAllSilently();
        Modules.getModules().forEach(m -> {
            m.config.resetToDefaults();
            m.showsToasts(true);
            m.keybind.unbind();
        });

        int moduleCount = stream.read();
        for(int i = 0; i < moduleCount; i++) ConfigManager.bytesToModule(stream);

        ThemeManager.get().readFromProfile(stream);
        ClientSettings.get().read(stream);
    }

    public static List<String> profiles() {
        return ProfileManager.profiles.keySet().stream().toList();
    }
    public static byte[] profile(String name) {
        return ProfileManager.profiles.get(name);
    }

    public static String activeProfile() {
        return ProfileManager.activeProfile;
    }
    public static void setActiveProfile(String name) {
        if(!ProfileManager.profiles.containsKey(name)) return;
        ProfileManager.activeProfile = name;

        ByteArrayInputStream stream = new ByteArrayInputStream(ProfileManager.profile(name));
        ConfigManager.readInt(stream); // len, skip

        int version = stream.read();
        if(!ProfileManager.activators.containsKey(version)) return;

        ProfileManager.activators.get(version).accept(stream);
    }

    public static byte[] captureState() {
        ChunkedOutputStream body = new ChunkedOutputStream();

        int modified = 0;
        for(Module module : Modules.getModules()) {
            if(module.isDefault()) continue;
            ConfigManager.moduleToBytes(body, module);
            modified++;
        }

        body.prepend(modified);
        body.prepend(ProfileManager.VERSION);

        ThemeManager.get().writeToProfile(body);
        ClientSettings.get().write(body);

        ByteArrayOutputStream wrapped = new ByteArrayOutputStream();
        ConfigManager.writeInt(wrapped, body.size());
        wrapped.writeBytes(body.toByteArray());
        return wrapped.toByteArray();
    }

    public static void loadProfile(ByteArrayInputStream stream, String name) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int len = ConfigManager.readInt(stream);
        ConfigManager.writeInt(out, len);

        byte[] data = new byte[len];
        stream.readNBytes(data, 0, len);

        out.writeBytes(data);
        ProfileManager.profiles.put(name, out.toByteArray());
    }

    public static String exportProfile(String name) {
        if(!ProfileManager.profiles.containsKey(name)) return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ConfigManager.writeString(stream, name);
        stream.writeBytes(ProfileManager.profiles.get(name));

        byte[] compressed = ConfigManager.deflate(stream.toByteArray());
        if(compressed == null) return "Profile export failed!";

        ByteArrayOutputStream finalStream = new ByteArrayOutputStream();
        finalStream.writeBytes("RUBY_PROFILE".getBytes(StandardCharsets.US_ASCII));
        finalStream.writeBytes(compressed);

        return Base64.getEncoder().encodeToString(finalStream.toByteArray());
    }

    public static void saveProfile(String name) {
        if(!ProfileManager.profiles.containsKey(name)) return;
        ProfileManager.profiles.put(name, ProfileManager.captureState());
    }

    public static void deleteProfile(String name) {
        ProfileManager.profiles.remove(name);
    }
    public static boolean importProfile(String data) { // returns true on sig match, not if valid
        ByteArrayInputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(data));

        byte[] sig = new byte[12];
        stream.readNBytes(sig, 0, sig.length);
        if(!(new String(sig, StandardCharsets.US_ASCII).equals("RUBY_PROFILE")))
            return false;

        byte[] decompressed = ConfigManager.inflate(stream.readAllBytes());
        if(decompressed == null) return true;
        ByteArrayInputStream inner = new ByteArrayInputStream(decompressed);

        String name = ConfigManager.readString(inner);
        ProfileManager.loadProfile(inner, name);
        return true;
    }

    public static String peekName(String data) {
        if(data == null || data.isEmpty()) return null;

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(data));

            byte[] sig = new byte[12];
            stream.readNBytes(sig, 0, sig.length);
            if(!(new String(sig, StandardCharsets.US_ASCII).equals("RUBY_PROFILE")))
                return null;

            byte[] decompressed = ConfigManager.inflate(stream.readAllBytes());
            if(decompressed == null) return null;

            ByteArrayInputStream inner = new ByteArrayInputStream(decompressed);
            return ConfigManager.readString(inner);
        } catch(Exception e) {
            return null;
        }
    }

    public static boolean createProfile(String name) {
        if(ProfileManager.profiles.containsKey(name)) return false;
        ProfileManager.profiles.put(name, ProfileManager.captureState());
        ProfileManager.activeProfile = name;
        return true;
    }
}
