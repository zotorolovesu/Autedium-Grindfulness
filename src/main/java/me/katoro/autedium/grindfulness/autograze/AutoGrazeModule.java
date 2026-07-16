package me.katoro.autedium.grindfulness.autograze;

import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.core.PerPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

// designated-slot auto-eat: hunger drops to the threshold and a plain food
// from your chosen hotbar slot gets eaten for you. full vanilla nutrition
// paid from your stack, ~eat-time cooldown between bites, and a hard combat
// lockout — clutch eating stays your job. weight 3.
public final class AutoGrazeModule implements GrindModule {
	// per-player "no eating before this game time"
	private final PerPlayer<Long> nextEatTick = new PerPlayer<>();
	// per-player tickCount of the last observed hurtTime>0 — catches environmental
	// damage (fire/drowning) that the mob-only vanilla timestamps miss
	private final PerPlayer<Integer> lastDamageTick = new PerPlayer<>();

	@Override
	public String id() {
		return "auto_graze";
	}

	@Override
	public int fairnessWeight() {
		return 3;
	}

	@Override
	public void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!enabled()) return;
			for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
				tickPlayer(sp);
			}
		});

	}

	private void tickPlayer(ServerPlayer sp) {
		// creative hunger bar is decorative — don't burn their food
		if (sp.isDeadOrDying() || sp.isSpectator() || sp.isCreative()) return;

		GrindConfig config = GrindConfig.get();
		long gameTime = sp.level().getGameTime();
		if (sp.hurtTime > 0) lastDamageTick.put(sp.getUUID(), sp.tickCount);
		boolean combatLocked = AutoGrazeRules.combatLocked(
			sp.hurtTime, sp.tickCount, sp.getLastHurtByMobTimestamp(), sp.getLastHurtMobTimestamp(),
			lastDamageTick.getOrDefault(sp.getUUID(), 0));
		boolean cooldownOver = AutoGrazeRules.cooldownOver(
			gameTime, nextEatTick.getOrDefault(sp.getUUID(), Long.MIN_VALUE));

		if (!AutoGrazeRules.shouldEat(sp.getFoodData().getFoodLevel(), config.grazeThreshold, combatLocked, cooldownOver)) return;

		ItemStack stack = sp.getInventory().getItem(AutoGrazeRules.slotToIndex(config.grazeSlot));
		FoodProperties food = stack.get(DataComponents.FOOD);
		if (food == null || !isPlainFood(stack)) return;

		// full vanilla food properties, item paid from the slot
		sp.getFoodData().eat(food);
		stack.shrink(1);
		sp.level().playSound(null, sp.blockPosition(),
			net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 1.0f);

		// eat-time approximation: instant consume, then locked out ~32 ticks
		nextEatTick.put(sp.getUUID(), gameTime + AutoGrazeRules.EAT_COOLDOWN_TICKS);
	}

	// plain nutrition only: no consume effects (golden apples etc. stay manual)
	// and no use remainder (soups/stews return a bowl — conserving that here is
	// dupe-adjacent plumbing, so those stay manual too)
	private static boolean isPlainFood(ItemStack stack) {
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		if (consumable != null && !consumable.onConsumeEffects().isEmpty()) return false;
		return stack.get(DataComponents.USE_REMAINDER) == null;
	}
}
