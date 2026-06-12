package ruby.helpers.inventory;

public record InventoryConstraints(
        int startDelay,
        int clickDelayMin,
        int clickDelayMax,
        int closeDelay,
        boolean requireInventoryOpen,
        boolean requireNoMovement
) {
    public static InventoryConstraints grimArmor() {
        return new InventoryConstraints(1, 1, 2, 1, true, false);
    }

    public static InventoryConstraints grimTotem() {
        return new InventoryConstraints(1, 2, 4, 1, false, true);
    }

    public int clickDelay() {
        if(this.clickDelayMin >= this.clickDelayMax) return this.clickDelayMin;
        return this.clickDelayMin + (int) (Math.random() * (this.clickDelayMax - this.clickDelayMin + 1));
    }
}
