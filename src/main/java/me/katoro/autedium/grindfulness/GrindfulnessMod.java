package me.katoro.autedium.grindfulness;

import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.core.ModuleRegistry;
import me.katoro.autedium.grindfulness.flowhaste.FlowHasteModule;
import me.katoro.autedium.grindfulness.ledger.GrindLedgerModule;
import me.katoro.autedium.grindfulness.net.GrindNetworking;
import me.katoro.autedium.grindfulness.pityping.PityPingModule;
import me.katoro.autedium.grindfulness.prospector.ProspectorModule;
import me.katoro.autedium.grindfulness.toasts.ToastsModule;
import me.katoro.autedium.grindfulness.veinminer.VeinMinerModule;
import me.katoro.autedium.grindfulness.vigil.VigilModule;
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
		GrindConfig.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("autedium_grindfulness.json"));
		GrindNetworking.registerPayloads();

		ModuleRegistry.register(new VeinMinerModule());
		ModuleRegistry.register(new FlowHasteModule());
		ModuleRegistry.register(new PityPingModule());
		ModuleRegistry.register(new ProspectorModule());
		ModuleRegistry.register(new ToastsModule());
		ModuleRegistry.register(new GrindLedgerModule());
		ModuleRegistry.register(new VigilModule());
		ModuleRegistry.all().forEach(GrindModule::init);

		LOGGER.info("AuTedium - Grindfulness loaded {} modules", ModuleRegistry.all().size());
	}
}
