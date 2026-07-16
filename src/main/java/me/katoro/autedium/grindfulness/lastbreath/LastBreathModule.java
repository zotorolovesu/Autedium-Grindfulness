package me.katoro.autedium.grindfulness.lastbreath;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.katoro.autedium.grindfulness.GrindfulnessMod;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.net.GrindNetworking;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

// pure info death recovery: where you died, how far, how long until the drops
// despawn. no item protection, no despawn changes — just kills the "uhh which
// ravine was it" tedium. weight 0.
public final class LastBreathModule implements GrindModule {
	// death pos + dimension + game time of death. persistent + copyOnDeath so it
	// survives the respawn entity swap (same pattern as PityPingModule.PITY_COUNT)
	public record DeathMark(BlockPos pos, String dimension, long gameTime) {
		public static final Codec<DeathMark> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(DeathMark::pos),
			Codec.STRING.fieldOf("dimension").forGetter(DeathMark::dimension),
			Codec.LONG.fieldOf("game_time").forGetter(DeathMark::gameTime)
		).apply(inst, DeathMark::new));
	}

	public static final AttachmentType<DeathMark> DEATH_MARK = AttachmentRegistry.create(
		GrindfulnessMod.id("death_mark"),
		builder -> builder
			.persistent(DeathMark.CODEC)
			.copyOnDeath()
	);

	private static final int GLOW_ARGB = 0x66FF4040; // translucent blood red

	@Override
	public String id() {
		return "last_breath";
	}

	@Override
	public int fairnessWeight() {
		return 0;
	}

	@Override
	public void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!enabled() || entity.level().isClientSide() || !(entity instanceof ServerPlayer sp)) return;
			// next death overwrites the previous mark, by design
			sp.setAttached(DEATH_MARK, new DeathMark(
				sp.blockPosition(),
				sp.level().dimension().identifier().toString(),
				sp.level().getGameTime()));
		});

		// immediate reminder on respawn, don't make them wait for the tick cadence
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!enabled()) return;
			DeathMark mark = newPlayer.getAttached(DEATH_MARK);
			if (mark != null) remind(newPlayer, mark);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!enabled()) return;
			for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
				DeathMark mark = sp.getAttached(DEATH_MARK);
				if (mark == null || sp.isDeadOrDying()) continue;

				boolean sameDim = sp.level().dimension().identifier().toString().equals(mark.dimension());
				if (sameDim && LastBreathRules.reached(sp.distanceToSqr(
						mark.pos().getX() + 0.5, mark.pos().getY() + 0.5, mark.pos().getZ() + 0.5))) {
					sp.removeAttached(DEATH_MARK);
					sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.last_breath.reached"));
					continue;
				}
				if (LastBreathRules.expired(mark.gameTime(), sp.level().getGameTime())) {
					sp.removeAttached(DEATH_MARK); // countdown + grace done, stop nagging
					continue;
				}
				if (LastBreathRules.shouldRemind(sp.level().getGameTime())) {
					remind(sp, mark);
				}
			}
		});
	}

	private static void remind(ServerPlayer sp, DeathMark mark) {
		BlockPos pos = mark.pos();
		boolean sameDim = sp.level().dimension().identifier().toString().equals(mark.dimension());

		if (!sameDim) {
			// other dimension: coords only, no distance/countdown (game time and
			// distance don't mean anything across the portal)
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.last_breath.bar_other_dim",
				pos.getX(), pos.getY(), pos.getZ(), mark.dimension()));
			return;
		}

		int distance = (int) Math.sqrt(sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
		// countdown only while the death chunk is plausibly loaded — if it's
		// unloaded the item timers are frozen and the number would be a lie
		if (sp.level().isLoaded(pos)) {
			long left = LastBreathRules.despawnTicksLeft(mark.gameTime(), sp.level().getGameTime());
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.last_breath.bar",
				pos.getX(), pos.getY(), pos.getZ(), distance, LastBreathRules.formatCountdown(left)));
		} else {
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.last_breath.bar_far",
				pos.getX(), pos.getY(), pos.getZ(), distance));
		}

		// through-wall glow column at the spot; re-sent every reminder so it never
		// hits the client-side duration clamp
		GrindNetworking.sendGlow(sp,
			List.of(pos, pos.above(), pos.above(2)),
			GLOW_ARGB, LastBreathRules.GLOW_TICKS);
	}
}
