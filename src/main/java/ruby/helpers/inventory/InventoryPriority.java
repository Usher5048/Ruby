package ruby.helpers.inventory;

public enum InventoryPriority {
    NORMAL(0),
    IMPORTANT(10);

    private final int weight;

    InventoryPriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return this.weight;
    }
}
