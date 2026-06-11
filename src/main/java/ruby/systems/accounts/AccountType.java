package ruby.systems.accounts;

public enum AccountType {
    Cracked("Cracked"),
    Microsoft("Microsoft"),
    Session("Session");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
