package ruby.systems.modules.render;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
                    || item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem
                    || item instanceof ExperienceBottleItem || item instanceof SplashPotionItem
                    || item instanceof LingeringPotionItem) {
                list.add(item);
            }
        }
        return list;
    }

    @Override
    public void render3D() {
        if (RubyClient.client.level == null || RubyClient.client.player == null) return;

        float tickDelta = RubyClient.client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (Player player : RubyClient.client.level.players()) {
            if (!otherPlayers.value() && player != RubyClient.client.player) continue;
            renderPlayer(player, tickDelta);
        }

        if (firedProjectiles.value()) {
            for (var entity : RubyClient.client.level.getEntities(null, RubyClient.client.player.getBoundingBox().inflate(256))) {
                if (!(entity instanceof Projectile)) continue;
                if (!simulator.set(entity)) continue;
                renderPath(0);
            }
        }
    }

    private void renderPlayer(Player player, float tickDelta) {
        ItemStack stack = player.getMainHandItem();
        if (!items.value().contains(stack.getItem())) {
            stack = player.getOffhandItem();
            if (!items.value().contains(stack.getItem())) return;
        }

        if (!simulator.set(player, stack, tickDelta)) return;
        renderPath(player == RubyClient.client.player ? ignoreFirstTicks.value() : 0);
    }

    private void renderPath(int ignoreStart) {
        int max = simulationSteps.value() <= 0 ? Integer.MAX_VALUE : simulationSteps.value();
        Vec3 last = null;
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
            Vec3 p = hit.getLocation();
            RenderShapes.box(p.x - 0.25, p.y - 0.25, p.z - 0.25,
                    p.x + 0.25, p.y + 0.25, p.z + 0.25,
                    sideColor.value(), lineColor.value(), shapeMode.value());
        }
    }
}
