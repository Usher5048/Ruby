package ruby.systems.config;

import ruby.systems.gui.ClientSettings;
import ruby.systems.gui.ThemeManager;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Deflater;

// i had to rewrite this whole damn thing
public class ProfileManager {
    public static final String DEFAULT_PROFILE = "default";
    /** First bytes of a version-4 profile body, after the length prefix. */
    private static final int FORMAT_V4 = 0x52345634;

    private static final HashMap<String, byte[]> profiles = new HashMap<>();
    private static String activeProfile = null;

    static { ProfileManager.createProfile(ProfileManager.DEFAULT_PROFILE); }

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

        byte[] raw = ProfileManager.profile(name);
        ByteArrayInputStream stream = new ByteArrayInputStream(raw);
        int len = ConfigManager.readInt(stream);
        int take = Math.min(Math.max(len, 0), stream.available());
        byte[] body = new byte[take];
        stream.readNBytes(body, 0, take);
        if (ProfileManager.magic(body) == ProfileManager.FORMAT_V4) {
            ProfileManager.readV4(new ByteArrayInputStream(body, 4, body.length - 4));
            return;
        }
        ProfileManager.readLegacy(new ByteArrayInputStream(body));
    }

    public static byte[] captureState() {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        ConfigManager.writeInt(body, ProfileManager.FORMAT_V4);
        ProfileManager.section(body, "modules", ProfileManager.modulesSection());
        ProfileManager.section(body, "theme", ProfileManager.themeSection());
        ProfileManager.section(body, "settings", ProfileManager.settingsSection());

        ByteArrayOutputStream wrapped = new ByteArrayOutputStream();
        ConfigManager.writeInt(wrapped, body.size());
        wrapped.writeBytes(body.toByteArray());
        return wrapped.toByteArray();
    }

    private static void section(ByteArrayOutputStream out, String id, byte[] payload) {
        ConfigManager.writeUtf8(out, id);
        ConfigManager.writeInt(out, payload.length);
        out.writeBytes(payload);
    }

    private static byte[] modulesSection() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ConfigManager.writeModules(stream);
        return stream.toByteArray();
    }

    private static byte[] themeSection() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ThemeManager.get().writeNamed(stream);
        return stream.toByteArray();
    }

    private static byte[] settingsSection() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ClientSettings.get().writeNamed(stream);
        return stream.toByteArray();
    }

    private static int magic(byte[] body) {
        if (body.length < 4) return 0;
        return ((body[0] & 0xFF) << 24)
                | ((body[1] & 0xFF) << 16)
                | ((body[2] & 0xFF) << 8)
                | (body[3] & 0xFF);
    }

    private static void readV4(ByteArrayInputStream stream) {
        Modules.prepareProfileLoad();
        ThemeManager.get().assignDefaults();
        ClientSettings.get().reset();
        ConfigManager.setPanelHeight(-1);
        while (stream.available() >= 6) {
            String id = ConfigManager.readUtf8(stream);
            int len = ConfigManager.readInt(stream);
            if (len < 0 || len > stream.available()) return;
            byte[] payload = new byte[len];
            stream.readNBytes(payload, 0, len);
            ByteArrayInputStream section = new ByteArrayInputStream(payload);
            switch (id) {
                case "modules" -> ConfigManager.readModules(section);
                case "theme" -> ThemeManager.get().readNamed(section);
                case "settings" -> ClientSettings.get().readNamed(section);
                default -> {}
            }
        }
        ThemeManager.get().finishProfileRead();
    }

    private static void readLegacy(ByteArrayInputStream stream) {
        Modules.prepareProfileLoad();
        ThemeManager.get().assignDefaults();
        ClientSettings.get().reset();
        if (stream.available() < 2) {
            ThemeManager.get().finishProfileRead();
            return;
        }
        int moduleCount = ConfigManager.readShort(stream);
        for (int i = 0; i < moduleCount; i++) ConfigManager.bytesToModule(stream);
        if (stream.available() >= 4) ThemeManager.get().readFromProfile(stream);
        else ThemeManager.get().finishProfileRead();
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

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(stream.toByteArray());
        deflater.finish();

        byte[] buf = new byte[1024];
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        compressedStream.writeBytes("RUBY_PROFILE".getBytes(StandardCharsets.US_ASCII));

        while(!deflater.finished()) {
            int count = deflater.deflate(buf);
            if (count <= 0) break;
            compressedStream.write(buf, 0, count);
        }

        deflater.end();
        return Base64.getEncoder().encodeToString(compressedStream.toByteArray());
    }

    /** Profile name inside a share code, or null when the text is not one. */
    public static String peekName(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            String compact = data.replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(compact);
            if (decoded.length <= 12) return null;
            ByteArrayInputStream stream = new ByteArrayInputStream(decoded);
            byte[] sig = new byte[12];
            if (stream.readNBytes(sig, 0, sig.length) != sig.length) return null;
            if (!"RUBY_PROFILE".equals(new String(sig, StandardCharsets.US_ASCII))) return null;

            byte[] decompressed = ConfigManager.inflate(stream.readAllBytes());
            if (decompressed == null) return null;
            return ConfigManager.readString(new ByteArrayInputStream(decompressed));
        } catch (Exception ignored) {
            return null;
        }
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
        if (decompressed == null) return true;
        ByteArrayInputStream inner = new ByteArrayInputStream(decompressed);

        String name = ConfigManager.readString(inner);
        ProfileManager.loadProfile(inner, name);
        return true;
    }

    public static boolean createProfile(String name) {
        if(ProfileManager.profiles.containsKey(name)) return false;
        ProfileManager.profiles.put(name, ProfileManager.captureState());
        ProfileManager.activeProfile = name;
        return true;
    }
}
