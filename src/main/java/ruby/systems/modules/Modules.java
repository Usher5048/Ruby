package ruby.systems.modules;

import ruby.RubyClient;
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
        // Combat
        register(new Criticals());
        register(new AutoTotem());
        register(new AutoArmor());
        register(new Hitboxes());
        register(new Reach());

        // Movement
        register(new Sprint());
        register(new Fly());
        register(new NoFall());
        register(new Step());
        register(new Velocity());
        register(new AirJump());
        register(new Speed());
        register(new HighJump());
        register(new AutoJump());
        register(new Spider());
        register(new Parkour());
        register(new Sneak());
        register(new SafeWalk());
        register(new AntiVoid());
        register(new FastClimb());
        register(new GUIMove());
        register(new Jesus());
        register(new AutoWalk());
        register(new Dolphin());
        register(new LongJump());
        register(new ElytraBoost());
        register(new TridentBoost());
        register(new NoSlow());

        // Player
        register(new AutoEat());
        register(new AutoTool());
        register(new ChestSwap());
        register(new Portals());
        register(new AutoRespawn());
        register(new FastPlace());

        // Render
        register(new Fullbright());
        register(new ActiveModules());

        // World
        register(new Nuker());
        register(new Timer());
        register(new Scaffold());

        // Exploit
        register(new ClickTP());

        // Misc
        register(new AutoReconnect());
    }

    private static void register(Module module) {
        modules.add(module);
    }

    public static ArrayList<Module> getModules() {
        return Modules.modules;
    }

    public static List<Module> getByCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.category() == category) result.add(module);
        }
        return result;
    }

    public static ArrayList<Module> getActiveModules() {
        return Modules.activeModules;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getByClass(Class<T> _class) {
        for (Module module : modules)
            if (module.getClass() == _class) return (T) module;

        return null;
    }

    public static Module getByName(String name) {
        for (Module module : modules)
            if (module.name().equalsIgnoreCase(name)) return module;

        return null;
    }

    public static void enable(Module module) {
        if (module == null) return;
        if (module.enabled()) return;

        Modules.activeModules.add(module);

        if (module.showsToasts())
            RubyClient.notifyUser("Enabled " + module);

        module.enabled = true;
        module.onEnable();
    }

    public static void disable(Module module) {
        if (module == null) return;
        if (!module.enabled()) return;

        Modules.activeModules.remove(module);

        if (module.showsToasts())
            RubyClient.notifyUser("Disabled " + module);

        module.enabled = false;
        module.onDisable();
    }

    public static void toggle(Module module) {
        if (module == null) return;
        if (!module.enabled()) Modules.enable(module);
        else Modules.disable(module);
    }

    public static void setEnabled(Module module, boolean isEnabled) {
        if (module == null) return;
        if (module.enabled != isEnabled)
            Modules.toggle(module);
    }
}

