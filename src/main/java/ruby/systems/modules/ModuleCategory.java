package ruby.systems.modules;

public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    EXPLOIT("Exploit"),
    MISC("Miscellaneous");

    private final String name;
    ModuleCategory(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}