package ruby.systems.social;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FriendsManager {
    private static final List<String> friends = new ArrayList<>();

    private FriendsManager() {}

    public static List<String> getFriends() {
        return Collections.unmodifiableList(FriendsManager.friends);
    }

    public static void setFriends(List<String> names) {
        FriendsManager.friends.clear();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                FriendsManager.friends.add(name.trim());
            }
        }
    }

    public static void clearFriends() {
        FriendsManager.friends.clear();
    }

    public static boolean addFriend(String name) {
        if (name == null || name.isBlank()) return false;
        String trimmed = name.trim();
        for (String existing : FriendsManager.friends) {
            if (existing.equalsIgnoreCase(trimmed)) return false;
        }
        FriendsManager.friends.add(trimmed);
        return true;
    }

    public static void removeFriend(int index) {
        if (index < 0 || index >= FriendsManager.friends.size()) return;
        FriendsManager.friends.remove(index);
    }

    public static String capitalizeProfileName(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    public static boolean isFriend(PlayerEntity player) {
        String name = player.getGameProfile().name();
        return FriendsManager.isFriend(name);
    }

    public static boolean isFriend(String name) {
        if(name == null || name.isBlank()) return false;

        for(String friend : FriendsManager.friends)
            if(friend.equalsIgnoreCase(name.trim())) return true;

        return false;
    }
}
