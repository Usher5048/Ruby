package ruby.systems.config;

import ruby.systems.gui.ThemeManager;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// i had to rewrite this whole damn thing
public class ProfileManager {
    public static final String DEFAULT_PROFILE = "default";

    private static final HashMap<String, byte[]> profiles = new HashMap<>();
    private static String activeProfile = null;

    static { ProfileManager.importProfile(ProfileManager.DEFAULT_PROFILE); }

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
        if(name.equals(ProfileManager.activeProfile)) return;
        ProfileManager.activeProfile = name;

        ByteArrayInputStream stream = new ByteArrayInputStream(ProfileManager.profile(name));
        ConfigManager.readInt(stream); // length for streams, can skip here

        int moduleCount = ConfigManager.readShort(stream);
        for(int i = 0; i < moduleCount; i++) ConfigManager.bytesToModule(stream);

        ThemeManager.get().readFromProfile(stream);
    }

    public static byte[] captureState() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        ConfigManager.writeShort(stream, Modules.getModules().size());
        for(Module module : Modules.getModules()) ConfigManager.moduleToBytes(stream, module);

        ThemeManager.get().writeToProfile(stream);

        ByteArrayOutputStream len = new ByteArrayOutputStream();
        ConfigManager.writeInt(len, stream.size());
        len.writeBytes(stream.toByteArray());

        return len.toByteArray();
    }

    public static void loadProfile(ByteArrayInputStream stream, String name) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int len = ConfigManager.readInt(stream);
        ConfigManager.writeInt(out, len);

        try {
            byte[] data = stream.readNBytes(len);
            out.writeBytes(data);
            ProfileManager.profiles.put(name, out.toByteArray());
        } catch(Exception ignored) {}
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
            compressedStream.write(buf, 0, count);
        }

        deflater.end();
        return Base64.getEncoder().encodeToString(compressedStream.toByteArray());
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
        try {
            byte[] sig = stream.readNBytes(12);
            if(!(new String(sig, StandardCharsets.US_ASCII).equals("RUBY_PROFILE")))
                return false;
        } catch(Exception ignored) { return false; }

        Inflater inflater = new Inflater();
        inflater.setInput(stream.readAllBytes());

        byte[] buf = new byte[1024];
        ByteArrayOutputStream decompressed = new ByteArrayOutputStream();

        while(!inflater.finished()) {
            try {
                int count = inflater.inflate(buf);
                decompressed.write(buf, 0, count);
            } catch(Exception ignored) { return true; }
        }

        inflater.end();
        ByteArrayInputStream inner = new ByteArrayInputStream(decompressed.toByteArray());

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
