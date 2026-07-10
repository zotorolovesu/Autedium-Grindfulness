package me.katoro.autedium.grindfulness;

import me.katoro.autedium.grindfulness.net.GrindNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrindfulnessMod implements ModInitializer {
	public static final String MOD_ID = "autedium_grindfulness";
	public static final Logger LOGGER = LoggerFactory.getLogger("AuTedium-Grindfulness");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		GrindNetworking.registerPayloads();
		LOGGER.info("AuTedium - Grindfulness loading");
	}
}
