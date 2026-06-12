package ruby.helpers.inventory;

import ruby.systems.events.Event;

import java.util.ArrayList;
import java.util.List;

public class ScheduleInventoryActionEvent extends Event {
    private final List<InventoryActionChain> chains = new ArrayList<>();

    public void schedule(InventoryConstraints constraints, List<InventoryAction> actions, InventoryPriority priority) {
        if(actions == null || actions.isEmpty()) return;
        this.chains.add(new InventoryActionChain(constraints, List.copyOf(actions), priority));
    }

    public List<InventoryActionChain> chains() {
        return this.chains;
    }
}
