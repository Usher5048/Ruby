package ruby.systems.modules;

public enum ModuleType {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    EXPLOIT("Exploit"),
    MISC("Miscellaneous");

    private final String name;
    ModuleType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}