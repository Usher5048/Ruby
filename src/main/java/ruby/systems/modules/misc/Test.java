package ruby.systems.modules.misc;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.player.ClientInput;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.helpers.input.MovementInput;
import ruby.helpers.render.Renderer;
import ruby.systems.events.Events;
import ruby.systems.events.Render2DEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

public class Test extends Module {
    public Test() {
        super("Test", "dw abt it", ModuleType.MISC);

        Events.PACKET.register(PacketEvents.RECEIVE, event -> {
            if(RubyClient.client.player == null) return;
            if(!this.enabled()) return;

            if(!(event.packet() instanceof ClientboundEntityEventPacket status)) return;
//            if()
        });

        Events.ENTITY.register(EntityEvents.BEFORE_INTERACT, event -> {
            if(RubyClient.client.player == null) return;
            if(!this.enabled()) return;

        });
    }

//    private final HashMap<BlockPos, BlockState> states = new HashMap<>();
//    private final HashMap<BlockPos, BooleanList> hitCounts = new HashMap<>();

    private boolean wasCentering = false;
    private boolean wasGoalBlock = false;
    private boolean wasBreakPos = false;
    private boolean isCentering = false;
    private BlockPos goalBlock = null;
    private BlockPos breakPos = null;
    private BlockPos origin = null;
    private boolean start = false;
    private MovementInput input;
    private int breakTimer = 0;
    private ClientInput prevInput;
    private float lastPitch;
    private float lastYaw;
    private int xOff = 0;
    private int zOff = 0;

    private int ticksToBreak(BlockPos pos) {
        if(RubyClient.client.level == null) return 1;

        double exactTicks = 1.0 / RubyClient.client.level.getBlockState(pos).getDestroyProgress(
                RubyClient.client.player,
                RubyClient.client.level,
                pos
        );

        return (int) Math.ceil(exactTicks);
    }

    @Override
    public void tick() {
        if(true) return;
        if(RubyClient.client.getConnection() == null) return;
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.level == null) return;

//        for(BooleanList list : this.hitCounts.values())
//            if(list.size() > 4096) list.clear();

        if(RubyClient.client.player.getHealth() < 10) {
            Modules.disable(this);
            RubyClient.client.getConnection().onDisconnect(new DisconnectionDetails(Component.literal("Dying")));
            return;
        }

        if(this.origin.getY() < -59) {
            Modules.disable(this);
            RubyClient.client.getConnection().onDisconnect(
                    new DisconnectionDetails(Component.literal("Finished excavation")));

            return;
        }

        if(this.xOff == -1 && this.zOff == -1) {
            this.goalBlock = this.origin;
            this.isCentering = true;
            this.breakPos = null;
            this.start = false;
            this.xOff = 0;
            this.zOff = 1;
            return;
        }

        if(this.breakPos != null) {
            if(!this.wasBreakPos) {
                this.lastYaw = RubyClient.client.player.getYRot();
                this.lastPitch = RubyClient.client.player.getXRot();
                this.breakTimer = 0;
                this.wasBreakPos = true;
            }

            RubyClient.client.player.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    Vec3.atCenterOf(this.breakPos)
            );

            int ticks = this.ticksToBreak(this.breakPos);
            RubyClient.client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);

            if(this.breakTimer == 0) {
                RubyClient.client.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                        this.breakPos, Direction.UP
                ));
            } else if(this.breakTimer > ticks) {
                RubyClient.client.gameMode.destroyBlock(this.breakPos);
                RubyClient.client.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        this.breakPos, Direction.UP
                ));

                this.breakPos = null;
                this.breakTimer = -1;
            }

            this.breakTimer++;
            return;
        }

        if(this.goalBlock != null) {
            if(!this.wasGoalBlock) {
                this.lastYaw = RubyClient.client.player.getYRot();
                this.lastPitch = RubyClient.client.player.getXRot();
                this.wasGoalBlock = true;
            }

            BlockPos dir = this.goalBlock.subtract(RubyClient.client.player.blockPosition());
            int dirL2 = dir.getX() * dir.getX() + dir.getZ() * dir.getZ();
            Vec3 vel = RubyClient.client.player.getDeltaMovement();
            double velL2 = vel.x() * vel.x() + vel.z() * vel.z();

            if(
                    velL2 > dirL2 ||
                    !RubyClient.client.level.getBlockState(this.goalBlock).isAir()
            ) {
                this.goalBlock = null;
                this.wasGoalBlock = false;
                this.input.stop();

                RubyClient.client.player.setYRot(this.lastYaw);
                RubyClient.client.player.setXRot(this.lastPitch);
                return;
            }


            this.input.stop();
            this.input.forward(true);
            this.input.sprint(true);

            RubyClient.client.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(this.goalBlock));
            return;
        }

        if(this.isCentering) {
            if(!this.wasCentering) {
                this.lastYaw = RubyClient.client.player.getYRot();
                this.lastPitch = RubyClient.client.player.getXRot();
                this.wasCentering = true;
            }

            // dist from center of block
            double blockOffX = Math.abs(RubyClient.client.player.getX() - (int) RubyClient.client.player.getX()) - 0.5;
            double blockOffZ = Math.abs(RubyClient.client.player.getZ() - (int) RubyClient.client.player.getZ()) - 0.5;

            boolean isWithinX = Math.abs(blockOffX) <= 0.1;
            boolean isWithinZ = Math.abs(blockOffZ) <= 0.1;

            if(isWithinX && isWithinZ) {
                this.wasCentering = false;
                this.isCentering = false;
                this.input.stop();

                RubyClient.client.player.setYRot(this.lastYaw);
                RubyClient.client.player.setXRot(this.lastPitch);
                return;
            }

            this.input.stop();
            this.input.sneak(true);

            RubyClient.client.player.setYRot(0);
            RubyClient.client.player.setXRot(90);

            if(!isWithinX) {
                if(RubyClient.client.player.getX() - (RubyClient.client.player.getBlockX() + 0.5f) > 0)
                    this.input.right(true); else this.input.left(true);
            }

            if(!isWithinZ) {
                if(RubyClient.client.player.getZ() - (RubyClient.client.player.getBlockZ() + 0.5f) > 0)
                    this.input.backward(true); else this.input.forward(true);
            }

            return;
        }

        if(!this.start) {
            this.breakPos = RubyClient.client.player.blockPosition().below();
            this.origin = this.origin.below();
            this.start = true;
            return;
        }

        this.breakPos = this.origin.offset(this.xOff, 1, this.zOff);
        if(!RubyClient.client.level.getBlockState(this.breakPos).isAir()) return;
        this.breakPos = this.origin.offset(this.xOff, 0, this.zOff);
        if(!RubyClient.client.level.getBlockState(this.breakPos).isAir()) return;
        this.goalBlock = this.breakPos; this.breakPos = null;
        if(!RubyClient.client.player.blockPosition().equals(this.goalBlock)) return;
        this.goalBlock = null;

        this.isCentering = this.zOff == 0 || this.zOff == 15;

        this.zOff += this.xOff % 2 == 0 ? 1 : -1;
        if(this.zOff > 15 || this.zOff < 0) {
            this.zOff -= this.xOff % 2 == 0 ? 1 : -1;
            this.xOff++;

            if(this.xOff > 15) {
                this.xOff = -1;
                this.zOff = -1;
            }
        }
    }

//    private int jobID = -1;
//    private BlockPos start;
//    private ArrayList<BlockPos> veins;

    private final String AUTH_KEY = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImIyNDg2Mzc0OTVjYjM4N2U0OWViNmRlMThkZjk5N2VlOGU1YWUyOTciLCJ0eXAiOiJKV1QifQ.eyJwcm92aWRlcl9pZCI6ImFub255bW91cyIsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9kaWdpdGFsYXBwZ3JvdXAtODI4MzUiLCJhdWQiOiJkaWdpdGFsYXBwZ3JvdXAtODI4MzUiLCJhdXRoX3RpbWUiOjE3ODQ2OTIxOTMsInVzZXJfaWQiOiJ3ZWo4R1RHZEw3UWZwa2pIWjl3ZWdLMVRwdTkyIiwic3ViIjoid2VqOEdUR2RMN1FmcGtqSFo5d2VnSzFUcHU5MiIsImlhdCI6MTc4NDY5MjE5MywiZXhwIjoxNzg0Njk1NzkzLCJmaXJlYmFzZSI6eyJpZGVudGl0aWVzIjp7fSwic2lnbl9pbl9wcm92aWRlciI6ImFub255bW91cyJ9fQ.o9phQeD2GBVJjcIGSGvTQMxis91Rx8-1rBwovL2-imeO3fPVEHtjh53Iu_0Hf5WVXbdhIPUyBo8UN0faabb1PcAk8HC-uWwhG1rw42YkRTXH-pOnMdR4x4hsq354kaQ9EOS4deGzzVGtnfMPI_bBkcHN7AGi7FBR8tex8Clbjb7_NlHQqsYf8jF2K6hW3DSjo2ZX1V0NgGRiNKGSE0gdHzgNocLX14ooo8msbEZhKa9NLUSmVmLUWgIdR1oiY-eV36IGFGGcHY098em8c-JBvi_t8ibyHqmMIW2rK83LX3fmc23wx6SPZq5Wg8pVzecxAnkisPoEE0JFy1yhZD0tuw";
    private final String APP_CHECK = "eyJraWQiOiJrMnhhbUEiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxOjk5Njc5ODAzNjA1Nzp3ZWI6NzQxM2E5ZWRiMDE0OTU5ODg5OTk0OCIsImF1ZCI6WyJwcm9qZWN0cy85OTY3OTgwMzYwNTciLCJwcm9qZWN0cy9kaWdpdGFsYXBwZ3JvdXAtODI4MzUiXSwicHJvdmlkZXIiOiJyZWNhcHRjaGFfZW50ZXJwcmlzZSIsImlzcyI6Imh0dHBzOi8vZmlyZWJhc2VhcHBjaGVjay5nb29nbGVhcGlzLmNvbS85OTY3OTgwMzYwNTciLCJleHAiOjE3ODQ2OTU3OTMsImlhdCI6MTc4NDY5MjE5MywianRpIjoiTFA0SGNjcmRva0NxOWNlVWpKTXU5QXg2WEhvVUp6c3FNT3EwaGpXUFZHTSJ9.iUHQbKFxt2ntqpAYBYKO40kSupN6-WSOWLJw5o51jTBA_Jqo9Ha4G12VYDYHsH7Fpc6bxUdv456Q67O7pI0rz20r3kNzNGLdao2ivSeGhu1PaBbfY4_DqSTKFihA05KeYJUFVBgnOyMn2Nc9xhuZupiVQOJAWntwZsdlmjA42MrS6gT8ZSYBGqVaIf0Zw-Vjmbxbzq6ZIgrKZdUS7ye8xOzJbN2QUFW2s-bjsOR9PUYNJh-gmfHa-e7FYLin0nbQPjKuqINHN28Eu46FmDmt1rp9xbIpti02ni-pGEUTy23CiuUEPqDRrvG2NjqKFw4Llp8cHAsFFMXQ-0pmx1b0NdzHT8cxk1LaRt3uNM_pqjASROl2wXXrZjdmozjokhVP_RYxlBe1jZzYz4a41Rg89NvI2h2fSqBK-Eps896YAcANb-ZrH2DoTfIfvNjmJgprV5XM06r_wFHBet6blqwC1LGEFGTG41PWhYBs6s0h8ZK3R8uicuBLSHSTtW9ZKopn";

    @Override
    public void onEnable() {
        if(true) return;
//        this.hitCounts.clear();
//        this.states.clear();

        if(RubyClient.client.player == null) return;

        this.input = new MovementInput();
        this.prevInput = RubyClient.client.player.input;
        RubyClient.client.player.input = this.input;
        this.origin = new BlockPos(
                (RubyClient.client.player.getBlockX() >> 4) << 4,
                RubyClient.client.player.getBlockY(),
                (RubyClient.client.player.getBlockZ() >> 4) << 4
        );

        this.goalBlock = this.origin;
        this.isCentering = true;
        this.breakPos = null;
        this.start = false;
        this.xOff = 0;
        this.zOff = 1;

//        this.start = RubyClient.client.player.blockPosition().below();
//
//        if(this.jobID == -1) return;
//
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest req = null;
//        try {
//            req = HttpRequest.newBuilder()
//                    .uri(new URI("https://api.finddiamonds.net/dfw-api/job/" + this.jobID))
//                    .GET()
//                    .header("Authorization", "Bearer " + this.AUTH_KEY)
//                    .header("X-Firebase-Appcheck", this.APP_CHECK)
//                    .build();
//        } catch(Exception ignored) {}
//        if(req == null) return;
//
//        HttpResponse<String> res = null;
//        try {
//            res = client.send(req, HttpResponse.BodyHandlers.ofString());
//        } catch(Exception ignored) {}
//        if(res == null) return;
//
//        if(res.body().isBlank()) {
//            this.notifyUser("Unfinished");
//            return;
//        }
//
//        JsonObject response = JsonParser.parseString(res.body()).getAsJsonObject();
//        JsonObject real = JsonParser.parseString(response.getAsJsonPrimitive("responseData").getAsString()).getAsJsonObject();
//        JsonArray results = real.get("main_target_data").getAsJsonArray();
//
//        ArrayList<BlockPos> veins = new ArrayList<>();
//        for(JsonElement vein : results) {
//            JsonObject v = vein.getAsJsonObject();
//            veins.add(new BlockPos(
//                    v.getAsJsonPrimitive("x").getAsInt(),
//                    v.getAsJsonPrimitive("y").getAsInt(),
//                    v.getAsJsonPrimitive("z").getAsInt()
//            ));
//        }
//
//        this.veins = veins;
//        this.jobID = -1;
    }

    @Override
    public void onDisable() {
        if(true) return;
        if(RubyClient.client.player == null) return;
        RubyClient.client.player.input = this.prevInput;

//        if(this.jobID != -1) return;
//
//        JsonObject root = new JsonObject();
//        root.addProperty("mcVersion", "java-1.21.4");
//        root.addProperty("seed", "-8173417094276519295");
//        root.addProperty("mainTarget", "diamonds");
//        root.addProperty("playerX", RubyClient.client.player.getBlockX());
//        root.addProperty("playerZ", RubyClient.client.player.getBlockZ());
//
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest req = null;
//        try {
//            req = HttpRequest.newBuilder()
//                    .uri(new URI("https://api.finddiamonds.net/dfw-api/job"))
//                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
//                    .header("Content-Type", "application/json")
//                    .header("Authorization", "Bearer " + this.AUTH_KEY)
//                    .header("X-Firebase-Appcheck", this.APP_CHECK)
//                    .build();
//        } catch(Exception ignored) {}
//        if(req == null) return;
//
//        HttpResponse<String> res = null;
//        try {
//            res = client.send(req, HttpResponse.BodyHandlers.ofString());
//        } catch(Exception ignored) {}
//
//        if(res == null) return;
//        if(res.body().isBlank()) return;
//
//        JsonObject job = JsonParser.parseString(res.body()).getAsJsonObject();
//        if(!job.has("jobId")) {
//            this.notifyUser(job.toString());
//            return;
//        }
//
//        this.jobID = job.getAsJsonPrimitive("jobId").getAsInt();
    }

    @Override
    public void render2D(Render2DEvent event) {
        if(RubyClient.client.level == null) return;
        if(RubyClient.client.player == null) return;

        for(Entity entity : RubyClient.client.level.entitiesForRendering()) {
            if(!(entity instanceof Wolf wolf)) continue;
            if(!wolf.isBaby()) continue;

            Vec2 screen = Renderer.worldToScreen(entity.getEyePosition().add(0, 0.5, 0));
            if(screen == null) continue;

            int millis = ((24000 - wolf.tickCount) * 50) % 1000;
            int seconds = (int) ((24000.0 - wolf.tickCount) / 20) % 60;
            int minutes = (int) ((24000.0 - wolf.tickCount) / 20 / 60) % 60;

            String text = String.format("%02d:%02d.%03d", minutes, seconds, millis);
            int w = GUIStyle.get().monospaceFont().getWidth(text);
            int h = GUIStyle.get().monospaceFont().fontHeight;
            int x = (int) screen.x - w / 2;
            int y = (int) screen.y;

            event.getContext().fill(x - 1, y - 1, x + w + 2, y + h + 1, 0xFF000000);
            GUIStyle.get().monospaceFont().draw(
                    event.getContext(),
                    text, x, y,
                    0xFFFF00FF
            );
        }
    }

    @Override
    public void render3D() {
        if(RubyClient.client.level == null) return;
        if(RubyClient.client.player == null) return;

        Renderer.setMode(Renderer.Mode.FILL_RESPECT_DEPTH);

        RandomSource rand = RandomSource.create(0);
        PerlinNoise noise = new PerlinNoise(rand);
        BlockPos origin = new BlockPos(-100, 200, -400);
        Long2BooleanOpenHashMap map = new Long2BooleanOpenHashMap();

        float angleX = (float) Math.toRadians(5);
        float angleZ = (float) Math.toRadians(15);
        float cosAX = Mth.cos(angleX);
        float sinAX = Mth.sin(angleX);
        float cosAZ = Mth.cos(angleZ);
        float sinAZ = Mth.sin(angleZ);

        for(int r = 80; r <= 120; r++) {
            for(int pz = -r; pz <= r; pz++) {
                for(int px = -r; px <= r; px++) {
                    if(Math.abs(px * px + pz * pz - r * r) > r) continue;

                    float x = px * cosAZ;
                    float y = pz * sinAX + px * sinAZ;
                    float z = pz * cosAX;

                    Renderer.color(0x996600FF);

                    int ix = Math.round(x);
                    int iy = Math.round(y);
                    int iz = Math.round(z);

                    double sample = (noise.get(ix / 10f, iy / 10f, iz / 10f) + 1) / 2;
                    if(sample < 0.45) Renderer.color(0x9900CCCC);

                    BlockPos pos = new BlockPos(
                            origin.getX() + ix,
                            origin.getY() + iy,
                            origin.getZ() + iz
                    );

                    if(!RubyClient.client.level.getBlockState(pos).isAir()) continue;
                    if(map.containsKey(pos.hashCode())) continue;

                    map.put(pos.hashCode(), true);
                    Renderer.cuboid(
                            pos.getX() + 0.5f,
                            pos.getY() + 0.5f,
                            pos.getZ() + 0.5f,
                            0.5f, 0.5f, 0.5f
                    );
                }
            }
        }

        int width = 60;
        int height = 100;

        for(float yStep = -1; yStep <= 1; yStep += 1f / height) {
            int radius = (int) (width * (1 - Math.abs(yStep)));
            int y = (int) (yStep * height);
            float t = Math.clamp(yStep, -0.25f, 0.25f) * 2 + 0.5f;
            float t2 = Math.max(0, yStep - 0.5f) * 2;

            for(int x = -radius; x <= radius; x++) {
                for(int z = -radius; z <= radius; z++) {
                    if(Math.abs(x) + Math.abs(z) != radius + Math.min(Math.abs(x), Math.abs(z)) / 2) continue;

                    if(yStep > 0 && rand.nextFloat() < 1.25 * t2) continue;

                    boolean lightColor = rand.nextFloat() < t;

                    if(lightColor) Renderer.color(0x99FFFFFF);
                    else Renderer.color(0x99333333);

                    double sample = (noise.get(x / 10f, y / 10f, z / 10f) + 1) / 2;
                    if(sample < 0.45) {
                        if(lightColor) Renderer.color(0x99CCCCFF);
                        else Renderer.color(0x33663399);
                    }

                    BlockPos pos = new BlockPos(
                            origin.getX() + x,
                            origin.getY() + y,
                            origin.getZ() + z
                    );

                    if(map.containsKey(pos.hashCode())) continue;
                    map.put(pos.hashCode(), true);

                    Renderer.cuboid(
                            pos.getX() + 0.5f,
                            pos.getY() + 0.5f,
                            pos.getZ() + 0.5f,
                            0.5f, 0.5f, 0.5f
                    );
                }
            }
        }

        if(true) return;

        Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
        Renderer.color(0xFF00FF);
        for(Entity entity : RubyClient.client.level.entitiesForRendering()) {
            if(!(entity instanceof Wolf wolf)) continue;
            if(!wolf.isOwnedBy(RubyClient.client.player)) continue;
            if(wolf.isInSittingPose()) continue;

            Renderer.cuboid(
                    wolf.position().x(),
                    wolf.position().y() + wolf.getBbHeight() / 2,
                    wolf.position().z(),
                    wolf.getBbWidth(),
                    wolf.getBbHeight(),
                    wolf.getBbWidth()
            );
        }

        if(true) return;

        Long2ObjectMap<BlockState> states = new Long2ObjectOpenHashMap<>();

        Renderer.color(0x33FF0000);
        Renderer.setMode(Renderer.Mode.FILL_RESPECT_DEPTH);

        for(int y = -20; y <= 20; y++) {
            for(int z = -20; z <= 20; z++) {
                for(int x = -20; x <= 20; x++) {
                    BlockPos p = RubyClient.client.player.blockPosition().offset(x, y, z);
                    BlockState s = states.get(p.asLong());
                    if(s == null) {
                        s = RubyClient.client.level.getBlockState(p);
                        states.put(p.asLong(), s);
                    }

                    if(!s.isAir()) continue;
                    if(RubyClient.client.level.getLightEngine().getRawBrightness(p, 0) != 0) continue;

//                    BlockPos up = p.up();
//                    BlockState us = states.get(up.asLong());
//                    if(us == null) {
//                        us = RubyClient.client.level.getBlockState(up);
//                        states.put(up.asLong(), us);
//                    }
//
//                    if(!us.isAir()) continue;

                    BlockPos dp = p.below();
                    BlockState ds = states.get(dp.asLong());
                    if(ds == null) {
                        ds = RubyClient.client.level.getBlockState(dp);
                        states.put(dp.asLong(), ds);
                    }

                    if(!ds.entityCanStandOnFace(
                            RubyClient.client.level, dp,
                            RubyClient.client.player, Direction.UP
                    )) continue;

                    Renderer.quad(
                            p.getX() + 0, p.getY() + 0.001, p.getZ() + 0,
                            p.getX() + 1, p.getY() + 0.001, p.getZ() + 0,
                            p.getX() + 1, p.getY() + 0.001, p.getZ() + 1,
                            p.getX() + 0, p.getY() + 0.001, p.getZ() + 1
                    );
                }
            }
        }

//        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//        Renderer.color(0x00DDCC);
//
//        double realD = 9999;
//        double rayD = 9999;
//        BlockPos realPos = null;
//        BlockPos rayPos = null;
//        for(int y = -25; y <= 25; y++) {
//            for(int z = -75; z <= 75; z++) {
//                for(int x = -75; x <= 75; x++) {
//                    BlockPos p = RubyClient.client.player.blockPosition().offset(x, y, z);
//                    if(
////                            !RubyClient.client.level.getBlockState(p).isOf(Blocks.NETHER_QUARTZ_ORE) &&
//                            !RubyClient.client.level.getBlockState(p).isOf(Blocks.NETHER_BRICKS)
//                    ) continue;
//
//                    Renderer.block(p.getX(), p.getY(), p.getZ());
//                    int d2 = x * x + (y - 1) * (y - 1) + z * z;
//                    if(d2 < realD) {
//                        realD = d2;
//                        realPos = p;
//                    }
//
////                    HitResult res = HitResults.raycast(new RaycastContext(
////                            RubyClient.client.player.getEyePosition(),
////                            p.toCenterPos(),
////                            RaycastContext.ShapeType.OUTLINE,
////                            RaycastContext.FluidHandling.NONE,
////                            RubyClient.client.player
////                    ));
////
////                    if(!(res instanceof BlockHitResult hit) || !hit.blockPosition().equals(p)) continue;
////                    if(d2 < rayD) {
////                        rayD = d2;
////                        rayPos = p;
////                    }
//                }
//            }
//        }
//
//        if(realPos != null) {
//            Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//            Vec3d center = Renderer.getScreenCenter();
//            Renderer.line(center, realPos.toCenterPos());
//        }
//
//        if(rayPos != null) {
//            Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//            Vec3d center = Renderer.getScreenCenter();
//            Renderer.color(0xFF0000);
//            Renderer.line(center, rayPos.toCenterPos());
//        }

        if(true) return;

        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
        if(this.breakPos != null) {
            Renderer.color(0xFF0000);
            Renderer.block(this.breakPos.getX(), this.breakPos.getY(), this.breakPos.getZ());
        }

        if(this.goalBlock != null) {
            int x = this.goalBlock.getX();
            int y = this.goalBlock.getY();
            int z = this.goalBlock.getZ();

            Renderer.color(0x00FF00);
            Renderer.line(x + 0, y, z + 0, x + 0, y + 2, z + 0);
            Renderer.line(x + 0, y, z + 1, x + 0, y + 2, z + 1);
            Renderer.line(x + 1, y, z + 0, x + 1, y + 2, z + 0);
            Renderer.line(x + 1, y, z + 1, x + 1, y + 2, z + 1);

            Renderer.color(0xFF0000);
            Renderer.line(Vec3.atBottomCenterOf(this.goalBlock), Vec3.atBottomCenterOf(RubyClient.client.player.blockPosition()));
        }

        if(this.isCentering) {
            double x = RubyClient.client.player.getBlockX() + 0.5;
            double y = RubyClient.client.player.getBlockY();
            double z = RubyClient.client.player.getBlockZ() + 0.5;

            Renderer.color(0x0000FF);
            Renderer.cuboid(x, y, z, 0.1, 0, 0.1);
        }

//        if(this.veins == null || this.veins.isEmpty()) return;
//
//        BlockPos closest = null;
//        double cDist = 99999;
//
//        for(BlockPos pos : this.veins) {
//            BlockState state = RubyClient.client.level.getBlockState(pos);
//            if(state == null) continue;
//            if(state.isAir()) continue;
//
//            Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
//            Renderer.color(0x1100FFCC);
//            Renderer.block(state, pos);
//
//            Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//            Renderer.color(0xFF00FFCC);
//            Renderer.block(state, pos);
//
//            if(RubyClient.client.player.blockPosition().getSquaredDistance(pos) < cDist) {
//                cDist = RubyClient.client.player.blockPosition().getSquaredDistance(pos);
//                closest = pos;
//            }
//        }
//
//        if(closest != null) {
//            Vec3d center = Renderer.getScreenCenter();
//            Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//            Renderer.color(0xFF00FFCC);
//            Renderer.line(
//                    center.getX(),
//                    center.getY(),
//                    center.getZ(),
//                    closest.getX() + 0.5,
//                    closest.getY() + 0.5,
//                    closest.getZ() + 0.5
//            );
//        }

//        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
//        Renderer.color(0x33FFFF);
//
//        Vec3d center = Renderer.getScreenCenter();
//
//        for(int y = -20; y <= 20; y++) {
//            for(int z = -20; z <= 20; z++) {
//                for(int x = -20; x <= 20; x++) {
//                    BlockPos pos = RubyClient.client.player.blockPosition().offset(x, y, z);
//
//                    BlockState state = this.states.get(pos);
//                    if(!this.states.containsKey(pos)) {
//                        state = RubyClient.client.level.getBlockState(pos);
//                        this.states.put(pos, state);
//                        this.hitCounts.put(pos, new BooleanArrayList());
//                    }
//
//                    if(!state.isOf(Blocks.DIAMOND_ORE) && !state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) continue;
//
//                    Vec3d p = pos.toCenterPos().addRandom(RubyClient.client.level.random, 5);
//                    HitResult res = HitResults.raycast(new RaycastContext(
//                            p, pos.toCenterPos(),
//                            RaycastContext.ShapeType.COLLIDER,
//                            RaycastContext.FluidHandling.NONE,
//                            RubyClient.client.player
//                    ));
//
//                    BooleanList curHCs = this.hitCounts.get(pos);
//
//                    curHCs.add(res instanceof BlockHitResult hit && hit.blockPosition().equals(pos));
//                    if((double) curHCs.stream().filter(Boolean::booleanValue).toList().size() / curHCs.size() < 0.2) continue;
//
//                    Renderer.line(center, pos.toCenterPos());
//                    Renderer.block(state, pos);
//                }
//            }
//        }
    }
}
