package ruby.systems.modules.combat;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.Rotations;
import ruby.helpers.render.Renderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

import java.util.ArrayDeque;
import java.util.Queue;

public class Hitboxes extends Module {
    public final DoubleValue expand = this.config.create(new DoubleValue.Builder("Expand")
            .description("How much to expand the hitbox of the entity.")
            .range(-1, 1, 0.05)
            .defaultValue(0.4)
            .build());

    public final EntityTypeListValue targets = this.config.create(new EntityTypeListValue.Builder("Targets")
            .description("Which types of entities to expand.")
            .defaultValue(EntityType.PLAYER)
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

    private PlayerInput lastInput = null;
    private boolean cancelNextSwing = false;
    private boolean ignoreNextAttack = false;
    private float prevPitch = Float.NaN;
    private float prevYaw = Float.NaN;
    private final Queue<Entity> attackQueue = new ArrayDeque<>();
    public Hitboxes() {
        super("Hitboxes", "Expands an entity's hitboxes.", ModuleType.COMBAT);

        Events.PACKET.register(PacketEvents.SEND, event -> {
            if(!this.enabled()) return;
            if(!this.cancelNextSwing) return;
            if(!(event.packet() instanceof HandSwingC2SPacket)) return;

            event.setCancelled(true);
            this.cancelNextSwing = false;
        });

        Events.ENTITY.register(EntityEvents.BEFORE_ATTACK, event -> {
            if(!this.enabled()) return;
            if(this.ignoreNextAttack) {
                this.ignoreNextAttack = false;
                return;
            }

            this.attackQueue.add(event.entity());
            this.cancelNextSwing = true;
            event.setCancelled(true);
        });
    }

    public double getEntityValue(Entity entity) {
        if(!this.enabled()) return 0;
        if(entity.equals(RubyClient.client.player)) return 0;

        if(this.ignoreFriends.value() && entity instanceof PlayerEntity player && FriendsManager.isFriend(player))
            return 0;

        if(this.targets.value().contains(entity.getType())) return this.expand.value();
        return 0;
    }

    @Override
    public void tick() {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.interactionManager == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        this.lastInput = null;
        this.prevYaw = Float.NaN; this.prevPitch = Float.NaN;
        if(this.attackQueue.isEmpty()) return;

        Entity entity = this.attackQueue.poll();
        if(entity == null) return;

        Box box = entity.getBoundingBox();
        Vec3d eye = RubyClient.client.player.getEyePos();
        double x = MathHelper.clamp(eye.getX(), box.minX, box.maxX);
        double y = MathHelper.clamp(eye.getY(), box.minY, box.maxY);
        double z = MathHelper.clamp(eye.getZ(), box.minZ, box.maxZ);
        if(RubyClient.client.player.getEyePos().squaredDistanceTo(x, y, z) > 9) return;

        this.lastInput = RubyClient.client.player.input.playerInput;
        RubyClient.client.player.input.playerInput = new PlayerInput(
                false, false, false, false,
                RubyClient.client.player.input.playerInput.jump(),
                RubyClient.client.player.input.playerInput.sneak(),
                false
        );

        this.prevYaw = RubyClient.client.player.getYaw();
        this.prevPitch = RubyClient.client.player.getPitch();

        float[] angles = Rotations.rotationTo(EntityAnchorArgumentType.EntityAnchor.FEET, entity.getEntityPos());
        RubyClient.client.player.setAngles(angles[0], angles[1]);

        this.ignoreNextAttack = true;

        RubyClient.client.interactionManager.attackEntity(RubyClient.client.player, entity);
        RubyClient.client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    @Override
    public void tickEnd() {
        if(RubyClient.client.player == null) return;

        if(!Float.isNaN(this.prevYaw) && !Float.isNaN(this.prevPitch)) {
            RubyClient.client.player.setAngles(this.prevYaw, this.prevPitch);
            this.prevYaw = Float.NaN; this.prevPitch = Float.NaN;
        }

        if(this.lastInput != null) {
            RubyClient.client.player.input.playerInput = this.lastInput;
            this.lastInput = null;
        }
    }

    @Override
    public void onEnable() {
        this.prevYaw = -1; this.prevPitch = -1;
        this.ignoreNextAttack = false;
        this.cancelNextSwing = false;
        this.attackQueue.clear();
        this.lastInput = null;
    }

    @Override
    public void render3D() {
        if(RubyClient.client.world == null) return;
        if(!this.renderHitboxes.value()) return;

        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
        Renderer.color(this.hitboxColor.opaque());

        for(Entity e : RubyClient.client.world.getEntities()) {
            double expand = this.getEntityValue(e);
            if(expand == 0) continue;

            Vec3d size = new Vec3d(
                    e.getWidth() + 2 * expand,
                    e.getHeight() + 2 * expand,
                    e.getWidth() + 2 * expand
            );

            Renderer.cuboid(
                    e.getEntityPos().add(0, e.getHeight() / 2, 0),
                    size
            );
        }
    }
}
