package ruby.systems.modules.combat;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import ruby.RubyClient;
import ruby.helpers.Rotations;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EntityTypeListValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class Hitboxes extends Module {
    public final DoubleValue expand = this.config.create(new DoubleValue.Builder("Expansion")
            .description("How much to expand selected entity hitboxes by")
            .range(0, 2, 0.05)
            .defaultValue(0.5)
            .build());

    public final EntityTypeListValue targets = this.config.create(new EntityTypeListValue.Builder("Targets")
            .description("Which types of entities to target")
            .defaultValue(EntityType.PLAYER)
            .build());

    public Hitboxes() {
        super("Hitboxes", "Expands entity hitboxes and silently aims at targets", ModuleType.COMBAT);

        Events.ENTITY.register(EntityEvents.ATTACK, event -> {
            if(!this.enabled()) return;
            if(RubyClient.client.player == null) return;
            if(RubyClient.client.getNetworkHandler() == null) return;
            if(!this.targets.value().contains(event.entity().getType())) return;

            Rotations.serverLookAt(EntityAnchorArgumentType.EntityAnchor.EYES, event.entity().getEntityPos());
        });
    }
}
