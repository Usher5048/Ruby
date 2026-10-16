package ruby.systems.modules.render;

import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.events.Render2DEvent;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.text.FontRenderer;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.phys.Vec3;

public class Hud extends Module {
    public final BooleanValue showFPS = this.config.create(new BooleanValue.Builder("Show FPS")
            .defaultValue(true)
            .build());

    public final BooleanValue showTPS = this.config.create(new BooleanValue.Builder("Show TPS")
            .defaultValue(true)
            .build());

    public final BooleanValue showCoords = this.config.create(new BooleanValue.Builder("Show Coordinates")
            .defaultValue(true)
            .build());

    public final BooleanValue showPing = this.config.create(new BooleanValue.Builder("Show Ping")
            .defaultValue(true)
            .build());

    public final BooleanValue showSpeed = this.config.create(new BooleanValue.Builder("Show Speed")
            .defaultValue(true)
            .build());

    public final BooleanValue showFacing = this.config.create(new BooleanValue.Builder("Show Direction")
            .defaultValue(true)
            .build());

    private double tps;
    private long lastPacketTime;
    private final ArrayList<Double> tpsHistory = new ArrayList<>();
    private int tick = 0;

    public Hud() {
        super("Hud", "Shows various info on your HUD.", ModuleType.RENDER);

        Events.PACKET.register(PacketEvents.RECEIVE, event -> {
            if(!(event.packet() instanceof ClientboundSetTimePacket)) return;
            this.lastPacketTime = System.currentTimeMillis();
        });
    }

    @Override
    public void tick() {
        this.tick++;

        // only update tps related shit every 100ms
        if(this.tick <= 2) return;
        this.tick = 0;

        long milliDiff = System.currentTimeMillis() - this.lastPacketTime;

        this.tpsHistory.add(20.0 / Math.max((milliDiff - 1000.0) / 500.0, 1.0));
        if(this.tpsHistory.size() > 300) // 300 entries * 100 ms = last 30 seconds of tps
            this.tpsHistory.removeFirst();

        this.tps = this.tpsHistory.stream().reduce(Double::sum).orElse(0.0) / this.tpsHistory.size();
    }

    @Override
    public void onEnable() {
        this.tick = 0;
        this.tpsHistory.clear();
    }

    @Override
    public void render2D(Render2DEvent event) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getConnection() == null) return;

        int padding = 2;

        FontRenderer textRenderer = GUIStyle.get().monospaceFont();
        GuiGraphicsExtractor context = event.getContext();

        ArrayList<String> left = new ArrayList<>();
        ArrayList<String> right = new ArrayList<>();

        if(this.showFPS.value())
            left.add(String.format("%d FPS", RubyClient.client.getFps()));

        Vec3 p = RubyClient.client.player.position();
        if(this.showCoords.value())
            left.add(String.format("XYZ: %.3f, %.3f, %.3f", p.x, p.y, p.z));

        String direction = RubyClient.client.player.getDirection().getSerializedName();
        String axisDir = RubyClient.client.player.getDirection().getAxis().getSerializedName().toUpperCase();
        boolean positive = RubyClient.client.player.getDirection().getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if(this.showFacing.value()) {
            left.add(String.format(
                    "Facing: %s; %c%s (%.2f / %.2f)",
                    direction, positive ? '+' : '-', axisDir,
                    ((RubyClient.client.player.getYRot() + 180) % 360 + 360) % 360 - 180,
                    RubyClient.client.player.getXRot()
            ));
        }

        if(this.showSpeed.value()) {
            Vec3 velocity = RubyClient.client.player.position().subtract(
                    RubyClient.client.player.xo,
                    RubyClient.client.player.yo,
                    RubyClient.client.player.zo
            );

            left.add(String.format("%.3f m/s", velocity.length() * 20));
        }

        PlayerInfo entry = RubyClient.client.getConnection().getPlayerInfo(RubyClient.client.player.getUUID());
        if(entry != null && this.showPing.value())
            right.add(String.format("Latency: %d ms", entry.getLatency()));

        if(this.showTPS.value())
            right.add(String.format("%.2f tps", this.tps));

        int y = padding;
        for(String str : left) {
            int width = textRenderer.getWidth(str);

            context.fill(
                    0 + padding,
                    y,
                    0 + padding + padding + width + padding,
                    y + padding + textRenderer.fontHeight,
                    0xFF111111
            );

            textRenderer.draw(
                    context,
                    str,
                    0 + padding + padding,
                    y + padding,
                    0xFFCC3366
            );

            y += padding + textRenderer.fontHeight;
        }

        y = padding;
        int sWidth = RubyClient.client.getWindow().getWidth();
        for(String str : right) {
            int width = textRenderer.getWidth(str);

            context.fill(
                    sWidth - padding - padding - width - padding,
                    y,
                    sWidth - padding,
                    y + padding + textRenderer.fontHeight,
                    0xFF111111
            );

            textRenderer.draw(
                    context,
                    str,
                    sWidth - padding - padding - width,
                    y + padding,
                    0xFFCC3366
            );

            y += padding + textRenderer.fontHeight;
        }

        int guiScale = RubyClient.client.getWindow().getGuiScale();
        int sHeight = RubyClient.client.getWindow().getHeight();
        long milliDiff = System.currentTimeMillis() - this.lastPacketTime;
        if(milliDiff > 3000) {
            String text = String.format("Server not responding! %dms", milliDiff);
            int textWidth = textRenderer.getWidth(text);

            context.fill(
                    sWidth / 2 - 2 * padding - textWidth / 2,
                    sHeight - padding - 61 * guiScale,
                    sWidth / 2 + 2 * padding + textWidth / 2,
                    sHeight - 61 * guiScale + padding + textRenderer.fontHeight,
                    0xFF111111
            );

            context.outline(
                    sWidth / 2 - 2 * padding - textWidth / 2,
                    sHeight - padding - 61 * guiScale,
                    2 * padding + textWidth + 2 * padding,
                    padding + textRenderer.fontHeight + padding,
                    0xFFCC3366
            );

            textRenderer.draw(
                    context,
                    text,
                    sWidth / 2 - textWidth / 2,
                    sHeight - 61 * guiScale,
                    0xFFCC3366
            );
        }
    }
}
