package ruby.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.RubyClient;
import ruby.helpers.InventoryHelper;
import ruby.helpers.inventory.InventoryManager;
import ruby.systems.modules.Modules;
import ruby.systems.modules.player.InventoryTweaks;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    protected abstract Slot getHoveredSlot(double xPosition, double yPosition);

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ruby$blockMouseClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if(InventoryHelper.isLocked() || InventoryManager.isOperating()) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ruby$blockKeyPress(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if(InventoryHelper.isLocked() || InventoryManager.isOperating()) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void shiftDragMove(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> info) {
        if(InventoryHelper.isLocked() || InventoryManager.isOperating()) return;

        InventoryTweaks tweaks = Modules.getByClass(InventoryTweaks.class);
        if(tweaks == null || !tweaks.enabled()) return;
        if(!tweaks.shiftDragMove.value()) return;

        if(click.button() != InputConstants.MOUSE_BUTTON_LEFT) return;
        if(!RubyClient.client.hasShiftDown()) return;
        if(RubyClient.client.player == null || RubyClient.client.gameMode == null) return;

        Slot slot = this.getHoveredSlot(click.x(), click.y());
        if(slot == null || !slot.hasItem()) return;

        RubyClient.client.gameMode.handleContainerInput(
                RubyClient.client.player.containerMenu.containerId,
                slot.index,
                click.button(),
                ContainerInput.QUICK_MOVE,
                RubyClient.client.player
        );
    }
}
