package ruby.systems.modules.render;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
                .defaultValue(EntityTypes.PLAYER).build());
        displayHealth = config.create(new BooleanValue.Builder("Health").defaultValue(true).build());
        displayPing = config.create(new BooleanValue.Builder("Ping").defaultValue(true).build());
        displayDistance = config.create(new BooleanValue.Builder("Distance").defaultValue(false).build());
        displayItems = config.create(new BooleanValue.Builder("Items")
                .description("Show the item the player is holding, inside the tag.")
                .defaultValue(true).build());
        ignoreSelf = config.create(new BooleanValue.Builder("Ignore Self").defaultValue(true).build());
        ignoreFriends = config.create(new BooleanValue.Builder("Ignore Friends").defaultValue(false).build());
        ignoreBots = config.create(new BooleanValue.Builder("Ignore Bots")
                .description("Only show nametags for players with a valid gamemode.")
                .defaultValue(true).build());
        scale = config.create(new DoubleValue.Builder("Scale")
                .description("1 is the sharp size. Larger values stretch the text.")
                .defaultValue(1).range(0.5, 2, 0.05).build());
        distanceScale = config.create(new BooleanValue.Builder("Distance Scale")
                .description("Shrinks the tag as the target gets farther away.")
                .defaultValue(false).build());
        minScale = config.create(new DoubleValue.Builder("Min Scale")
                .description("Smallest the tag gets at long range.")
                .range(0.5, 1, 0.01).defaultValue(0.5)
                .visible(distanceScale::value)
                .build());
        backgroundOpacity = config.create(new DoubleValue.Builder("Background Opacity")
                .range(0, 1, 0.05).defaultValue(0.92).build());
        nameColor = config.create(new ColorValue.Builder("Name Color").defaultValue(GUIStyle.TEXT_BRIGHT).build());
        backgroundColor = config.create(new ColorValue.Builder("Background").defaultValue(0xFF090809).build());
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
        if (RubyClient.client.level == null || RubyClient.client.player == null) return;
        this.loosenOldMin();

        GUIStyle style = GUIStyle.get();
        float tickDelta = ruby.helpers.render.Renderer.partialTicks();
        float opacity = backgroundOpacity.value().floatValue();
        int panelBg = withAlpha(backgroundColor.value(), opacity);
        int panelBorder = GUIStyle.withAlpha(style.borderPanel(), opacity);
        int screenW = RubyClient.client.getWindow().getWidth();
        int screenH = RubyClient.client.getWindow().getHeight();

        List<Drawn> tags = new ArrayList<>();
        for (Entity entity : RubyClient.client.level.entitiesForRendering()) {
            if (!shouldRender(entity)) continue;

            Vec3 pos = entity.getPosition(tickDelta);
            Vec3 anchor = new Vec3(pos.x, pos.y + entity.getBbHeight() + 0.35, pos.z);
            double sized = this.scale.value();
            if (!NametagUtils.to2D(anchor, sized, this.distanceScale.value(), this.minScale.value())) continue;
            sized = NametagUtils.scale;
            double sx = NametagUtils.getX();
            double sy = NametagUtils.getY();
            if (sx < -64 || sy < -32 || sx > screenW + 64 || sy > screenH + 32) continue;

            ItemStack held = displayItems.value() && entity instanceof Player player
                    ? player.getMainHandItem() : ItemStack.EMPTY;
            tags.add(new Drawn(
                    RubyClient.client.player.distanceToSqr(entity),
                    sx, sy, sized,
                    buildTag(entity), held
            ));
        }

        tags.sort(Comparator.comparingDouble(Drawn::dist).reversed());
        int color = nameColor.value();
        for (Drawn tag : tags) {
            RubyNametagRenderer.draw(
                    event.getContext(), style,
                    tag.x, tag.y, tag.scale,
                    tag.data, color, panelBg, panelBorder, tag.held
            );
        }
    }

    /** True when this module is drawing its own tag, so the vanilla nameplate should stay hidden. */
    public boolean hidesVanilla(Entity entity) {
        return this.shouldRender(entity);
    }

    private RubyNametagRenderer.TagData buildTag(Entity entity) {
        String name = entity.getName().getString();
        boolean friend = entity instanceof Player player && FriendsManager.isFriend(player);

        Integer health = null;
        if (displayHealth.value() && entity instanceof LivingEntity living) {
            health = Math.round(living.getHealth() + living.getAbsorptionAmount());
        }

        Integer distance = null;
        if (displayDistance.value()) {
            distance = (int) RubyClient.client.player.distanceTo(entity);
        }

        Integer ping = null;
        if (displayPing.value() && entity instanceof Player player
                && RubyClient.client.getConnection() != null) {
            PlayerInfo entry = RubyClient.client.getConnection().getPlayerInfo(player.getUUID());
            if (entry != null) ping = entry.getLatency();
        }

        return new RubyNametagRenderer.TagData(name, health, ping, distance, friend);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (!entities.value().contains(entity.getType())) return false;

        Freecam freecam = Modules.getByClass(Freecam.class);
        boolean freecamActive = freecam != null && freecam.enabled();
        boolean firstPerson = RubyClient.client.options.getCameraType().isFirstPerson();

        if (ignoreSelf.value() && entity == RubyClient.client.player && !freecamActive && firstPerson) return false;
        if (ignoreFriends.value() && entity instanceof Player player && FriendsManager.isFriend(player)) return false;
        if (ignoreBots.value() && entity instanceof Player player && botFilter.shouldHide(player)) return false;
        return true;
    }

    /** Hotswap does not rebuild settings, so an older floor can still be stored on the live slider. */
    private static boolean readableFloor;

    private void loosenOldMin() {
        if (Nametags.readableFloor) return;
        Nametags.readableFloor = true;
        if (this.minScale.value() < 0.5) this.minScale.setQuiet(0.5);
    }

    private record Drawn(
            double dist, double x, double y, double scale,
            RubyNametagRenderer.TagData data, ItemStack held
    ) {}

    private static int withAlpha(int color, float alpha) {
        int rgb = color & 0x00FFFFFF;
        return (Math.round(alpha * 255) << 24) | rgb;
    }
}
