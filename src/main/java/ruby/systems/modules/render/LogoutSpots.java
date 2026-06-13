package ruby.systems.modules.render;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.UUID;

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
    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();
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
        players.clear();
        lastPlayerList.clear();
        if (RubyClient.client.getNetworkHandler() != null) {
            lastPlayerList.addAll(RubyClient.client.getNetworkHandler().getPlayerList());
        }
        updateLastPlayers();
        timer = 10;
    }

    @Override
    public void onDisable() {
        players.clear();
        lastPlayerList.clear();
    }

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    @Override
    public void tick() {
        if (RubyClient.client.world == null || RubyClient.client.getNetworkHandler() == null) return;

        Collection<PlayerListEntry> online = RubyClient.client.getNetworkHandler().getPlayerList();
        if (online.size() != lastPlayerList.size()) {
            for (PlayerListEntry entry : lastPlayerList) {
                boolean stillOnline = online.stream()
                        .anyMatch(e -> e.getProfile().equals(entry.getProfile()));
                if (stillOnline) continue;

                for (PlayerEntity player : lastPlayers) {
                    if (player.getUuid().equals(entry.getProfile().id())) {
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

        for (PlayerEntity player : RubyClient.client.world.getPlayers()) {
            players.removeIf(entry -> entry.uuid.equals(player.getUuid()));
        }
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        if (RubyClient.client.world != null) {
            lastPlayers.addAll(RubyClient.client.world.getPlayers());
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

        Entry(PlayerEntity entity) {
            halfWidth = entity.getWidth() / 2.0;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;
            xWidth = entity.getBoundingBox().getLengthX();
            zWidth = entity.getBoundingBox().getLengthZ();
            height = entity.getBoundingBox().getLengthY();
            uuid = entity.getUuid();
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

        void render2D(net.minecraft.client.gui.DrawContext ctx, GUIStyle style, int panelBg, int panelBorder) {
            if (!NametagUtils.to2D(new net.minecraft.util.math.Vec3d(x + halfWidth, y + height + 0.5, z + halfWidth), scale.value())) {
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
                    net.minecraft.item.ItemStack.EMPTY
            );
        }
    }
}
