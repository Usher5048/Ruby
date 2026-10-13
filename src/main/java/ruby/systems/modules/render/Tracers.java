package ruby.systems.modules.render;

import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import ruby.RubyClient;
import ruby.helpers.render.RenderShapes;
import ruby.helpers.render.Renderer;
import ruby.mixin.ChunkWaypointAccessor;
import ruby.mixin.TrackedWaypointAccessor;
import ruby.mixin.Vec3iWaypointAccessor;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

import java.util.UUID;

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

    private final BooleanValue extendTracers = this.config.create(new BooleanValue.Builder("Extend tracers")
            .description("Uses the locator bar to show rough positions of far away players")
            .defaultValue(false)
            .visible(() -> {
                if(RubyClient.client.getConnection() == null) return false;
                return RubyClient.client.getConnection()
                        .getWaypointManager()
                        .hasWaypoints();
            }).build());

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
            if (target.value() == Target.Head) y += entity.getEyeHeight();
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

        if(!this.extendTracers.value()) return;
        if(RubyClient.client.getConnection() == null) return;

        ClientWaypointManager manager = RubyClient.client.getConnection().getWaypointManager();
        if(!manager.hasWaypoints()) return;

        // Waypoints aren't attached to entities, fall back to player data
        AABB box = RubyClient.client.player.getBoundingBox();
        float eyeHeight = RubyClient.client.player.getEyeHeight();
        double height = box.maxY - box.minY;

        // TODO: There HAS to be a better way to do this
        manager.forEachWaypoint(RubyClient.client.player, waypoint -> {
            TrackedWaypoint.Type type = ((TrackedWaypointAccessor) waypoint).ruby$getType();
            if(type == TrackedWaypoint.Type.EMPTY) return;
            if(type == TrackedWaypoint.Type.AZIMUTH) return;

            Vec3 pos = null;
            if(type == TrackedWaypoint.Type.CHUNK) {
                pos = Vec3.atBottomCenterOf(
                        ((ChunkWaypointAccessor) waypoint).getChunkPos()
                        .getMiddleBlockPosition(RubyClient.client.player.getBlockY())
                );
            } else if(type == TrackedWaypoint.Type.VEC3I)
                pos = Vec3.atBottomCenterOf(((Vec3iWaypointAccessor) waypoint).getVector());

            if(pos == null) return;

            if(this.target.value() == Target.Head) pos.add(0, eyeHeight, 0);
            else if(this.target.value() == Target.Body) pos.add(0, height / 2, 0);

            Entity e = RubyClient.client.level.getEntity(waypoint.id().left().orElse(UUID.randomUUID()));
            if(e != null) return; // if we have a copy of the entity the normal tracer has already been drawn

            int color = this.playersColor.value(); // locator bar only shows players on normal servers
            RenderShapes.lineToCenter(pos.x(), pos.y(), pos.z(), color);

            if(this.stem.value()) {
                Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
                Renderer.color(color);
                Renderer.line(pos.x(), pos.y(), pos.z(), pos.x(), pos.y() + height, pos.z());
            }

            this.count++;
        });
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
