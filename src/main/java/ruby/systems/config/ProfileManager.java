package ruby.systems.config;

import ruby.RubyClient;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ProfileManager {
    private static final int PROFILE_VERSION = 3;

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

    private static void applyProfileFromDisk(String normalized) {
        Modules.disableAllSilently();

        File file = ProfileManager.profileFile(normalized);
        if (!file.exists()) {
            RubyClient.LOGGER.debug("Profile file missing for {}", normalized);
            return;
        }

        try {
            byte[] data = Files.readAllBytes(file.toPath());
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            int version = stream.read();
            if (version != 2 && version != PROFILE_VERSION) {
                RubyClient.LOGGER.warn("Unsupported profile version {} for {}", version, normalized);
                return;
            }

            byte[] compressed = stream.readAllBytes();
            if (compressed.length == 0) return;

            Inflater inflater = new Inflater();
            inflater.setInput(compressed);
            byte[] buf = new byte[1024];
            ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
            while (!inflater.finished()) {
                int count = inflater.inflate(buf);
                decompressed.write(buf, 0, count);
            }
            inflater.end();

            ProfileManager.applyProfileData(new ByteArrayInputStream(decompressed.toByteArray()), version);
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
            ProfileManager.serializeModules(stream);

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
            finalStream.write(PROFILE_VERSION);
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

    private static File profileFile(String name) {
        return new File(ProfileManager.profilesDir(), ProfileManager.normalizeName(name) + ".cfg");
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

    private static void applyProfileData(ByteArrayInputStream stream, int version) {
        int moduleCount = ConfigManager.readShortPublic(stream);
        for (int i = 0; i < moduleCount; i++) {
            String modName = ConfigManager.readStringPublic(stream);
            boolean enabled = version >= PROFILE_VERSION && stream.read() == 1;
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
    }
}
