package ruby.systems.modules.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class ElytraTweaks extends Module {

    public final BooleanValue speedControl = this.config.create(new BooleanValue.Builder("Speed Control")
            .description("Allows the use of W and S to control forwards velocity")
            .defaultValue(true)
            .build());

    public final BooleanValue heightControl = this.config.create(new BooleanValue.Builder("Height Control")
            .description("Allows the use of jumping and sneaking to control vertical velocity")
            .defaultValue(false)
            .build());

    public ElytraTweaks() {
        super("Elytra Tweaks", "Tweaks the movement of elytras.", ModuleType.PLAYER);
    }

    private void heightControl() {
        if(RubyClient.client.player == null) return;

        Vec3 vel = RubyClient.client.player.getDeltaMovement();

        if(RubyClient.client.options.keyJump.isDown())
            RubyClient.client.player.setDeltaMovement(vel.add(0, 0.08, 0));
        else if(RubyClient.client.options.keyShift.isDown())
            RubyClient.client.player.setDeltaMovement(vel.add(0, -0.04, 0));
    }

    private void speedControl() {
        if(RubyClient.client.player == null) return;

        float yaw = (float) Math.toRadians(RubyClient.client.player.getYRot());
        float pitch = (float) Math.toRadians(RubyClient.client.player.getXRot());

        Vec3 vel = RubyClient.client.player.getDeltaMovement();
        Vec3 newVel = vel.add(
                -Mth.sin(yaw) * Mth.cos(pitch) * 0.05,
                -Mth.sin(pitch) * 0.05,
                Mth.cos(yaw) * Mth.cos(pitch) * 0.05
        );

        if(RubyClient.client.options.keySprint.isDown())
            newVel = newVel.subtract(vel).normalize().scale(5 * 0.05);

        if(RubyClient.client.options.keyUp.isDown())
            RubyClient.client.player.setDeltaMovement(newVel);
        else if(RubyClient.client.options.keyDown.isDown())
            RubyClient.client.player.setDeltaMovement(newVel.subtract(vel).scale(-1).add(vel));
    }

    @Override
    public void tick() {
        if(RubyClient.client.player == null) return;

        ItemStack chest = RubyClient.client.player.getItemBySlot(EquipmentSlot.CHEST);
        if(chest.getItem() != Items.ELYTRA) return;

        if(!RubyClient.client.player.isFallFlying())
            return;

        if(this.speedControl.value()) speedControl();
        if(this.heightControl.value()) heightControl();
    }
}
