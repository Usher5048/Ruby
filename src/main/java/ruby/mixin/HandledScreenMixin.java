package ruby.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
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

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    protected abstract Slot getSlotAt(double xPosition, double yPosition);

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ruby$blockMouseClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if(InventoryHelper.isLocked() || InventoryManager.isOperating()) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ruby$blockKeyPress(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if(InventoryHelper.isLocked() || InventoryManager.isOperating()) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void shiftDragMove(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> info) {
        if(InventoryHelper.isLocked() || InventoryManager.isOperating()) return;

        InventoryTweaks tweaks = Modules.getByClass(InventoryTweaks.class);
        if(tweaks == null || !tweaks.enabled()) return;
        if(!tweaks.shiftDragMove.value()) return;

        if(click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if(!RubyClient.client.isShiftPressed()) return;
        if(RubyClient.client.player == null || RubyClient.client.interactionManager == null) return;

        Slot slot = this.getSlotAt(click.x(), click.y());
        if(slot == null || !slot.hasStack()) return;

        RubyClient.client.interactionManager.clickSlot(
                RubyClient.client.player.currentScreenHandler.syncId,
                slot.id,
                click.button(),
                SlotActionType.QUICK_MOVE,
                RubyClient.client.player
        );
    }
}
