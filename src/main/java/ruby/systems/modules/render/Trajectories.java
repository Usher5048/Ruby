package ruby.systems.modules.render;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.projectile.ProjectileSimulator;
import ruby.helpers.render.RenderShapes;
import ruby.helpers.render.Renderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.ItemListValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Trajectories ported from Meteor Client.
 */
public class Trajectories extends Module {

    private final ItemListValue items;
    private final BooleanValue otherPlayers;
    private final BooleanValue firedProjectiles;
    private final IntegerValue simulationSteps;
    private final IntegerValue ignoreFirstTicks;
    private final EnumValue<RenderShapes.ShapeMode> shapeMode;
    private final ColorValue sideColor;
    private final ColorValue lineColor;

    private final ProjectileSimulator simulator = new ProjectileSimulator();

    public Trajectories() {
        super("Trajectories", "Predicts the trajectory of throwable items.", ModuleType.RENDER);

        items = config.create(new ItemListValue.Builder("Items")
                .defaultValue(defaultItems())
                .build());
        otherPlayers = config.create(new BooleanValue.Builder("Other Players").defaultValue(true).build());
        firedProjectiles = config.create(new BooleanValue.Builder("Fired Projectiles").defaultValue(false).build());
        simulationSteps = config.create(new IntegerValue.Builder("Simulation Steps")
                .range(0, 5000).defaultValue(500).build());
        ignoreFirstTicks = config.create(new IntegerValue.Builder("Ignore First Ticks")
                .range(0, 20).defaultValue(3).build());
        shapeMode = config.create(new EnumValue.Builder<RenderShapes.ShapeMode>("Shape Mode")
                .defaultValue(RenderShapes.ShapeMode.Both).build());
        sideColor = config.create(new ColorValue.Builder("Side Color").defaultValue(0x23FF9600).build());
        lineColor = config.create(new ColorValue.Builder("Line Color").defaultValue(0xFFFF9600).build());
    }

    private static List<Item> defaultItems() {
        List<Item> list = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
                    || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem
                    || item instanceof ExperienceBottleItem || item instanceof SplashPotionItem
                    || item instanceof LingeringPotionItem) {
                list.add(item);
            }
        }
        return list;
    }

    @Override
    public void render3D() {
        if (RubyClient.client.world == null || RubyClient.client.player == null) return;

        float tickDelta = RubyClient.client.getRenderTickCounter().getTickProgress(false);

        for (PlayerEntity player : RubyClient.client.world.getPlayers()) {
            if (!otherPlayers.value() && player != RubyClient.client.player) continue;
            renderPlayer(player, tickDelta);
        }

        if (firedProjectiles.value()) {
            for (var entity : RubyClient.client.world.getEntities()) {
                if (!(entity instanceof ProjectileEntity)) continue;
                if (!simulator.set(entity)) continue;
                renderPath(0);
            }
        }
    }

    private void renderPlayer(PlayerEntity player, float tickDelta) {
        ItemStack stack = player.getMainHandStack();
        if (!items.value().contains(stack.getItem())) {
            stack = player.getOffHandStack();
            if (!items.value().contains(stack.getItem())) return;
        }

        if (!simulator.set(player, stack, tickDelta)) return;
        renderPath(player == RubyClient.client.player ? ignoreFirstTicks.value() : 0);
    }

    private void renderPath(int ignoreStart) {
        int max = simulationSteps.value() <= 0 ? Integer.MAX_VALUE : simulationSteps.value();
        Vec3d last = null;
        HitResult hit = null;

        for (int i = 0; i < max; i++) {
            if (i >= ignoreStart) {
                if (last != null) {
                    Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
                    Renderer.color(lineColor.value());
                    Renderer.line(last, simulator.pos);
                }
                last = simulator.pos;
            } else {
                last = simulator.pos;
            }

            hit = simulator.tick();
            if (hit.getType() != HitResult.Type.MISS) break;
        }

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            Vec3d p = hit.getPos();
            RenderShapes.box(p.x - 0.25, p.y - 0.25, p.z - 0.25,
                    p.x + 0.25, p.y + 0.25, p.z + 0.25,
                    sideColor.value(), lineColor.value(), shapeMode.value());
        }
    }
}
