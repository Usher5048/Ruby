package ruby.systems.modules.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.render.NametagUtils;
import ruby.helpers.render.Renderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.config.EnumValue;
import ruby.systems.events.Render2DEvent;
import ruby.systems.hud.HudRenderer;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

public class ESP extends Module {

    public enum Mode { Box, TwoD;
        @Override public String toString() { return this == TwoD ? "2D" : name(); }
    }
    public enum TwoDStyle { Full, Corners;
        @Override public String toString() { return name(); }
    }
    public enum ShapeMode { Lines, Sides, Both }

    private final EnumValue<Mode> mode;
    private final EnumValue<TwoDStyle> twoDStyle;
    private final EnumValue<ShapeMode> shapeMode;
    private final EntityTypeListValue entities;
    private final BooleanValue ignoreSelf;
    private final DoubleValue fillOpacity;
    private final DoubleValue cornerScale;
    private final DoubleValue fadeDistance;
    private final DoubleValue maxDistance;
    private final BooleanValue distanceColors;
    private final BooleanValue friendOverride;
    private final ColorValue playersColor;
    private final ColorValue animalsColor;
    private final ColorValue waterColor;
    private final ColorValue monstersColor;
    private final ColorValue ambientColor;
    private final ColorValue miscColor;
    private final ColorValue friendColor;

    private int count;

    public ESP() {
        super("ESP", "Highlights entities through walls.", ModuleType.RENDER);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .defaultValue(Mode.Box).build());
        twoDStyle = config.create(new EnumValue.Builder<TwoDStyle>("2D Style")
                .description("Full rectangle or corner brackets.")
                .defaultValue(TwoDStyle.Corners)
                .visible(() -> mode.value() == Mode.TwoD)
                .build());
        shapeMode = config.create(new EnumValue.Builder<ShapeMode>("Shape Mode")
                .defaultValue(ShapeMode.Both)
                .visible(() -> mode.value() == Mode.Box || twoDStyle.value() == TwoDStyle.Full)
                .build());
        entities = config.create(new EntityTypeListValue.Builder("Entities")
                .defaultValue(EntityType.PLAYER).build());
        ignoreSelf = config.create(new BooleanValue.Builder("Ignore Self").defaultValue(true).build());
        fillOpacity = config.create(new DoubleValue.Builder("Fill Opacity")
                .range(0, 1, 0.05).defaultValue(0.25)
                .visible(() -> shapeMode.value() != ShapeMode.Lines)
                .build());
        cornerScale = config.create(new DoubleValue.Builder("Corner Scale")
                .description("Corner arm length as a fraction of box size.")
                .range(0.1, 0.5, 0.05).defaultValue(0.25)
                .visible(() -> mode.value() == Mode.TwoD && twoDStyle.value() == TwoDStyle.Corners)
                .build());
        fadeDistance = config.create(new DoubleValue.Builder("Fade Distance")
                .range(0, 64, 1).defaultValue(6.0).build());
        maxDistance = config.create(new DoubleValue.Builder("Max Distance")
                .range(8, 512, 4).defaultValue(128.0).build());
        distanceColors = config.create(new BooleanValue.Builder("Distance Colors").defaultValue(false).build());
        friendOverride = config.create(new BooleanValue.Builder("Friend Colors")
                .defaultValue(true)
                .visible(distanceColors::value)
                .build());
        playersColor = config.create(new ColorValue.Builder("Players Color").defaultValue(0xFFFFFFFF).build());
        animalsColor = config.create(new ColorValue.Builder("Animals Color").defaultValue(0xFF19FF19).build());
        waterColor = config.create(new ColorValue.Builder("Water Color").defaultValue(0xFF1919FF).build());
        monstersColor = config.create(new ColorValue.Builder("Monsters Color").defaultValue(0xFFFF1919).build());
        ambientColor = config.create(new ColorValue.Builder("Ambient Color").defaultValue(0xFF191919).build());
        miscColor = config.create(new ColorValue.Builder("Misc Color").defaultValue(0xFFAAAAAA).build());
        friendColor = config.create(new ColorValue.Builder("Friend Color").defaultValue(0xFF33CCFF).build());
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    @Override
    public void render3D() {
        if (mode.value() != Mode.Box || RubyClient.client.world == null || RubyClient.client.player == null) return;

        count = 0;
        float tickDelta = RubyClient.client.gameRenderer.getCamera().getLastTickProgress();
        ShapeMode shape = shapeMode.value();

        for (Entity entity : RubyClient.client.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            Box box = lerpBox(entity, tickDelta);
            int lineColor = colorFor(entity);
            int sideColor = withAlpha(lineColor, fillOpacity.value().floatValue());

            if (shape == ShapeMode.Sides || shape == ShapeMode.Both) {
                Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
                Renderer.color(sideColor);
                drawBox(box);
            }
            if (shape == ShapeMode.Lines || shape == ShapeMode.Both) {
                Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
                Renderer.color(lineColor);
                drawBox(box);
            }
            count++;
        }
    }

    @Override
    public void render2D(Render2DEvent event) {
        if (mode.value() != Mode.TwoD || RubyClient.client.world == null || RubyClient.client.player == null) return;

        HudRenderer hud = HudRenderer.INSTANCE;
        hud.begin(event.getContext());

        count = 0;
        float tickDelta = RubyClient.client.gameRenderer.getCamera().getLastTickProgress();
        boolean corners = twoDStyle.value() == TwoDStyle.Corners;
        ShapeMode shape = shapeMode.value();

        for (Entity entity : RubyClient.client.world.getEntities()) {
            if (!shouldRender(entity)) continue;

            NametagUtils.ScreenBounds bounds = NametagUtils.projectBounds(lerpBox(entity, tickDelta));
            if (bounds == null) continue;

            int lineColor = colorFor(entity);
            double x1 = bounds.x1(), y1 = bounds.y1(), x2 = bounds.x2(), y2 = bounds.y2();

            if (!corners && shape != ShapeMode.Lines) {
                hud.quad(x1, y1, x2 - x1, y2 - y1, withAlpha(lineColor, fillOpacity.value().floatValue()));
            }
            if (corners) {
                hud.cornerBox(x1, y1, x2, y2, lineColor, cornerScale.value());
            } else if (shape != ShapeMode.Sides) {
                hud.boxOutline(x1, y1, x2, y2, lineColor);
            }
            count++;
        }
    }

    private static void drawBox(Box box) {
        Vec3d center = box.getCenter();
        Vec3d size = new Vec3d(box.getLengthX(), box.getLengthY(), box.getLengthZ());
        Renderer.cuboid(center, size);
    }

    private static Box lerpBox(Entity entity, float tickDelta) {
        Vec3d l = entity.getLerpedPos(tickDelta);
        return entity.getBoundingBox().offset(l.x - entity.getX(), l.y - entity.getY(), l.z - entity.getZ());
    }

    private boolean shouldRender(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (ignoreSelf.value() && entity == RubyClient.client.player) return false;
        if (!entities.value().contains(entity.getType())) return false;

        double maxDist = maxDistance.value();
        if (maxDist > 0 && RubyClient.client.player.squaredDistanceTo(entity) > maxDist * maxDist) return false;
        return fadeAlpha(entity) > 0f;
    }

    private int colorFor(Entity entity) {
        return withAlpha(baseColor(entity), fadeAlpha(entity));
    }

    private int baseColor(Entity entity) {
        if (distanceColors.value()) {
            if(friendOverride.value() && entity instanceof PlayerEntity player && FriendsManager.isFriend(player))
                return friendColor.value();

            double dist = Math.sqrt(RubyClient.client.player.squaredDistanceTo(entity));
            float ratio = (float) Math.min(dist / maxDistance.value(), 1.0);
            int r = (int) (255 * ratio);
            int g = (int) (255 * (1.0 - ratio));
            return 0xFF000000 | (r << 16) | (g << 8) | 0xFF;
        }

        if(entity instanceof PlayerEntity player && FriendsManager.isFriend(player))
            return friendColor.value();

        if (entity instanceof PlayerEntity) return playersColor.value();
        if (entity instanceof AnimalEntity) return animalsColor.value();
        if (entity instanceof WaterCreatureEntity) return waterColor.value();
        if (entity instanceof Monster) return monstersColor.value();
        if (entity instanceof AmbientEntity) return ambientColor.value();
        return miscColor.value();
    }

    private float fadeAlpha(Entity entity) {
        double fade = fadeDistance.value();
        if (fade <= 0) return 1f;

        double dist = Math.sqrt(RubyClient.client.player.squaredDistanceTo(entity));
        if (dist <= fade) return 1f;

        double range = Math.max(maxDistance.value() - fade, 1.0);
        return (float) Math.max(0, 1.0 - (dist - fade) / range);
    }

    private static int withAlpha(int color, float alpha) {
        int a = (color >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0f, Math.min(1f, alpha)));
        return (scaled << 24) | (color & 0x00FFFFFF);
    }
}
