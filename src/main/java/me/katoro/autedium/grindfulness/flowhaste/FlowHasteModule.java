package me.katoro.autedium.grindfulness.flowhaste;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.core.PerPlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.UUID;

public final class FlowHasteModule implements GrindModule {
	private static final int EFFECT_DURATION_TICKS = 100; // 5s, refreshed every break

	private static FlowHasteModule instance;

	// PerPlayer evicts on disconnect — closes the old never-evicts leak
	private final PerPlayer<FlowStreak> streaks = new PerPlayer<>();

	public FlowHasteModule() {
		instance = this;
	}

	// read-only peek for the grind ledger's "best streak" line
	public static int currentStreak(UUID playerId) {
		FlowHasteModule m = instance;
		if (m == null) return 0;
		FlowStreak s = m.streaks.get(playerId);
		return s == null ? 0 : s.count();
	}

	@Override
	public String id() {
		return "flow_haste";
	}

	@Override
	public int fairnessWeight() {
		return 1;
	}

	@Override
	public void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!enabled() || level.isClientSide() || !(player instanceof ServerPlayer sp)) return;

			BlockFamilies.Family family = BlockFamilies.family(state);
			if (family == BlockFamilies.Family.NONE) return;

			FlowStreak streak = streaks.computeIfAbsent(sp.getUUID(), u -> new FlowStreak());
			int count = streak.onBreak(family.name(), level.getGameTime(), GrindConfig.get().flowResetTicks);
			int amplifier = FlowStreak.amplifier(count);
			if (amplifier >= 0) {
				// visible=false so no particle spam, icon still shows tho
				sp.addEffect(new MobEffectInstance(MobEffects.HASTE, EFFECT_DURATION_TICKS, amplifier, false, false));
			}
		});
	}
}
