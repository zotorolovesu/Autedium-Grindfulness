package me.katoro.autedium.grindfulness.vigil;

// pure math for the vigil channel — NO minecraft imports, unit tested.
// multiplier honesty is the whole point: expected extra random ticks per crop
// block per game tick must equal (multiplier - 1) * vanilla rate, no more.
public final class VigilRules {
	// vanilla picks randomTickSpeed positions per 16x16x16 section per tick
	public static final int SECTION_VOLUME = 16 * 16 * 16;
	// "vanilla-idle-equivalent" fuel burn baseline; scaled by multiplier.
	// 8x time = 8x hunger burn (guard #3)
	public static final float EXHAUSTION_BASE_PER_TICK = 0.005f;
	// config hard caps (guard #6)
	public static final int MAX_RADIUS = 16;
	public static final int MAX_MULTIPLIER = 16;
	public static final long DAY_LENGTH = 24000L;
	public static final long NIGHT_START = 12000L;

	private VigilRules() {}

	public static boolean isDay(long dayTime) {
		long t = dayTime % DAY_LENGTH;
		if (t < 0) t += DAY_LENGTH;
		return t < NIGHT_START;
	}

	// guard #1 + #2
	public static boolean canStart(boolean isDay, int foodLevel, int hungerFloor) {
		return isDay && foodLevel >= hungerFloor;
	}

	// guards #1, #2, #5 — any true breaker ends the channel
	public static boolean shouldBreak(boolean isDay, int foodLevel, int hungerFloor,
	                                  boolean moved, boolean damaged, boolean sneaking) {
		return !isDay || foodLevel < hungerFloor || moved || damaged || !sneaking;
	}

	public static int clampRadius(int radius) {
		return Math.max(1, Math.min(radius, MAX_RADIUS));
	}

	public static int clampMultiplier(int multiplier) {
		return Math.max(1, Math.min(multiplier, MAX_MULTIPLIER));
	}

	public static float exhaustionPerTick(int multiplier) {
		return EXHAUSTION_BASE_PER_TICK * clampMultiplier(multiplier);
	}

	// number of blocks in the scanned cube of the given radius
	public static int cubeVolume(int radius) {
		int side = 2 * radius + 1;
		return side * side * side;
	}

	// expected EXTRA random ticks a single block should receive per game tick
	public static double extraTickRate(int multiplier, int randomTickSpeed) {
		return (clampMultiplier(multiplier) - 1) * (double) randomTickSpeed / SECTION_VOLUME;
	}

	// how many random positions to sample from the cube this tick so that each
	// block's expected extra ticks == extraTickRate. mean = volume * rate; we
	// take the whole part guaranteed + one fractional bernoulli roll.
	public static int extraTickAttempts(int multiplier, int randomTickSpeed, int volume, double random01) {
		double mean = volume * extraTickRate(multiplier, randomTickSpeed);
		int whole = (int) mean;
		double frac = mean - whole;
		return whole + (random01 < frac ? 1 : 0);
	}
}
