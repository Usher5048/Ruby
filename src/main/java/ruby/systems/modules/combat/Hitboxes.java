package ruby.systems.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.render.Renderer;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

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

    public Hitboxes() {
        super("Hitboxes", "Expands an entity's hitboxes.", ModuleType.COMBAT);

        Events.ENTITY.register(EntityEvents.BEFORE_ATTACK, event -> {

        });
    }

    public double getEntityValue(Entity entity) {
        if(!this.enabled()) return 0;
        if(this.ignoreFriends.value() && entity instanceof PlayerEntity player && FriendsManager.isFriend(player))
            return 0;

        if(this.targets.value().contains(entity.getType())) return this.expand.value();
        return 0;
    }

    // Testing don't remove
    @Override
    public void render3D() {
        if(RubyClient.client.player == null) return;

        Vec3d serverPos = Bypasses.get().position();
        Vec3d serverVel = Bypasses.get().velocity();
        boolean serverGround = Bypasses.get().onGround();

        float pitch = Bypasses.get().pitch();
        float yaw = Bypasses.get().yaw();
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        Vec3d serverLook = new Vec3d(i * j, -k, h * j);

        float width = RubyClient.client.player.getWidth();
        float height = RubyClient.client.player.getHeight();
        float eyeHeight = RubyClient.client.player.getEyeHeight(RubyClient.client.player.getPose());

        Vec3d renderEye = serverPos.add(0, eyeHeight, 0);
        Vec3d renderPos = serverPos.add(0, height / 2, 0);
        Vec3d renderSize = new Vec3d(width, height, width);

        Renderer.color(0x330000FF);
        Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
        Renderer.cuboid(renderPos, renderSize);

        if(serverGround) {
            Renderer.color(0x3300FF00);
            Renderer.cuboid(serverPos, new Vec3d(0.2, 0.2, 0.2));
        }

        Renderer.color(0x0000FF);
        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
        Renderer.cuboid(renderPos, renderSize);
        Renderer.line(renderPos, renderPos.add(serverVel));

        Renderer.color(0xFF0000);
        Renderer.line(renderEye, renderEye.add(serverLook));

        if(serverGround) {
            Renderer.color(0x00FF00);
            Renderer.cuboid(serverPos, new Vec3d(0.2, 0.2, 0.2));
        }
    }
}
