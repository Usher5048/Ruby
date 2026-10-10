package ruby.systems.modules.render;

import ruby.RubyClient;
import ruby.helpers.render.NametagUtils;
import ruby.helpers.render.RenderShapes;
import ruby.helpers.render.RubyNametagRenderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.events.Render2DEvent;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Player;

/**
 * Logout spots for players that disconnect nearby.
 */
public class LogoutSpots extends Module {

    private final DoubleValue scale;
    private final BooleanValue fullHeight;
    private final EnumValue<RenderShapes.ShapeMode> shapeMode;
    private final ColorValue sideColor;
    private final ColorValue lineColor;
    private final ColorValue nameColor;
    private final ColorValue nameBackgroundColor;

    private final List<Entry> players = new ArrayList<>();
    private final List<PlayerInfo> lastPlayerList = new ArrayList<>();
    private final List<Player> lastPlayers = new ArrayList<>();
    private String serverKey;
    private int timer;

    public LogoutSpots() {
        super("Logout Spots", "Displays a box where another player logged out.", ModuleType.RENDER);

        scale = config.create(new DoubleValue.Builder("Scale").defaultValue(1.0).range(0, 3, 0.1).build());
        fullHeight = config.create(new BooleanValue.Builder("Full Height").defaultValue(true).build());
        shapeMode = config.create(new EnumValue.Builder<RenderShapes.ShapeMode>("Shape Mode")
                .defaultValue(RenderShapes.ShapeMode.Both).build());
        sideColor = config.create(new ColorValue.Builder("Side Color").defaultValue(0x37FF00FF).build());
        lineColor = config.create(new ColorValue.Builder("Line Color").defaultValue(0xFFFF00FF).build());
        nameColor = config.create(new ColorValue.Builder("Name Color").defaultValue(GUIStyle.TEXT_BRIGHT).build());
        nameBackgroundColor = config.create(new ColorValue.Builder("Name Background").defaultValue(GUIStyle.BG_ELEVATED).build());
    }

    @Override
    public void onEnable() {
        syncServerSession();
    }

    @Override
    public void onDisable() {
        serverKey = null;
        players.clear();
        lastPlayerList.clear();
        lastPlayers.clear();
    }

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    @Override
    public void tick() {
        Minecraft client = RubyClient.client;
        if (client.level == null || client.getConnection() == null) {
            if (serverKey != null) {
                serverKey = null;
                players.clear();
                lastPlayerList.clear();
                lastPlayers.clear();
            }
            return;
        }

        syncServerSession();

        Collection<PlayerInfo> online = client.getConnection().getOnlinePlayers();
        if (online.size() != lastPlayerList.size()) {
            for (PlayerInfo entry : lastPlayerList) {
                boolean stillOnline = online.stream()
                        .anyMatch(e -> e.getProfile().equals(entry.getProfile()));
                if (stillOnline) continue;

                for (Player player : lastPlayers) {
                    if (player.getUUID().equals(entry.getProfile().id())) {
                        add(new Entry(player));
                    }
                }
            }

            lastPlayerList.clear();
            lastPlayerList.addAll(online);
            updateLastPlayers();
        }

        if (--timer <= 0) {
            updateLastPlayers();
            timer = 10;
        }

        for (Player player : client.level.players()) {
            players.removeIf(entry -> entry.uuid.equals(player.getUUID()));
        }
    }

    private String resolveServerKey() {
        Minecraft client = RubyClient.client;
        if (client.isLocalServer()) {
            if (client.level == null) return null;
            return "sp:" + client.level.dimension().identifier();
        }

        ServerData entry = client.getCurrentServer();
        return entry != null ? entry.ip : null;
    }

    private void syncServerSession() {
        String key = resolveServerKey();
        if (Objects.equals(key, serverKey)) return;

        serverKey = key;
        players.clear();
        lastPlayerList.clear();
        lastPlayers.clear();

        if (key != null && RubyClient.client.getConnection() != null) {
            lastPlayerList.addAll(RubyClient.client.getConnection().getOnlinePlayers());
        }
        updateLastPlayers();
        timer = 10;
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        if (RubyClient.client.level != null) {
            lastPlayers.addAll(RubyClient.client.level.players());
        }
    }

    private void add(Entry entry) {
        players.removeIf(e -> e.uuid.equals(entry.uuid));
        players.add(entry);
    }

    @Override
    public void render3D() {
        for (Entry entry : players) entry.render3D();
    }

    @Override
    public void render2D(Render2DEvent event) {
        GUIStyle style = GUIStyle.get();
        int panelBg = withPanelAlpha(nameBackgroundColor.value());
        int panelBorder = GUIStyle.withAlpha(style.borderSubtle(), ((panelBg >> 24) & 0xFF) / 255f);

        for (Entry entry : players) {
            entry.render2D(event.getContext(), style, panelBg, panelBorder);
        }
    }

    private int withPanelAlpha(int color) {
        int rgb = color & 0x00FFFFFF;
        int alpha = (color >> 24) & 0xFF;
        if (alpha == 0) alpha = 235;
        return (alpha << 24) | rgb;
    }

    private class Entry {
        final double x, y, z, xWidth, zWidth, halfWidth, height;
        final UUID uuid;
        final String name;
        final int health;

        Entry(Player entity) {
            halfWidth = entity.getBbWidth() / 2.0;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;
            xWidth = entity.getBoundingBox().getXsize();
            zWidth = entity.getBoundingBox().getZsize();
            height = entity.getBoundingBox().getYsize();
            uuid = entity.getUUID();
            name = entity.getName().getString();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
        }

        void render3D() {
            if (fullHeight.value()) {
                RenderShapes.box(x, y, z, x + xWidth, y + height, z + zWidth,
                        sideColor.value(), lineColor.value(), shapeMode.value());
            } else {
                RenderShapes.box(x, y, z, x + xWidth, y + 0.05, z + zWidth,
                        sideColor.value(), lineColor.value(), shapeMode.value());
            }
        }

        void render2D(net.minecraft.client.gui.GuiGraphicsExtractor ctx, GUIStyle style, int panelBg, int panelBorder) {
            if (!NametagUtils.to2D(new net.minecraft.world.phys.Vec3(x + halfWidth, y + height + 0.5, z + halfWidth), scale.value())) {
                return;
            }

            RubyNametagRenderer.draw(
                    ctx,
                    style,
                    NametagUtils.getX(),
                    NametagUtils.getY(),
                    NametagUtils.scale,
                    new RubyNametagRenderer.TagData(name, health, null, null, false),
                    nameColor.value(),
                    panelBg,
                    panelBorder,
                    true,
                    net.minecraft.world.item.ItemStack.EMPTY
            );
        }
    }
}
