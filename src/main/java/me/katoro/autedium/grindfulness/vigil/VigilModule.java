package me.katoro.autedium.grindfulness.vigil;

import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// channeled "wait": kneel down, compress daytime in a radius — ALL random-tick
// behavior (crops, saplings, copper, leaves, grass...) plus furnace-family block
// entities. pay scaled hunger, stay a sitting duck. bends wall-clock — heaviest
// module yet.
//
// generalization choice (spec left it to the implementer): BLANKET random-tick
// acceleration via isRandomlyTicking(), no allowlist tag — the vanilla flag IS
// the allowlist. spawners/entity spawning never random-tick, so guard #4 holds
// by construction. the old crop_family tag is gone.
public final class VigilModule implements GrindModule {
	private static final double MOVE_EPSILON_SQR = 0.01; // ~0.1 blocks of drift = you moved

	// server-side channel map, cleared on disconnect. no persistence (per spec)
	private final Map<UUID, VigilState> channeling = new ConcurrentHashMap<>();

	@Override
	public String id() {
		return "vigil";
	}

	@Override
	public int fairnessWeight() {
		return 4;
	}

	@Override
	public void init() {
		// trigger: sneak + right-click a time-flowing block (randomly ticking, or a
		// furnace-family block entity) with an empty main hand. deliberate by
		// construction — you cant do it while holding anything.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!enabled() || level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer sp) || !sp.isShiftKeyDown()) return InteractionResult.PASS;
			if (!sp.getMainHandItem().isEmpty()) return InteractionResult.PASS;
			BlockState state = level.getBlockState(hit.getBlockPos());
			if (!state.isRandomlyTicking() && !isFurnaceFamily(level.getBlockEntity(hit.getBlockPos()))) return InteractionResult.PASS;

			if (channeling.remove(sp.getUUID()) != null) {
				sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.vigil.end"));
				return InteractionResult.SUCCESS;
			}
			// guard #1 (daytime only) + guard #2 (hunger floor)
			boolean day = VigilRules.isDay(level.getDefaultClockTime());
			if (!VigilRules.canStart(day, sp.getFoodData().getFoodLevel(), GrindConfig.get().vigilHungerFloor)) {
				sp.sendOverlayMessage(Component.translatable(
					day ? "autedium_grindfulness.vigil.too_hungry" : "autedium_grindfulness.vigil.not_day"));
				return InteractionResult.SUCCESS;
			}
			channeling.put(sp.getUUID(), new VigilState(sp.position(), ((ServerLevel) level).getGameTime()));
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.vigil.start"));
			return InteractionResult.SUCCESS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (channeling.isEmpty()) return;
			if (!enabled()) { channeling.clear(); return; }
			for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
				VigilState state = channeling.get(sp.getUUID());
				if (state != null) tickChannel(sp, state);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> channeling.remove(handler.getPlayer().getUUID()));
	}

	private void tickChannel(ServerPlayer sp, VigilState state) {
		ServerLevel level = sp.level() instanceof ServerLevel sl ? sl : null;
		if (level == null || !sp.isAlive()) {
			channeling.remove(sp.getUUID());
			return;
		}

		int floor = GrindConfig.get().vigilHungerFloor;
		boolean day = VigilRules.isDay(level.getDefaultClockTime());
		boolean moved = sp.position().distanceToSqr(state.anchor) > MOVE_EPSILON_SQR;
		boolean damaged = sp.hurtTime > 0; // guard #5: real vulnerability, damage breaks it
		// releasing sneak is the "any input" breaker — kneeling IS the channel
		if (VigilRules.shouldBreak(day, sp.getFoodData().getFoodLevel(), floor, moved, damaged, sp.isShiftKeyDown())) {
			channeling.remove(sp.getUUID());
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.vigil.broken"));
			return;
		}

		int multiplier = VigilRules.clampMultiplier(GrindConfig.get().vigilMultiplier); // guard #6
		int radius = VigilRules.clampRadius(GrindConfig.get().vigilRadius);             // guard #6

		// guard #3: scaled exhaustion — 8x time = 8x hunger burn, every tick
		sp.getFoodData().addExhaustion(VigilRules.exhaustionPerTick(multiplier));

		// multiplier-honest time compression: sample random positions in the cube
		// exactly like vanilla samples sections, so each block's expected extra
		// random ticks == (multiplier - 1) x vanilla rate. never tick-every-block.
		int speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
		RandomSource random = level.getRandom();
		int volume = VigilRules.cubeVolume(radius);
		int attempts = VigilRules.extraTickAttempts(multiplier, speed, volume, random.nextDouble());
		BlockPos center = sp.blockPosition();
		int side = 2 * radius + 1;
		for (int i = 0; i < attempts; i++) {
			BlockPos pos = center.offset(random.nextInt(side) - radius, random.nextInt(side) - radius, random.nextInt(side) - radius);
			BlockState target = level.getBlockState(pos);
			// guard #4: only what vanilla already random-ticks. spawners and entity
			// spawning are not random-tick behavior — unreachable by construction
			if (!target.isRandomlyTicking()) continue;
			target.randomTick(level, pos, random);
			// feedback tied to the actual effect (event-based, iris-safe)
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
				pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.0);
		}

		// furnace-family block entities get (multiplier - 1) extra serverTicks —
		// fuel-per-item ratio preserved by construction, fuel just burns faster.
		// HARD exclusion (guard #4): only these five, never spawners/anything else.
		tickBlockEntities(level, center, radius, multiplier - 1);

		// action bar: vigil · m:ss · hunger bar
		if (level.getGameTime() % 10 == 0) {
			long elapsed = (level.getGameTime() - state.startTick) / 20;
			String clock = String.format(java.util.Locale.ROOT, "%d:%02d", elapsed / 60, elapsed % 60);
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.vigil.bar",
				clock, hungerBar(sp.getFoodData().getFoodLevel())));
		}
	}

	// walk the loaded chunks overlapping the cube, extra-tick furnace-family BEs.
	// chunk BE-map iteration, not a 4913-position cube scan.
	private static void tickBlockEntities(ServerLevel level, BlockPos center, int radius, int extraTicks) {
		if (extraTicks <= 0) return;
		int minCx = (center.getX() - radius) >> 4, maxCx = (center.getX() + radius) >> 4;
		int minCz = (center.getZ() - radius) >> 4, maxCz = (center.getZ() + radius) >> 4;
		for (int cx = minCx; cx <= maxCx; cx++) {
			for (int cz = minCz; cz <= maxCz; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				// copy: tickers can mutate the chunk's BE map while we iterate
				for (BlockEntity be : chunk.getBlockEntities().values().toArray(BlockEntity[]::new)) {
					if (be.isRemoved() || !isFurnaceFamily(be)) continue;
					BlockPos pos = be.getBlockPos();
					if (Math.abs(pos.getX() - center.getX()) > radius
						|| Math.abs(pos.getY() - center.getY()) > radius
						|| Math.abs(pos.getZ() - center.getZ()) > radius) continue;
					tickExtra(level, be, extraTicks);
				}
			}
		}
	}

	// furnace + blast furnace + smoker (all AbstractFurnace), brewing stand, campfire
	private static boolean isFurnaceFamily(BlockEntity be) {
		return be instanceof AbstractFurnaceBlockEntity
			|| be instanceof BrewingStandBlockEntity
			|| be instanceof CampfireBlockEntity;
	}

	@SuppressWarnings("unchecked")
	private static <T extends BlockEntity> void tickExtra(ServerLevel level, T be, int times) {
		BlockState state = be.getBlockState();
		BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) be.getType());
		if (ticker == null) return;
		for (int i = 0; i < times && !be.isRemoved(); i++) {
			ticker.tick(level, be.getBlockPos(), state, be);
		}
	}

	private static String hungerBar(int food) {
		StringBuilder bar = new StringBuilder(11);
		for (int i = 0; i < food / 2; i++) bar.append('█');
		if (food % 2 == 1) bar.append('▌');
		return bar.toString();
	}
}
