package ruby.systems.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
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
    }

    public double getEntityValue(Entity entity) {
        if(!this.enabled()) return 0;
        if(this.ignoreFriends.value() && entity instanceof PlayerEntity player
                && FriendsManager.isFriend(player.getGameProfile().name()))
            return 0;
        if(this.targets.value().contains(entity.getType())) return this.expand.value();
        return 0;
    }
}
