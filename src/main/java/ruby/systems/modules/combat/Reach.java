package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Extends the player's entity-targeting reach and block-interaction reach
 * by modifying the vanilla ENTITY_INTERACTION_RANGE and BLOCK_INTERACTION_RANGE
 * attributes directly on the client player.
 * <p>
 * <b>Entity reach</b> only makes targets selectable / highlightable from
 * further away on the client.  This only changes client-side selection and interaction attributes.
 * <p>
 * <b>Block reach</b> is uncapped — servers rarely flag block interaction
 * range in the same way.
 * <p>
 * Vanilla defaults: entity = 3.0, block = 4.5 (survival).
 */
public class Reach extends Module {

    private final DoubleValue entityReach;
    private final DoubleValue blockReach;

    private double prevEntityRange = 3.0;
    private double prevBlockRange  = 4.5;

    public Reach() {
        super("Reach", "Increases your attack and block interaction reach.", ModuleType.COMBAT);

        entityReach = config.create(new DoubleValue.Builder("Entity Reach")
                .description("Client-side entity targeting reach. Attacks are clamped to 2.9.")
                .defaultValue(4.5).min(2.9).max(6.0).step(0.1)
                .build());

        blockReach = config.create(new DoubleValue.Builder("Block Reach")
                .description("Block interaction reach distance.")
                .defaultValue(5.0).min(4.5).max(10.0).step(0.1)
                .build());
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EntityAttributeInstance entityAttr = mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) {
            prevEntityRange = entityAttr.getBaseValue();
            entityAttr.setBaseValue(entityReach.value());
        }

        EntityAttributeInstance blockAttr = mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) {
            prevBlockRange = blockAttr.getBaseValue();
            blockAttr.setBaseValue(blockReach.value());
        }
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EntityAttributeInstance entityAttr = mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) entityAttr.setBaseValue(entityReach.value());

        EntityAttributeInstance blockAttr = mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) blockAttr.setBaseValue(blockReach.value());
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EntityAttributeInstance entityAttr = mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) entityAttr.setBaseValue(prevEntityRange);

        EntityAttributeInstance blockAttr = mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) blockAttr.setBaseValue(prevBlockRange);
    }

    @Override
    public String getInfoString() {
        return String.format("%.1f", this.entityReach.value());
    }
}

