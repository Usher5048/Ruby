package ruby.mixin;

import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;

@Mixin(Window.class)
public class WindowMixin {
	@Inject(at = @At("HEAD"), method = "setTitle", cancellable = true)
	public void setTitle(String title, CallbackInfo info) {
		GLFW.glfwSetWindowTitle(
				RubyClient.client.getWindow().getHandle(),
				title + " - " + RubyClient.MOD_NAME + " " + RubyClient.VERSION
		);

		info.cancel();
	}
}