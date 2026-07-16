package me.katoro.autedium.grindfulness.autograze;

// pure eligibility math for auto-eating — no MC imports, plain JUnit territory.
// the module feeds in hunger, combat timing, and slot config; this decides.
// food is always paid in full from the designated slot — the click is removed,
// the cost is not.
public final class AutoGrazeRules {
	public static final int COMBAT_LOCKOUT_TICKS = 100; // no grazing within 100 ticks of dealing/taking damage
	public static final int EAT_COOLDOWN_TICKS = 32;    // ~vanilla eat duration, no machine-gun eating
	public static final int MIN_THRESHOLD = 1;
	public static final int MAX_THRESHOLD = 19;
	public static final int MIN_SLOT = 1;
	public static final int MAX_SLOT = 9;

	private AutoGrazeRules() {}

	// 1-based config slot -> 0-based hotbar inventory index; clamped so a
	// hand-edited json can never hit armor/offhand indices
	public static int slotToIndex(int slot) {
		return Math.clamp(slot, MIN_SLOT, MAX_SLOT) - 1;
	}

	// hungry enough? at-or-below threshold eats; 20 never qualifies (max is 19,
	// clamped so a hand-edited 999 doesn't force-feed at full hunger)
	public static boolean hungryEnough(int foodLevel, int threshold) {
		return foodLevel <= Math.clamp(threshold, MIN_THRESHOLD, MAX_THRESHOLD);
	}

	// COMBAT LOCKOUT: hurtTime still counting down, or dealt/received damage
	// within the last 100 ticks. eating mid-fight stays a manual decision.
	// timestamps are tickCount values; 0 means "never" and tickCount starts
	// at 0, so a fresh player with ts 0 at tick 50 would read as combat —
	// guard with ts > 0.
	// lastObservedDamageTick: module-tracked tick of the last hurtTime>0 sighting —
	// covers environmental damage (fire, drowning, cactus) that the mob-only
	// vanilla timestamps miss; without it a burning player gets fed between pulses
	public static boolean combatLocked(int hurtTime, int tickCount, int lastHurtByTimestamp, int lastHurtMobTimestamp, int lastObservedDamageTick) {
		if (hurtTime > 0) return true;
		return withinLockout(tickCount, lastHurtByTimestamp)
			|| withinLockout(tickCount, lastHurtMobTimestamp)
			|| withinLockout(tickCount, lastObservedDamageTick);
	}

	private static boolean withinLockout(int tickCount, int timestamp) {
		return timestamp > 0 && tickCount - timestamp < COMBAT_LOCKOUT_TICKS;
	}

	// internal eat cooldown — mirrors eat time so grazing isn't faster than hands
	public static boolean cooldownOver(long gameTime, long nextAllowedTick) {
		return gameTime >= nextAllowedTick;
	}

	// the whole verdict, minus item checks (those need MC types)
	public static boolean shouldEat(int foodLevel, int threshold, boolean combatLocked, boolean cooldownOver) {
		return !combatLocked && cooldownOver && hungryEnough(foodLevel, threshold);
	}
}
