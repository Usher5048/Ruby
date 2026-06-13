package ruby.systems.modules.render;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.render.NametagBotFilter;
import ruby.helpers.render.NametagUtils;
import ruby.helpers.render.RubyNametagRenderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.events.Render2DEvent;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

public class Nametags extends Module {

    private final EntityTypeListValue entities;
    private final DoubleValue scale;
    private final BooleanValue distanceScale;
    private final DoubleValue minScale;
    private final BooleanValue ignoreSelf;
    private final BooleanValue ignoreFriends;
    private final BooleanValue ignoreBots;
    private final BooleanValue displayHealth;
    private final BooleanValue displayDistance;
    private final BooleanValue displayPing;
    private final BooleanValue displayItems;
    private final DoubleValue backgroundOpacity;
    private final ColorValue nameColor;
    private final ColorValue backgroundColor;
    private final NametagBotFilter botFilter = new NametagBotFilter();

    public Nametags() {
        super("Nametags", "Renders customizable nametags.", ModuleType.RENDER);

        entities = config.create(new EntityTypeListValue.Builder("Entities")
                .defaultValue(EntityType.PLAYER).build());
        scale = config.create(new DoubleValue.Builder("Scale").defaultValue(1.1).range(0.1, 3, 0.1).build());
        distanceScale = config.create(new BooleanValue.Builder("Distance Scale")
                .description("Shrink nametags slightly with distance.")
                .defaultValue(true).build());
        minScale = config.create(new DoubleValue.Builder("Min Scale")
                .description("Smallest scale when distance scaling is enabled.")
                .range(0.5, 1.5, 0.05).defaultValue(0.85)
                .visible(distanceScale::value)
                .build());
        ignoreSelf = config.create(new BooleanValue.Builder("Ignore Self").defaultValue(true).build());
        ignoreFriends = config.create(new BooleanValue.Builder("Ignore Friends").defaultValue(false).build());
        ignoreBots = config.create(new BooleanValue.Builder("Ignore Bots")
                .description("Only show nametags for players with a valid gamemode.")
                .defaultValue(true).build());
        displayHealth = config.create(new BooleanValue.Builder("Health").defaultValue(true).build());
        displayDistance = config.create(new BooleanValue.Builder("Distance").defaultValue(false).build());
        displayPing = config.create(new BooleanValue.Builder("Ping").defaultValue(true).build());
        displayItems = config.create(new BooleanValue.Builder("Items")
                .description("Show the item the player is holding.")
                .defaultValue(true).build());
        backgroundOpacity = config.create(new DoubleValue.Builder("Background Opacity")
                .range(0, 1, 0.05).defaultValue(0.92).build());
        nameColor = config.create(new ColorValue.Builder("Name Color").defaultValue(GUIStyle.TEXT_BRIGHT).build());
        backgroundColor = config.create(new ColorValue.Builder("Background").defaultValue(GUIStyle.BG_ELEVATED).build());
    }

    @Override
    public void onDisable() {
        botFilter.clear();
    }

    @Override
    public void tick() {
        if (ignoreBots.value()) botFilter.tick();
    }

    @Override
    public void render2D(Render2DEvent event) {
        if (RubyClient.client.world == null || RubyClient.client.player == null) return;
        if (RubyClient.client.options.hudHidden) return;

        GUIStyle style = GUIStyle.get();
        float tickDelta = RubyClient.client.gameRenderer.getCamera().getLastTickProgress();
        int panelBg = withAlpha(backgroundColor.value(), backgroundOpacity.value().floatValue());
        int panelBorder = GUIStyle.withAlpha(style.borderSubtle(), backgroundOpacity.value().floatValue());

        for (Entity entity : RubyClient.client.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            Vec3d pos = entity.getLerpedPos(tickDelta);
            Vec3d anchor = new Vec3d(pos.x, pos.y + entity.getHeight() + 0.5, pos.z);
            if (!NametagUtils.to2D(anchor, scale.value(), distanceScale.value(), minScale.value())) continue;

            ItemStack held = displayItems.value() && entity instanceof PlayerEntity player
                    ? player.getMainHandStack() : ItemStack.EMPTY;

            RubyNametagRenderer.draw(
                    event.getContext(), style,
                    NametagUtils.getX(), NametagUtils.getY(), NametagUtils.scale,
                    buildTag(entity), nameColor.value(), panelBg, panelBorder, true, held
            );
        }
    }

    private RubyNametagRenderer.TagData buildTag(Entity entity) {
        String name = entity.getName().getString();
        boolean friend = entity instanceof PlayerEntity player && FriendsManager.isFriend(player);

        Integer health = null;
        if (displayHealth.value() && entity instanceof PlayerEntity player) {
            health = Math.round(player.getHealth() + player.getAbsorptionAmount());
        }

        Integer distance = null;
        if (displayDistance.value()) {
            distance = (int) RubyClient.client.player.distanceTo(entity);
        }

        Integer ping = null;
        if (displayPing.value() && entity instanceof PlayerEntity player
                && RubyClient.client.getNetworkHandler() != null) {
            PlayerListEntry entry = RubyClient.client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }

        return new RubyNametagRenderer.TagData(name, health, ping, distance, friend);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (!entities.value().contains(entity.getType())) return false;

        Freecam freecam = Modules.getByClass(Freecam.class);
        boolean freecamActive = freecam != null && freecam.enabled();
        boolean firstPerson = RubyClient.client.options.getPerspective().isFirstPerson();

        if (ignoreSelf.value() && entity == RubyClient.client.player && !freecamActive && firstPerson) return false;
        if (ignoreFriends.value() && entity instanceof PlayerEntity player && FriendsManager.isFriend(player)) return false;
        if (ignoreBots.value() && entity instanceof PlayerEntity player && botFilter.shouldHide(player)) return false;
        return true;
    }

    private static int withAlpha(int color, float alpha) {
        int rgb = color & 0x00FFFFFF;
        return (Math.round(alpha * 255) << 24) | rgb;
    }
}
