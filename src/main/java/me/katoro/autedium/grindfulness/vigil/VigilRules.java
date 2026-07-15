package me.katoro.autedium.grindfulness.vigil;

// pure math for the vigil channel — NO minecraft imports, unit tested.
// multiplier honesty is the whole point: expected extra random ticks per block
// per game tick must equal (multiplier - 1) * vanilla rate, no more. rework 2
// adds skyrim-wait duration math: player picks N game hours, channel delivers
// (multiplier - 1) tick-equivalents per tick until N * 1000 delivered.
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
	// skyrim-wait slider bounds: 1-8 game hours in 0.5 steps, one game hour = 1000 ticks
	public static final long TICKS_PER_GAME_HOUR = 1000L;
	public static final double MIN_WAIT_HOURS = 1.0;
	public static final double MAX_WAIT_HOURS = 8.0;

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

	// guards #1, #2, #5 — any true breaker ends the channel. sneak is no longer
	// part of the deal (rework 2): kneel = stand still, movement is the input breaker.
	public static boolean shouldBreak(boolean isDay, int foodLevel, int hungerFloor,
	                                  boolean moved, boolean damaged) {
		return !isDay || foodLevel < hungerFloor || moved || damaged;
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

	// ---- rework 2: duration-target math ----

	// game ticks of daylight left before NIGHT_START (0 if already night)
	public static long remainingDaytime(long dayTime) {
		long t = dayTime % DAY_LENGTH;
		if (t < 0) t += DAY_LENGTH;
		return t >= NIGHT_START ? 0 : NIGHT_START - t;
	}

	// slider ceiling: min(8h, remaining daytime) snapped DOWN to a 0.5 step.
	// can dip below MIN_WAIT_HOURS near dusk — caller must refuse then.
	public static double maxWaitHours(long remainingDaytime) {
		double capped = Math.min(MAX_WAIT_HOURS, remainingDaytime / (double) TICKS_PER_GAME_HOUR);
		return Math.floor(capped * 2.0) / 2.0;
	}

	// server-side sanitizer for the client's requested hours: snap to 0.5 steps,
	// clamp into [1, maxWaitHours]. returns 0 if there isn't a full hour of
	// daylight left (request refused).
	public static double clampWaitHours(double hours, long remainingDaytime) {
		double max = maxWaitHours(remainingDaytime);
		if (max < MIN_WAIT_HOURS) return 0.0;
		double snapped = Math.round(hours * 2.0) / 2.0;
		return Math.max(MIN_WAIT_HOURS, Math.min(snapped, max));
	}

	// target = hours x 1000 game-time-equivalents to deliver
	public static long targetTicks(double hours) {
		return Math.round(hours * TICKS_PER_GAME_HOUR);
	}

	// delivered per channel tick = the EXTRA tick-equivalents (multiplier - 1)
	public static int deliveredPerTick(int multiplier) {
		return clampMultiplier(multiplier) - 1;
	}

	// how many real server ticks until the target is delivered. multiplier 1
	// delivers nothing -> never completes; callers must refuse to start.
	public static long channelDurationTicks(long targetTicks, int multiplier) {
		int per = deliveredPerTick(multiplier);
		if (per <= 0) return Long.MAX_VALUE;
		return (targetTicks + per - 1) / per;
	}

	// live readout: real seconds the channel will run for N hours
	public static double estimatedRealSeconds(double hours, int multiplier) {
		long dur = channelDurationTicks(targetTicks(hours), multiplier);
		return dur == Long.MAX_VALUE ? Double.POSITIVE_INFINITY : dur / 20.0;
	}

	// live readout: total exhaustion the full wait will cost
	public static float estimatedExhaustion(double hours, int multiplier) {
		long dur = channelDurationTicks(targetTicks(hours), multiplier);
		return dur == Long.MAX_VALUE ? Float.POSITIVE_INFINITY : exhaustionPerTick(multiplier) * dur;
	}

	// vanilla: 4 exhaustion drains 1 saturation/food point
	public static double estimatedFoodCost(double hours, int multiplier) {
		return estimatedExhaustion(hours, multiplier) / 4.0;
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
