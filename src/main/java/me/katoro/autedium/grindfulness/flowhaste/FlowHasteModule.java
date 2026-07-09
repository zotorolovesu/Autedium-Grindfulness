package me.katoro.autedium.grindfulness.flowhaste;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlowHasteModule implements GrindModule {
	private static final int EFFECT_DURATION_TICKS = 100; // 5s, refreshed every break

	private final Map<UUID, FlowStreak> streaks = new ConcurrentHashMap<>();

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
