package ruby.systems.modules.combat;

import ruby.RubyClient;
import ruby.helpers.Rotations;
import ruby.helpers.render.Renderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class Hitboxes extends Module {
    public final DoubleValue expand = this.config.create(new DoubleValue.Builder("Expand")
            .description("How much to expand the hitbox of the entity.")
            .range(-1, 1, 0.05)
            .defaultValue(0.4)
            .build());

    public final EntityTypeListValue targets = this.config.create(new EntityTypeListValue.Builder("Targets")
            .description("Which types of entities to expand.")
            .defaultValue(EntityTypes.PLAYER)
            .build());

    public final BooleanValue ignoreFriends = this.config.create(new BooleanValue.Builder("Ignore Friends")
            .description("Does not expand hitboxes of friends.")
            .defaultValue(true)
            .build());

    public final BooleanValue renderHitboxes = this.config.create(new BooleanValue.Builder("Render Hitboxes")
            .description("Renders the real size of hitboxes on entities")
            .defaultValue(true)
            .build());

    public final ColorValue hitboxColor = this.config.create(new ColorValue.Builder("Hitbox Color")
            .description("The color to use for rendering hitboxes")
            .defaultValue(0xFFFFFF)
            .build());

    private final Queue<EntityEvent> eventQueue = new ArrayDeque<>();
    private boolean ignoreNextInteract = false;
    private boolean cancelNextSwing = false;
    private Input lastInput = null;
    private float prevPitch = Float.NaN;
    private boolean subTicking = false;
    private float prevYaw = Float.NaN;

    public Hitboxes() {
        super("Hitboxes", "Expands an entity's hitboxes.", ModuleType.COMBAT);

        Events.PACKET.register(PacketEvents.SEND, event -> {
            if(!this.enabled()) return;
            if(!this.cancelNextSwing) return;
            if(!(event.packet() instanceof ServerboundPunchPacket)) return;

            event.setCancelled(true);
            this.cancelNextSwing = false;
        });

        Events.ENTITY.register(EntityEvents.BEFORE_INTERACT, event -> {
            if(!this.enabled()) return;
            if(this.ignoreNextInteract) {
                this.ignoreNextInteract = false;
                return;
            }

            if(!event.wasSecondary())
                this.cancelNextSwing = true;

            event.setCancelled(true);
            this.eventQueue.add(event);
        });
    }

    public double getEntityValue(Entity entity) {
        if(!this.enabled()) return 0;
        if(RubyClient.client.player == null) return 0;
        if(entity.equals(RubyClient.client.player)) return 0;

        if(this.ignoreFriends.value() && entity instanceof Player player && FriendsManager.isFriend(player))
            return 0;

        if(this.targets.value().contains(entity.getType())) return this.expand.value();
        return 0;
    }

    @Override
    public void tick() {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.gameMode == null) return;
        if(RubyClient.client.getConnection() == null) return;

        if(!this.subTicking) {
            this.lastInput = null;
            this.prevYaw = Float.NaN; this.prevPitch = Float.NaN;
        }

        if(this.eventQueue.isEmpty()) return;

        EntityEvent event = this.eventQueue.poll();
        Entity entity = event.entity();
        if(entity == null) return;

        AABB box = entity.getBoundingBox();
        Vec3 eye = RubyClient.client.player.getEyePosition();
        double x = Mth.clamp(eye.x(), box.minX, box.maxX);
        double y = Mth.clamp(eye.y(), box.minY, box.maxY);
        double z = Mth.clamp(eye.z(), box.minZ, box.maxZ);
        if(RubyClient.client.player.getEyePosition().distanceToSqr(x, y, z) > 9) return;

        if(!this.subTicking) {
            this.lastInput = RubyClient.client.player.input.keyPresses;
            RubyClient.client.player.input.keyPresses = new Input(
                    false, false, false, false,
                    RubyClient.client.player.input.keyPresses.jump(),
                    RubyClient.client.player.input.keyPresses.shift(),
                    false
            );

            this.prevYaw = RubyClient.client.player.getYRot();
            this.prevPitch = RubyClient.client.player.getXRot();

            float[] angles = Rotations.rotationTo(EntityAnchorArgument.Anchor.FEET, entity.position());
            RubyClient.client.player.setYRot(angles[0]);
            RubyClient.client.player.setXRot(angles[1]);
        }

        this.ignoreNextInteract = true;
        if(!event.wasSecondary()) {
            RubyClient.client.gameMode.attack(RubyClient.client.player, entity);
            RubyClient.client.getConnection().send(ServerboundPunchPacket.INSTANCE);
        } else {
            RubyClient.client.gameMode.interact(
                    RubyClient.client.player,
                    entity,
                    event.hitResult() != null
                            ? event.hitResult()
                            : new EntityHitResult(entity, entity.getEyePosition()),
                    event.hand()
            );
        }
    }

    @Override
    public void tickEnd() {
        if(RubyClient.client.player == null) return;

        if(!Float.isNaN(this.prevYaw) && !Float.isNaN(this.prevPitch)) {
            RubyClient.client.player.setYRot(this.prevYaw);
            RubyClient.client.player.setXRot(this.prevPitch);
            this.prevYaw = Float.NaN; this.prevPitch = Float.NaN;
        }

        if(this.lastInput != null) {
            RubyClient.client.player.input.keyPresses = this.lastInput;
            this.lastInput = null;
        }
    }

    @Override
    public void onEnable() {
        this.prevYaw = -1; this.prevPitch = -1;
        this.ignoreNextInteract = false;
        this.cancelNextSwing = false;
        this.eventQueue.clear();
        this.subTicking = false;
        this.lastInput = null;
    }

    @Override
    public void render3D() {
//        if(RubyClient.client.player == null) return;
//
//        Vec3d serverPos = Bypasses.get().position();
//        Vec3d serverVel = Bypasses.get().velocity();
//        boolean serverGround = Bypasses.get().onGround();
//
//        float pitch = Bypasses.get().pitch();
//        float yaw = Bypasses.get().yaw();
//        float f = pitch * ((float)Math.PI / 180F);
//        float g = -yaw * ((float)Math.PI / 180F);
//        float h = MathHelper.cos(g);
//        float i = MathHelper.sin(g);
//        float j = MathHelper.cos(f);
//        float k = MathHelper.sin(f);
//        Vec3d serverLook = new Vec3d(i * j, -k, h * j);
//
//        float width = RubyClient.client.player.getWidth();
//        float height = RubyClient.client.player.getHeight();
//        float eyeHeight = RubyClient.client.player.getEyeHeight(RubyClient.client.player.getPose());
//
//        Vec3d renderEye = serverPos.add(0, eyeHeight, 0);
//        Vec3d renderPos = serverPos.add(0, height / 2, 0);
//        Vec3d renderSize = new Vec3d(width, height, width);
//
//        Renderer.color(0x330000FF);
//        Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
//        Renderer.cuboid(renderPos, renderSize);
//
//        if(serverGround) {
//            Renderer.color(0x3300FF00);
//            Renderer.cuboid(serverPos, new Vec3d(0.2, 0.2, 0.2));
//        }
//
//        Renderer.color(0x0000FF);
//        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//        Renderer.cuboid(renderPos, renderSize);
//        Renderer.line(renderPos, renderPos.add(serverVel));
//
//        Renderer.color(0xFF0000);
//        Renderer.line(renderEye, renderEye.add(serverLook));
//
//        if(serverGround) {
//            Renderer.color(0x00FF00);
//            Renderer.cuboid(serverPos, new Vec3d(0.2, 0.2, 0.2));
//        }

        if(RubyClient.client.level == null) return;
        if(!this.renderHitboxes.value()) return;

        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
        Renderer.color(this.hitboxColor.opaque());

        for(Entity e : RubyClient.client.level.entitiesForRendering()) {
            double expand = this.getEntityValue(e);
            if(expand == 0) continue;

            Vec3 size = new Vec3(
                    e.getBbWidth() + 2 * expand,
                    e.getBbHeight() + 2 * expand,
                    e.getBbWidth() + 2 * expand
            );

            Renderer.cuboid(
                    e.position().add(0, e.getBbHeight() / 2, 0),
                    size
            );
        }
    }
}
