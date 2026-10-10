package ruby.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.helpers.world.ChunkReloadHelper;
import ruby.systems.commands.Command;
import ruby.systems.commands.Commands;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

import java.util.Arrays;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
	@Inject(at = @At("HEAD"), method = "sendChat", cancellable = true)
	public void onChatMessage(String message, CallbackInfo info) {
		String chatPrefix = RubyClient.chatPrefix.value().toLowerCase();
		if(!message.toLowerCase().startsWith(chatPrefix)) return;

		info.cancel();
		RubyClient.client.gui.hud.getChat().addRecentChat(message);

		String[] args = message.substring(chatPrefix.length()).split(" ");
		String command = args[0].toLowerCase();
		args = Arrays.copyOfRange(args, 1, args.length);

		Command cmd = Commands.getByName(command);
		if(cmd == null) RubyClient.notifyUser("Unknown command: " + command);
		else cmd.execute(args);
	}

	@Inject(method = "handleLogin", at = @At("TAIL"))
	private void ruby$reloadXrayOnJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
		Xray xray = Modules.getByClass(Xray.class);
		if (xray != null && xray.enabled()) {
			ChunkReloadHelper.schedule();
		}
	}
}