package ruby.helpers.inventory;

import java.util.List;

public record InventoryActionChain(
        InventoryConstraints constraints,
        List<InventoryAction> actions,
        InventoryPriority priority
) {
    public boolean requiresInventoryOpen() {
        return this.actions.stream().anyMatch(InventoryAction::requiresInventoryOpen);
    }
}
