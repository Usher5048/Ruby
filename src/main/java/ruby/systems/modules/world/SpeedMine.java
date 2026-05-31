package ruby.systems.modules.world;

import net.minecraft.block.BlockState;
import ruby.RubyClient;
import ruby.mixin.ClientPlayerInteractionManagerAccessor;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class SpeedMine extends Module {
    public SpeedMine() {
        super("Speed Mine", "Breaks blocks roughly 1.4x faster", ModuleType.WORLD);
    }

    @Override
    public void tick() {
        if(RubyClient.client.interactionManager == null) return;
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.world == null) return;

        ClientPlayerInteractionManagerAccessor accessor = (ClientPlayerInteractionManagerAccessor) RubyClient.client.interactionManager;
        BlockState state = RubyClient.client.world.getBlockState(accessor.ruby$getBreakingPos());
        float progress = accessor.ruby$getBreakingProgress();

        float delta = state.calcBlockBreakingDelta(
                RubyClient.client.player,
                RubyClient.client.world,
                accessor.ruby$getBreakingPos()
        );

        if(progress + delta < 0.7f) return;
        accessor.ruby$setBreakingProgress(1f);
    }
}
