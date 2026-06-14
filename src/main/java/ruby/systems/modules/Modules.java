package ruby.systems.modules;

import ruby.RubyClient;
import ruby.systems.events.Events;
import ruby.systems.events.Render2DEvent;
import ruby.systems.events.TickEvents;
import ruby.systems.modules.combat.*;
import ruby.systems.modules.exploit.*;
import ruby.systems.modules.misc.*;
import ruby.systems.modules.movement.*;
import ruby.systems.modules.player.*;
import ruby.systems.modules.render.*;
import ruby.systems.modules.world.*;

import java.util.ArrayList;
import java.util.List;

public class Modules {
    private static final ArrayList<Module> activeModules = new ArrayList<>();
    private static final ArrayList<Module> modules = new ArrayList<>();

    static {
        Events.TICK.register(TickEvents.BEGIN, Modules::tick);
        Events.RENDER2D.register(Modules::render2D);
        Events.RENDER3D.register(Modules::render3D);

        // Combat
        Modules.modules.add(new AutoArmor());
        Modules.modules.add(new AutoGapple());
        Modules.modules.add(new AutoPot());
        Modules.modules.add(new AutoTotem());
        Modules.modules.add(new Criticals());
        Modules.modules.add(new FakeLag());
        Modules.modules.add(new Hitboxes());
        Modules.modules.add(new KillAura());
        Modules.modules.add(new Reach());
        Modules.modules.add(new ShieldBreaker());
        Modules.modules.add(new Velocity());
        Modules.modules.add(new WTap());

        // Exploit
        Modules.modules.add(new ClickTP());

        // Misc
        Modules.modules.add(new AntiVanish());
        Modules.modules.add(new AutoReconnect());
        Modules.modules.add(new GamemodeNotifier());

        // Movement
        Modules.modules.add(new AirJump());
        Modules.modules.add(new AntiVoid());
        Modules.modules.add(new AutoJump());
        Modules.modules.add(new AutoWalk());
        Modules.modules.add(new Boost());
        Modules.modules.add(new Dolphin());
        Modules.modules.add(new ElytraBoost());
        Modules.modules.add(new FastClimb());
        Modules.modules.add(new Flight());
        Modules.modules.add(new HighJump());
        Modules.modules.add(new InventoryMove());
        Modules.modules.add(new Jesus());
        Modules.modules.add(new LongJump());
        Modules.modules.add(new NoFall());
        Modules.modules.add(new NoJumpDelay());
        Modules.modules.add(new NoPush());
        Modules.modules.add(new NoSlow());
        Modules.modules.add(new Parkour());
        Modules.modules.add(new SafeWalk());
        Modules.modules.add(new Sneak());
        Modules.modules.add(new Speed());
        Modules.modules.add(new Spider());
        Modules.modules.add(new Sprint());
        Modules.modules.add(new Step());
        Modules.modules.add(new TridentBoost());

        // Player
        Modules.modules.add(new AutoEat());
        Modules.modules.add(new AutoRespawn());
        Modules.modules.add(new AutoTool());
        Modules.modules.add(new BreakDelay());
        Modules.modules.add(new ChestSwap());
        Modules.modules.add(new ExpThrower());
        Modules.modules.add(new FastPlace());
        Modules.modules.add(new InventoryTweaks());
        Modules.modules.add(new Portals());
        Modules.modules.add(new SpeedMine());

        // Render
        Modules.modules.add(new ActiveModules());
        Modules.modules.add(new BlockESP());
        Modules.modules.add(new ESP());
        Modules.modules.add(new Freecam());
        Modules.modules.add(new FreeLook());
        Modules.modules.add(new Fullbright());
        Modules.modules.add(new Hud());
        Modules.modules.add(new LogoutSpots());
        Modules.modules.add(new Nametags());
        Modules.modules.add(new StorageESP());
        Modules.modules.add(new TextureTweaks());
        Modules.modules.add(new Trajectories());
        Modules.modules.add(new Tracers());
        Modules.modules.add(new Xray());
        Modules.modules.add(new Zoom());

        // World
        Modules.modules.add(new AirPlace());
        Modules.modules.add(new Nuker());
        Modules.modules.add(new Extinguish());
        Modules.modules.add(new Scaffold());
        Modules.modules.add(new Timer());
    }

    public static ArrayList<Module> getModules() {
        return Modules.modules;
    }

    public static List<Module> getByType(ModuleType type) {
        List<Module> result = new ArrayList<>();
        for(Module module : Modules.modules)
            if(module.category() == type) result.add(module);

        return result;
    }

    public static ArrayList<Module> getActiveModules() {
        return Modules.activeModules;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getByClass(Class<T> _class) {
        for(Module module : Modules.modules)
            if(module.getClass() == _class) return (T) module;

        return null;
    }

    public static Module getByName(String name) {
        for(Module module : Modules.modules)
            if(module.name().equalsIgnoreCase(name)) return module;

        return null;
    }

    public static void enable(Module module) {
        if(module == null) return;
        if(module.enabled()) return;

        Modules.activeModules.add(module);

        if(module.showsToasts())
            RubyClient.notifyUser("Enabled " + module);

        module.enabled = true;
        module.onEnable();
    }

    public static void disable(Module module) {
        if(module == null) return;
        if(!module.enabled()) return;

        Modules.activeModules.remove(module);

        if(module.showsToasts())
            RubyClient.notifyUser("Disabled " + module);

        module.enabled = false;
        module.onDisable();
    }

    public static void toggle(Module module) {
        if(module == null) return;
        if(!module.enabled()) Modules.enable(module);
        else Modules.disable(module);
    }

    public static void setEnabled(Module module, boolean isEnabled) {
        if (module == null) return;
        if (module.enabled != isEnabled) Modules.toggle(module);
    }

    public static void disableAllSilently() {
        for (Module module : Modules.modules) {
            Modules.disableSilently(module);
        }
    }

    public static void disableSilently(Module module) {
        if (module == null || !module.enabled()) return;
        Modules.activeModules.remove(module);
        module.enabled = false;
        Modules.runOnRenderThread(module::onDisable);
    }

    public static void enableSilently(Module module) {
        if (module == null || module.enabled()) return;
        Modules.activeModules.add(module);
        module.enabled = true;
        Modules.runOnRenderThread(module::onEnable);
    }

    private static void runOnRenderThread(Runnable action) {
        if (RubyClient.client.isOnThread()) action.run();
        else RubyClient.client.execute(action);
    }

    public static void setEnabledSilently(Module module, boolean isEnabled) {
        if (module == null) return;
        if (isEnabled) Modules.enableSilently(module);
        else Modules.disableSilently(module);
    }

    public static void tick(Events.GenericEvent e) {
        for(Module module : modules) {
            if(module.keybind.isUnbound()) continue;
            if(module.keybind.isPressed()) Modules.toggle(module);
            if(module.keybind.togglesOnRelease() && module.keybind.wasPressed())
                Modules.toggle(module);
        }

        for(Module module : Modules.activeModules)
            module.tick();
    }

    public static void render2D(Render2DEvent event) {
        event.getContext().getMatrices().pushMatrix();
        event.getContext().getMatrices().scale(
                1f / RubyClient.client.getWindow().getScaleFactor(),
                1f / RubyClient.client.getWindow().getScaleFactor()
        );

        for(Module module : Modules.activeModules)
            module.render2D(event);

        event.getContext().getMatrices().popMatrix();
    }

    public static void render3D(Events.GenericEvent e) {
        for(Module module : Modules.activeModules)
            module.render3D();
    }
}

