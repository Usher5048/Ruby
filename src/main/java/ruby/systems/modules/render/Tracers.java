package ruby.systems.modules.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import ruby.RubyClient;
import ruby.helpers.render.RenderShapes;
import ruby.helpers.render.Renderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

/**
 * Ported from Meteor Client tracers (lines mode).
 */
public class Tracers extends Module {

    public enum Target { Head, Body, Feet }

    private final EntityTypeListValue entities;
    private final BooleanValue ignoreSelf;
    private final BooleanValue ignoreFriends;
    private final BooleanValue showInvis;
    private final EnumValue<Target> target;
    private final BooleanValue stem;
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

    public Tracers() {
        super("Tracers", "Displays tracer lines to specified entities.", ModuleType.RENDER);

        entities = config.create(new EntityTypeListValue.Builder("Entities")
                .defaultValue(EntityTypes.PLAYER).build());
        ignoreSelf = config.create(new BooleanValue.Builder("Ignore Self").defaultValue(false).build());
        ignoreFriends = config.create(new BooleanValue.Builder("Ignore Friends").defaultValue(false).build());
        showInvis = config.create(new BooleanValue.Builder("Show Invisible").defaultValue(true).build());
        target = config.create(new EnumValue.Builder<Target>("Target").defaultValue(Target.Body).build());
        stem = config.create(new BooleanValue.Builder("Stem").defaultValue(true).build());
        maxDistance = config.create(new DoubleValue.Builder("Max Distance").defaultValue(256.0).range(0, 512, 8).build());
        distanceColors = config.create(new BooleanValue.Builder("Distance Colors").defaultValue(false).build());
        friendOverride = config.create(new BooleanValue.Builder("Friend Colors")
                .defaultValue(true).visible(distanceColors::value).build());
        playersColor = config.create(new ColorValue.Builder("Players Color").defaultValue(0x7FCDCDCD).build());
        animalsColor = config.create(new ColorValue.Builder("Animals Color").defaultValue(0x7F91FF91).build());
        waterColor = config.create(new ColorValue.Builder("Water Color").defaultValue(0x7F9191FF).build());
        monstersColor = config.create(new ColorValue.Builder("Monsters Color").defaultValue(0x7FFF9191).build());
        ambientColor = config.create(new ColorValue.Builder("Ambient Color").defaultValue(0x7F4B4B4B).build());
        miscColor = config.create(new ColorValue.Builder("Misc Color").defaultValue(0x7F919191).build());
        friendColor = config.create(new ColorValue.Builder("Friend Color").defaultValue(0x7F33CCFF).build());
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    @Override
    public void render3D() {
        if (RubyClient.client.level == null || RubyClient.client.player == null) return;

        count = 0;
        float tickDelta = RubyClient.client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (Entity entity : RubyClient.client.level.entitiesForRendering()) {
            if (shouldIgnore(entity)) continue;

            AABB box = entity.getBoundingBox();
            double x = entity.getPosition(tickDelta).x;
            double y = entity.getPosition(tickDelta).y;
            double z = entity.getPosition(tickDelta).z;

            double height = box.maxY - box.minY;
            if (target.value() == Target.Head) y += height;
            else if (target.value() == Target.Body) y += height / 2;

            int color = colorFor(entity);
            RenderShapes.lineToCenter(x, y, z, color);

            if (stem.value()) {
                Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
                Renderer.color(color);
                Renderer.line(x, entity.getY(), z, x, entity.getY() + height, z);
            }

            count++;
        }
    }

    private boolean shouldIgnore(Entity entity) {
        if (entity == null || !entity.isAlive()) return true;
        if(RubyClient.client.player == null) return true;

        Freecam freecam = ruby.systems.modules.Modules.getByClass(Freecam.class);
        if ((freecam == null || !freecam.enabled()) && entity == RubyClient.client.player) return true;
        if (!entities.value().contains(entity.getType())) return true;
        if (ignoreSelf.value() && entity == RubyClient.client.player) return true;
        if (ignoreFriends.value() && entity instanceof Player player && FriendsManager.isFriend(player)) return true;
        if (!showInvis.value() && entity.isInvisible()) return true;

        double max = maxDistance.value();
        return max > 0 && RubyClient.client.player.distanceToSqr(entity) > max * max;
    }

    private int colorFor(Entity entity) {
        if(RubyClient.client.player == null) return 0;

        if (distanceColors.value()) {
            if (friendOverride.value() && entity instanceof Player player && FriendsManager.isFriend(player))
                return friendColor.value();

            double dist = Math.sqrt(RubyClient.client.player.distanceToSqr(entity));
            float ratio = (float) Math.min(dist / maxDistance.value(), 1.0);
            int r = (int) (255 * ratio);
            int g = (int) (255 * (1.0 - ratio));
            return 0xFF000000 | (r << 16) | (g << 8) | 0xFF;
        }

        if (entity instanceof Player player && FriendsManager.isFriend(player))
            return friendColor.value();

        if (entity instanceof Player) return playersColor.value();
        if (entity instanceof Animal) return animalsColor.value();
        if (entity instanceof WaterAnimal) return waterColor.value();
        if (entity instanceof Enemy) return monstersColor.value();
        if (entity instanceof AmbientCreature) return ambientColor.value();
        return miscColor.value();
    }
}
