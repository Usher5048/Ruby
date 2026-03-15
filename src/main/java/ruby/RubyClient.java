package ruby;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import ruby.systems.config.ConfigManager;
import ruby.systems.config.Configuration;

import java.io.InputStream;

public class RubyClient implements ModInitializer {
	public static final String MOD_ID = "ruby";
	public static final String MOD_NAME = "Ruby";
	public static final String MC_VERSION = SharedConstants.getGameVersion().name();
	public static final String VERSION = "v" + FabricLoader
			.getInstance()
			.getModContainer(RubyClient.MOD_ID)
			.orElseThrow(() -> new RuntimeException("Wtf???"))
			.getMetadata()
			.getVersion()
			.getFriendlyString()
			.split("\\+")[0];

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final MinecraftClient client = MinecraftClient.getInstance();
	public static final Configuration config = new Configuration();

	public static InputStream getResourceStream(String path) {
		return RubyClient.class.getResourceAsStream(String.format(
				"/assets/%s/%s", RubyClient.MOD_ID, path
		));
	}

	public static void log(String message) {
		RubyClient.LOGGER.info("[" + RubyClient.MOD_NAME + "] " + message);
	}
	public static void notifyUser(String message) {
		RubyClient.notifyUser(message, false);
	}
	public static void notifyUserRaw(Text message) {
		RubyClient.notifyUserRaw(message, false);
	}

	public static void notifyUser(String message, boolean actionBar) {
		if(RubyClient.client.player == null) return;
		RubyClient.client.player.sendMessage(
				Text.literal(!actionBar ? "[" : "")
						.append(!actionBar ? Text.literal("Ruby").withColor(0xCC3366) : Text.of(""))
						.append(!actionBar ? "]" : "")
						.append(message),

				actionBar
		);
	}

	public static void notifyUserRaw(Text message, boolean actionBar) {
		if(RubyClient.client.player == null) return;
		RubyClient.client.player.sendMessage(
				Text.literal(!actionBar ? "[" : "")
						.append(!actionBar ? Text.literal("Ruby").withColor(0xCC3366) : Text.of(""))
						.append(!actionBar ? "]" : "")
						.append(message),

				actionBar
		);
	}

	@Override
	public void onInitialize() {
		ConfigManager.loadState();
	}
}

