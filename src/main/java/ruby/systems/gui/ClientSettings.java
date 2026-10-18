package ruby.systems.gui;

import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ConfigManager;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.Value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Click GUI preferences that are not a single module. */
// TODO: Put this bullshit with the rest of the client configs in RubyClient.config, maybe per profile?
public final class ClientSettings {
    private static final int HOLD_MILLIS = 2200;

    private boolean toasts = true;
    private boolean moduleToasts = true;
    private boolean toastLeft = true;
    private boolean animations = true;
    private boolean hints = true;
    private boolean keybinds = true;
    private int holdMillis = HOLD_MILLIS;
    private int maxToasts = 30;
    private double dropdownScale = 1.2;
    private boolean perspective;

    private BooleanValue toastsValue;
    private BooleanValue moduleToastsValue;
    private BooleanValue animationsValue;
    private BooleanValue hintsValue;
    private BooleanValue keybindsValue;
    private DoubleValue holdValue;
    private EnumValue<Side> sideValue;
    private EnumValue<CommandPrefix> prefixValue;
    private IntegerValue limitValue;
    private IntegerValue dimValue;
    private DoubleValue dropdownValue;
    private BooleanValue perspectiveValue;
    private List<Section> sections;
    private int revision;

    private ClientSettings() {}

    public static ClientSettings get() {
        return Holder.INSTANCE;
    }

    public boolean toasts() {
        return this.toasts;
    }

    public boolean moduleToasts() {
        return this.moduleToasts;
    }

    public boolean toastLeft() {
        return this.toastLeft;
    }

    public boolean animations() {
        return this.animations;
    }

    public boolean hints() {
        return this.hints;
    }

    public boolean showKeybinds() {
        return this.keybinds;
    }

    /** Size of the rows inside an open dropdown. 1.2 is the normal size. Module cards stay put. */
    public float dropdownScale() {
        this.preferDefault();
        return (float) Math.clamp(this.dropdownScale, 0.75, 1.35);
    }

    /** The first version of this slider defaulted to 1. */
    private static boolean preferApplied;

    private void preferDefault() {
        if (ClientSettings.preferApplied) return;
        ClientSettings.preferApplied = true;
        if (Math.abs(this.dropdownScale - 1.0) < 0.001) this.dropdownScale = 1.2;
        if (this.dropdownValue != null) this.dropdownValue.setQuiet(this.dropdownScale);
    }

    /** Bumps when the settings list gains a control, so an open panel can rebuild. */
    public int revision() {
        this.ensure();
        this.installDropdownScale();
        this.installExperimental();
        return this.revision;
    }

    public float holdSeconds() {
        return Math.clamp(this.holdMillis / 1000f, 0.6f, 8f);
    }

    public int maxToasts() {
        return Math.clamp(this.maxToasts, 1, 30);
    }

    /** One step of an expand animation. Off snaps to the end. */
    public static float expandStep(float dt) {
        if (!ClientSettings.get().animations) return 1f;
        return GuiTime.toSeconds(dt) / Motion.EXPAND;
    }

    public boolean perspective() {
        return this.perspective;
    }

    public List<Section> sections() {
        this.ensure();
        this.installDropdownScale();
        this.installExperimental();
        return this.sections;
    }

    public void reset() {
        this.toasts = true;
        this.moduleToasts = true;
        this.toastLeft = true;
        this.animations = true;
        this.hints = true;
        this.keybinds = true;
        this.holdMillis = HOLD_MILLIS;
        this.maxToasts = 30;
        this.push();
    }

    public void write(ByteArrayOutputStream stream) {
        int flags = 0;
        if (this.toasts) flags |= 1;
        if (this.moduleToasts) flags |= 2;
        if (this.animations) flags |= 4;
        if (this.hints) flags |= 8;
        if (this.keybinds) flags |= 16;
        if (this.toastLeft) flags |= 32;
        ConfigManager.writeInt(stream, flags);
        ConfigManager.writeShort(stream, this.holdMillis);
        // Why are we writing this twice (ThemeManager.writeToProfile)
//        stream.write(Math.clamp(ThemeManager.get().dimPercent(), 0, 100));
        stream.write(this.maxToasts());
        stream.write(Math.round(this.dropdownScale() * 100f));
        stream.write(this.perspective ? 1 : 0);
    }

    public void read(ByteArrayInputStream stream) {
        if (stream.available() < 8) return;
        int flags = ConfigManager.readInt(stream);
        this.toasts = (flags & 1) != 0;
        this.moduleToasts = (flags & 2) != 0;
        this.animations = (flags & 4) != 0;
        this.hints = (flags & 8) != 0;
        this.keybinds = (flags & 16) != 0;
        this.toastLeft = (flags & 32) != 0;
        this.holdMillis = ConfigManager.readShort(stream);
//        int dim = stream.read();
        this.maxToasts = stream.read();
        if (stream.available() >= 1) {
            int percent = stream.read();
            if (percent > 0) this.dropdownScale = percent / 100.0;
        }
        if (stream.available() >= 1) this.perspective = stream.read() != 0;
//        ThemeManager.get().setDimQuiet(dim);
        this.push();
    }

    private void ensure() {
        if (this.sections != null) return;
        this.toastsValue = this.bool("Toasts", this.toasts, value -> this.toasts = value);
        this.moduleToastsValue = this.bool("Module toasts", this.moduleToasts, value -> this.moduleToasts = value);
        this.holdValue = (DoubleValue) new DoubleValue.Builder("Duration")
                .defaultValue(this.holdSeconds())
                .range(0.6, 8, 0.1)
                .changed(value -> this.holdMillis = Math.round(value.floatValue() * 1000f))
                .build();
        this.sideValue = this.side();
        this.limitValue = (IntegerValue) new IntegerValue.Builder("Limit")
                .defaultValue(this.maxToasts)
                .range(1, 30)
                .changed(value -> this.maxToasts = value)
                .build();
        this.animationsValue = this.bool("Animations", this.animations, value -> this.animations = value);
        this.dimValue = (IntegerValue) new IntegerValue.Builder("Dim")
                .defaultValue(ThemeManager.get().dimPercent())
                .range(0, 100)
                .changed(value -> ThemeManager.get().setDimPercent(value))
                .build();
        this.hintsValue = this.bool("Hints", this.hints, value -> this.hints = value);
        this.keybindsValue = this.bool("Keybinds", this.keybinds, value -> this.keybinds = value);
        this.dropdownValue = this.dropdownScaleValue();
        this.prefixValue = this.prefix();
        this.perspectiveValue = this.bool("Perspective interface", this.perspective, value -> this.perspective = value);

        List<Value<?>> colors = new ArrayList<>(ThemeManager.get().palette());
        this.sections = List.of(
                new Section("Colors", colors),
                new Section("Toasts", List.of(
                        this.toastsValue, this.moduleToastsValue, this.holdValue, this.sideValue, this.limitValue)),
                new Section("Interface", List.of(this.animationsValue, this.dimValue, this.dropdownValue)),
                new Section("Other", List.of(this.hintsValue, this.keybindsValue, this.prefixValue)),
                new Section("Experimental", List.of(this.perspectiveValue))
        );
    }

    private void push() {
        if (this.toastsValue == null) return;
        this.toastsValue.setQuiet(this.toasts);
        this.moduleToastsValue.setQuiet(this.moduleToasts);
        this.animationsValue.setQuiet(this.animations);
        this.hintsValue.setQuiet(this.hints);
        this.keybindsValue.setQuiet(this.keybinds);
        this.holdValue.setQuiet((double) this.holdSeconds());
        this.sideValue.setQuiet(this.toastLeft ? Side.Left : Side.Right);
        this.limitValue.setQuiet(this.maxToasts());
        this.dimValue.setQuiet(ThemeManager.get().dimPercent());
        if (this.dropdownValue != null) this.dropdownValue.setQuiet(this.dropdownScale);
        if (this.perspectiveValue != null) this.perspectiveValue.setQuiet(this.perspective);
    }

    /** Live settings were built before the experimental section existed. */
    private void installExperimental() {
        if (this.perspectiveValue != null) return;
        this.perspectiveValue = this.bool("Perspective interface", this.perspective, value -> this.perspective = value);
        if (this.sections == null) return;
        List<Section> next = new ArrayList<>(this.sections);
        next.add(new Section("Experimental", List.of(this.perspectiveValue)));
        this.sections = List.copyOf(next);
        this.revision++;
    }

    /** Live settings were built before the dropdown slider existed. */
    private void installDropdownScale() {
        if (this.dropdownValue != null) return;
        this.dropdownValue = this.dropdownScaleValue();
        if (this.sections == null) return;
        List<Section> next = new ArrayList<>();
        for (Section section : this.sections) {
            if (!"Interface".equals(section.name())) {
                next.add(section);
                continue;
            }
            List<Value<?>> values = new ArrayList<>(section.values());
            values.add(this.dropdownValue);
            next.add(new Section(section.name(), List.copyOf(values)));
        }
        this.sections = List.copyOf(next);
        this.revision++;
    }

    private DoubleValue dropdownScaleValue() {
        return (DoubleValue) new DoubleValue.Builder("Dropdown scale")
                .defaultValue(1.2)
                .range(0.75, 1.35, 0.05)
                .changed(value -> this.dropdownScale = value)
                .build();
    }

    private BooleanValue bool(String name, boolean current, java.util.function.Consumer<Boolean> changed) {
        return (BooleanValue) new BooleanValue.Builder(name).defaultValue(current).changed(changed).build();
    }

    private EnumValue<Side> side() {
        return (EnumValue<Side>) new EnumValue.Builder<Side>("Side")
                .defaultValue(this.toastLeft ? Side.Left : Side.Right)
                .changed(value -> this.toastLeft = value == Side.Left)
                .build();
    }

    private EnumValue<CommandPrefix> prefix() {
        CommandPrefix current = CommandPrefix.from(RubyClient.chatPrefix.value());
        return (EnumValue<CommandPrefix>) new EnumValue.Builder<CommandPrefix>("Command prefix")
                .defaultValue(current)
                .changed(value -> RubyClient.chatPrefix.setValue(value.text))
                .build();
    }

    public enum Side {
        Left, Right
    }

    public enum CommandPrefix {
        DOT("."),
        COMMA(","),
        BANG("!"),
        HASH("#");

        final String text;

        CommandPrefix(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return this.text;
        }

        static CommandPrefix from(String text) {
            if (text != null) {
                for (CommandPrefix prefix : CommandPrefix.values()) {
                    if (prefix.text.equals(text)) return prefix;
                }
            }
            return DOT;
        }
    }

    public record Section(String name, List<Value<?>> values) {}

    private static final class Holder {
        private static final ClientSettings INSTANCE = new ClientSettings();
    }
}
