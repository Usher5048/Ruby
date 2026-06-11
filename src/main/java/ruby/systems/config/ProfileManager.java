package ruby.systems.config;

import ruby.RubyClient;
import ruby.systems.gui.ThemeManager;
import ruby.systems.modules.Keybind;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ProfileManager {
    public static final String SHARE_PREFIX = "ruby:";
    private static final int PROFILE_VERSION = 4;

    private static String activeProfile = "default";
    private static final List<String> profiles = new ArrayList<>(List.of("default"));
    private static boolean migratedProfiles;

    private ProfileManager() {}

    public static File profilesDir() {
        File dir = new File(RubyClient.client.runDirectory, "config/" + RubyClient.MOD_ID + "_profiles");
        if (!ProfileManager.migratedProfiles) {
            ProfileManager.migratedProfiles = true;
            ProfileManager.migrateLegacyProfiles(dir);
        }
        dir.mkdirs();
        return dir;
    }

    private static void migrateLegacyProfiles(File targetDir) {
        targetDir.mkdirs();
        File nested = new File(RubyClient.client.runDirectory, "config/" + RubyClient.MOD_ID + "/profiles");
        if (!nested.isDirectory()) return;

        File[] files = nested.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".cfg"));
        if (files == null) return;

        for (File file : files) {
            File dest = new File(targetDir, file.getName());
            if (dest.exists()) continue;
            try {
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                RubyClient.LOGGER.warn("Failed to migrate profile {}", file.getName(), e);
            }
        }
    }

    public static String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_]", "");
    }

    public static String getActiveProfile() {
        return ProfileManager.activeProfile;
    }

    public static void setActiveProfile(String name) {
        if (name == null || name.isBlank()) return;
        String normalized = ProfileManager.normalizeName(name);
        if (normalized.isEmpty()) return;
        ProfileManager.activeProfile = normalized;
        if (!ProfileManager.profiles.contains(normalized)) {
            ProfileManager.profiles.add(normalized);
            ProfileManager.sortProfiles();
        }
    }

    public static List<String> getProfiles() {
        return Collections.unmodifiableList(ProfileManager.profiles);
    }

    public static void refreshProfileList() {
        File dir = ProfileManager.profilesDir();
        ProfileManager.profiles.clear();
        ProfileManager.profiles.add("default");

        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".cfg"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                if (!ProfileManager.profiles.contains(name)) {
                    ProfileManager.profiles.add(name);
                }
            }
        }

        ProfileManager.sortProfiles();

        if (!ProfileManager.profiles.contains(ProfileManager.activeProfile)) {
            ProfileManager.activeProfile = "default";
        }
    }

    private static void sortProfiles() {
        ProfileManager.profiles.sort((a, b) -> {
            if ("default".equals(a)) return -1;
            if ("default".equals(b)) return 1;
            return a.compareTo(b);
        });
    }

    public static void switchProfile(String name) {
        String normalized = ProfileManager.normalizeName(name);
        if (normalized.isEmpty()) return;

        String previous = ProfileManager.activeProfile;
        if (!previous.equals(normalized)) {
            ProfileManager.saveProfile(previous);
        }

        ProfileManager.setActiveProfile(normalized);
        ProfileManager.applyProfileFromDisk(normalized);
    }

    public static void loadProfile(String name) {
        ProfileManager.setActiveProfile(name);
        ProfileManager.applyProfileFromDisk(ProfileManager.normalizeName(name));
    }

    public static boolean isShareCode(String input) {
        if (input == null) return false;
        String trimmed = input.trim();
        if (trimmed.startsWith(ProfileManager.SHARE_PREFIX)) return true;
        return trimmed.length() >= 48 && trimmed.matches("^[A-Za-z0-9+/=_\\-]+$");
    }

    public static String exportShareCode(String name) {
        String normalized = ProfileManager.normalizeName(name);
        if (normalized.isEmpty()) return null;

        ProfileManager.saveProfile(normalized);
        try {
            byte[] data = Files.readAllBytes(ProfileManager.profileFile(normalized).toPath());
            return ProfileManager.SHARE_PREFIX + Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to export profile {}", normalized, e);
            return null;
        }
    }

    public static String importProfile(String input) {
        if (!ProfileManager.isShareCode(input)) return null;

        String encoded = input.trim();
        if (encoded.startsWith(ProfileManager.SHARE_PREFIX)) {
            encoded = encoded.substring(ProfileManager.SHARE_PREFIX.length());
        }

        try {
            byte[] fileData = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
            ByteArrayInputStream stream = new ByteArrayInputStream(fileData);
            int version = stream.read();
            if (version < 2 || version > ProfileManager.PROFILE_VERSION) return null;

            byte[] compressed = stream.readAllBytes();
            if (compressed.length == 0) return null;

            byte[] payload = ProfileManager.decompress(compressed);
            ByteArrayInputStream payloadStream = new ByteArrayInputStream(payload);

            String profileName;
            if (version >= 4) {
                profileName = ProfileManager.normalizeName(ConfigManager.readStringPublic(payloadStream));
            } else {
                return null;
            }

            if (profileName.isEmpty() || "default".equals(profileName)) return null;

            ProfileManager.profilesDir().mkdirs();
            Files.write(ProfileManager.profileFile(profileName).toPath(), fileData);

            if (!ProfileManager.profiles.contains(profileName)) {
                ProfileManager.profiles.add(profileName);
                ProfileManager.sortProfiles();
            }

            ProfileManager.setActiveProfile(profileName);
            ProfileManager.resetAllModules();
            ProfileManager.applyProfileData(payloadStream, version, false);
            return profileName;
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to import profile", e);
            return null;
        }
    }

    private static void applyProfileFromDisk(String normalized) {
        ProfileManager.resetAllModules();

        File file = ProfileManager.profileFile(normalized);
        if (!file.exists()) {
            RubyClient.LOGGER.debug("Profile file missing for {}", normalized);
            return;
        }

        try {
            byte[] data = Files.readAllBytes(file.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            int version = stream.read();
            if (version < 2 || version > ProfileManager.PROFILE_VERSION) {
                RubyClient.LOGGER.warn("Unsupported profile version {} for {}", version, normalized);
                return;
            }

            byte[] compressed = stream.readAllBytes();
            if (compressed.length == 0) return;

            ProfileManager.applyProfileData(
                    new ByteArrayInputStream(ProfileManager.decompress(compressed)),
                    version,
                    true
            );
            RubyClient.LOGGER.debug("Loaded profile {}", normalized);
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to load profile {}", normalized, e);
        }
    }

    public static boolean saveProfile(String name) {
        String normalized = ProfileManager.normalizeName(name);
        if (normalized.isEmpty()) return false;

        ProfileManager.profilesDir().mkdirs();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ProfileManager.serializePayload(stream, normalized);

            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(stream.toByteArray());
            deflater.finish();

            byte[] buf = new byte[1024];
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            while (!deflater.finished()) {
                int count = deflater.deflate(buf);
                compressed.write(buf, 0, count);
            }
            deflater.end();

            ByteArrayOutputStream finalStream = new ByteArrayOutputStream();
            finalStream.write(ProfileManager.PROFILE_VERSION);
            compressed.writeTo(finalStream);
            Files.write(ProfileManager.profileFile(normalized).toPath(), finalStream.toByteArray());

            if (!ProfileManager.profiles.contains(normalized)) {
                ProfileManager.profiles.add(normalized);
                ProfileManager.sortProfiles();
            }
            return true;
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to save profile {}", normalized, e);
            return false;
        }
    }

    public static void deleteProfile(String name) {
        if ("default".equalsIgnoreCase(name)) return;
        String normalized = ProfileManager.normalizeName(name);
        ProfileManager.profileFile(normalized).delete();
        ProfileManager.profiles.remove(normalized);
        if (ProfileManager.activeProfile.equalsIgnoreCase(normalized)) {
            ProfileManager.switchProfile("default");
        }
    }

    public static boolean createProfile(String name) {
        String normalized = ProfileManager.normalizeName(name);
        if (normalized.isEmpty() || "default".equals(normalized)) return false;
        if (ProfileManager.profiles.contains(normalized)) return false;
        if (!ProfileManager.saveProfile(normalized)) return false;
        ProfileManager.setActiveProfile(normalized);
        return ProfileManager.profiles.contains(normalized);
    }

    public static boolean addProfile(String input) {
        if (input == null || input.isBlank()) return false;

        String imported = ProfileManager.importProfile(input);
        if (imported != null) return true;

        return ProfileManager.createProfile(input);
    }

    private static File profileFile(String name) {
        return new File(ProfileManager.profilesDir(), ProfileManager.normalizeName(name) + ".cfg");
    }

    private static byte[] decompress(byte[] compressed) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        byte[] buf = new byte[1024];
        ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
        while (!inflater.finished()) {
            int count = inflater.inflate(buf);
            decompressed.write(buf, 0, count);
        }
        inflater.end();
        return decompressed.toByteArray();
    }

    private static void resetAllModules() {
        Modules.disableAllSilently();
        for (Module module : Modules.getModules()) {
            module.keybind = Keybind.unbound();
            module.config.resetToDefaults();
        }
    }

    private static void serializePayload(ByteArrayOutputStream stream, String profileName) {
        ConfigManager.writeStringPublic(stream, profileName);
        ProfileManager.serializeModules(stream);
        ThemeManager.get().writeToProfile(stream);
    }

    private static void serializeModules(ByteArrayOutputStream stream) {
        ConfigManager.writeShortPublic(stream, Modules.getModules().size());
        for (Module module : Modules.getModules()) {
            ConfigManager.writeStringPublic(stream, module.name());
            stream.write(module.enabled() ? 1 : 0);
            ConfigManager.writeIntPublic(stream, module.keybind.serialize());
            ConfigManager.configToBytesPublic(stream, module.config);
        }
    }

    private static void applyProfileData(ByteArrayInputStream stream, int version, boolean readEmbeddedName) {
        if (version >= 4 && readEmbeddedName) {
            ConfigManager.readStringPublic(stream);
        }

        int moduleCount = ConfigManager.readShortPublic(stream);
        for (int i = 0; i < moduleCount; i++) {
            String modName = ConfigManager.readStringPublic(stream);
            boolean enabled = version >= 3 && stream.read() == 1;
            int keybind = ConfigManager.readIntPublic(stream);

            Module module = Modules.getByName(modName);
            if (module == null) {
                ConfigManager.bytesToConfigPublic(stream, null);
                continue;
            }

            module.keybind.deserialize(keybind);
            ConfigManager.bytesToConfigPublic(stream, module.config);
            Modules.setEnabledSilently(module, enabled);
        }

        if (version >= 4 && stream.available() > 0) {
            ThemeManager.get().readFromProfile(stream);
        }
    }
}
