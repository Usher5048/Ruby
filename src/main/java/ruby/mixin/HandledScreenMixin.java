package ruby.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.RubyClient;
import ruby.systems.modules.Modules;
import ruby.systems.modules.player.InventoryTweaks;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    protected abstract Slot getSlotAt(double xPosition, double yPosition);

    @Shadow
    protected abstract void onMouseClick(Slot slot, int invSlot, int clickData, SlotActionType actionType);

    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void shiftDragMove(Click click, double offsetX, double offsetY, CallbackInfoReturnable <Boolean> info) {
        InventoryTweaks tweaks = Modules.getByClass(InventoryTweaks.class);
        if(tweaks == null || !tweaks.enabled()) return;
        if(!tweaks.shiftDragMove.value()) return;

        if(click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if(!RubyClient.client.isShiftPressed()) return;

        Slot slot = this.getSlotAt(click.x(), click.y());
        if(slot == null || !slot.hasStack()) return;

        this.onMouseClick(slot, slot.id, click.button(), SlotActionType.QUICK_MOVE);
    }
}
