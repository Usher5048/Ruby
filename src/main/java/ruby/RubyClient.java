package ruby;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class RubyClient implements ModInitializer {
	public static final String MOD_ID = "ruby";
	public static final String MOD_NAME = "RubyClient";
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

	@Override
	public void onInitialize() {
		RubyClient.LOGGER.info("fasdfasdfasdf");
	}
}

