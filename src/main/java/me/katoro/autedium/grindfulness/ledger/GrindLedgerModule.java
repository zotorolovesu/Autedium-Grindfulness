package me.katoro.autedium.grindfulness.ledger;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.FairnessMeter;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.core.PerPlayer;
import me.katoro.autedium.grindfulness.core.ModuleRegistry;
import me.katoro.autedium.grindfulness.flowhaste.FlowHasteModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public final class GrindLedgerModule implements GrindModule {
	private static final int BURST_FLOOR = 50;    // idle summary needs a real burst behind it
	private static final int POLL_INTERVAL = 20;  // check idle once a second, plenty

	// session-scoped, cleared on disconnect — NOT the flow-haste never-evicts situation
	private final PerPlayer<LedgerTracker> trackers = new PerPlayer<>();

	@Override
	public String id() {
		return "grind_ledger";
	}

	@Override
	public int fairnessWeight() {
		return 0; // pure info, changes no cost, no loot
	}

	@Override
	public void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!enabled() || level.isClientSide() || !(player instanceof ServerPlayer sp)) return;

			BlockFamilies.Family family = BlockFamilies.family(state);
			if (family == BlockFamilies.Family.NONE) return; // grinding, not door-breaking

			long now = ((ServerLevel) level).getServer().getTickCount();
			LedgerTracker tracker = trackers.computeIfAbsent(sp.getUUID(), u -> new LedgerTracker());
			// flow module registers before us, so its streak is already bumped for this break
			int streak = FlowHasteModule.currentStreak(sp.getUUID());
			tracker.recordStreak(streak);

			if (tracker.onBreak(family.name(), now, GrindConfig.get().ledgerMilestone, idleWindowTicks()) == LedgerTracker.BreakResult.MILESTONE) {
				sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.ledger.milestone", tracker.total(), streak));
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!enabled() || server.getTickCount() % POLL_INTERVAL != 0) return;
			long now = server.getTickCount();
			for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
				LedgerTracker tracker = trackers.get(sp.getUUID());
				if (tracker == null) continue;
				LedgerTracker.IdleSummary summary = tracker.pollIdle(now, idleWindowTicks(), BURST_FLOOR);
				if (summary != null) {
					sp.sendOverlayMessage(idleLine(summary));
				}
			}
		});

	}

	private static long idleWindowTicks() {
		return GrindConfig.get().ledgerIdleSeconds * 20L;
	}

	private static Component idleLine(LedgerTracker.IdleSummary s) {
		String minutes = String.format(Locale.ROOT, "%.1f", s.grindTicks() / 1200.0);
		return Component.translatable("autedium_grindfulness.ledger.idle",
			s.countsByFamily().getOrDefault(BlockFamilies.Family.STONE.name(), 0),
			s.countsByFamily().getOrDefault(BlockFamilies.Family.LOG.name(), 0),
			s.bestStreak(),
			minutes,
			verdict());
	}

	private static Component verdict() {
		Map<String, Boolean> enabledById = new HashMap<>();
		for (GrindModule m : ModuleRegistry.all()) {
			enabledById.put(m.id(), m.enabled());
		}
		return Component.translatable("autedium_grindfulness.verdict." + FairnessMeter.verdictKey(FairnessMeter.score(enabledById)));
	}
}
