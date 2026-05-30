package ruby.systems.modules;

import ruby.RubyClient;
import ruby.systems.events.Events;
import ruby.systems.events.render.Render2DEvent;
import ruby.systems.events.render.Render3DEvent;
import ruby.systems.events.tick.TickEvents;
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
        Modules.modules.add(new AutoTotem());
        Modules.modules.add(new Criticals());
        Modules.modules.add(new Hitboxes());
        Modules.modules.add(new Reach());
        Modules.modules.add(new ShieldBreaker());

        // Exploit
        Modules.modules.add(new ClickTP());

        // Misc
        Modules.modules.add(new AutoReconnect());

        // Movement
        Modules.modules.add(new AirJump());
        Modules.modules.add(new AntiVoid());
        Modules.modules.add(new AutoJump());
        Modules.modules.add(new AutoWalk());
        Modules.modules.add(new Dolphin());
        Modules.modules.add(new ElytraBoost());
        Modules.modules.add(new FastClimb());
        Modules.modules.add(new Flight());
        Modules.modules.add(new GUIMove());
        Modules.modules.add(new HighJump());
        Modules.modules.add(new Jesus());
        Modules.modules.add(new LongJump());
        Modules.modules.add(new NoFall());
        Modules.modules.add(new NoSlow());
        Modules.modules.add(new Parkour());
        Modules.modules.add(new SafeWalk());
        Modules.modules.add(new Sneak());
        Modules.modules.add(new Speed());
        Modules.modules.add(new Spider());
        Modules.modules.add(new Sprint());
        Modules.modules.add(new Step());
        Modules.modules.add(new TridentBoost());
        Modules.modules.add(new Velocity());

        // Player
        Modules.modules.add(new AutoEat());
        Modules.modules.add(new AutoRespawn());
        Modules.modules.add(new AutoTool());
        Modules.modules.add(new ChestSwap());
        Modules.modules.add(new FastPlace());
        Modules.modules.add(new Portals());

        // Render
        Modules.modules.add(new ActiveModules());
        Modules.modules.add(new Fullbright());

        // World
        Modules.modules.add(new Nuker());
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

    public static void resetMovementKeys() {
        if (RubyClient.client == null || RubyClient.client.options == null) return;

        RubyClient.client.options.forwardKey.setPressed(false);
        RubyClient.client.options.backKey.setPressed(false);
        RubyClient.client.options.leftKey.setPressed(false);
        RubyClient.client.options.rightKey.setPressed(false);
        RubyClient.client.options.jumpKey.setPressed(false);
        RubyClient.client.options.sneakKey.setPressed(false);
        RubyClient.client.options.sprintKey.setPressed(false);
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
        if(module == null) return;
        if(module.enabled != isEnabled)
            Modules.toggle(module);
    }

    public static void tick(Events.GenericEvent event) {
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
        for(Module module : Modules.activeModules)
            module.render2D(event);
    }

    public static void render3D(Render3DEvent event) {
        for(Module module : Modules.activeModules)
            module.render3D(event);
    }
}

