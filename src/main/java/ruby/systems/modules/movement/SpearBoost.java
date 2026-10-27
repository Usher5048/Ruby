package ruby.systems.modules.movement;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import ruby.RubyClient;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class SpearBoost extends Module {
    private final IntegerValue packetCount = this.config.create(new IntegerValue.Builder("Lunge Count")
            .description("The amount of extra times to lunge per lunge")
            .defaultValue(5)
            .range(1, 20)
            .build());

    public SpearBoost() {
        super("Spear Boost", "Abuses spear lunging to launch you.", ModuleType.MOVEMENT);

        Events.PACKET.register(PacketEvents.SEND, event -> {
            if(RubyClient.client.player == null) return;

            if(!this.enabled()) return;
            if(!(event.packet() instanceof ServerboundPlayerActionPacket packet)) return;
            if(packet.getAction() != ServerboundPlayerActionPacket.Action.STAB) return;

            ItemStack stack = RubyClient.client.player.getMainHandItem();
            if(!stack.is(ItemTags.SPEARS)) return;

            int lungeLevel = 0; // You'd think it'd be easier to get the level of an enchantment
            ItemEnchantments enchantments = stack.getEnchantments();
            for(Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if(!entry.getKey().is(Enchantments.LUNGE)) continue;
                lungeLevel = entry.getIntValue();
                break;
            }

            if(lungeLevel <= 0) return;
            for(int i = 0; i < this.packetCount.value(); i++) {
                event.connection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STAB,
                        BlockPos.ZERO, Direction.DOWN
                ));
            }
        });
    }
}
