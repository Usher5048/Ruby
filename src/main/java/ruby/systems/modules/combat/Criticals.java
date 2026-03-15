package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Packet mode: sends fake position packets with Y offsets before attack.
 * Meteor's Jump mode: calls player.jump() before attack.
 * Adapted here to tick-based: sends crit packets when looking at an entity with full cooldown.
 */
public class Criticals extends Module {
    public enum Mode { Packet, Jump, MiniJump }

    private final EnumValue<Mode> mode;

    public Criticals() {
        super("Criticals", "Always land critical hits.", ModuleCategory.COMBAT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The mode used to perform criticals.")
                .defaultValue(Mode.Packet)
                .build());
    }


    public boolean isPacketModeActive() {
        return enabled() && mode.value() == Mode.Packet;
    }
    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (!player.isOnGround()) return;
        if (player.isSubmergedInWater() || player.isInLava()) return;

        // Only apply when looking at a living entity and cooldown is ready
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof LivingEntity)) return;
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        switch (mode.value()) {
            case Packet -> {
                // Meteor's packet mode: send fake position packets to appear airborne
                // Server sees player move up then down, so next attack is a critical
                double x = player.getX(), y = player.getY(), z = player.getZ();
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, false));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
            }
            case Jump -> // Full jump for guaranteed crit
                    player.jump();
            case MiniJump -> // Meteor's mini jump: small velocity upward
                    player.setVelocity(player.getVelocity().x, 0.1, player.getVelocity().z);
        }
    }
}

