package me.katoro.autedium.grindfulness.lastbreath;

// pure math for the death-spot reminder — no MC imports, plain JUnit territory.
// items despawn 6000 ticks after death; we keep reminding for a small grace
// window past that (chunk might've been unloaded, countdown paused server-side)
// then give up and clear.
public final class LastBreathRules {
	public static final int DESPAWN_TICKS = 6000;  // 5 min, vanilla item despawn
	public static final int GRACE_TICKS = 200;     // 10s of "yeah it's gone" slack
	public static final int REMIND_INTERVAL = 100; // action bar + glow re-send cadence
	public static final int GLOW_TICKS = 220;      // > interval so the column never flickers, << 1200 clamp
	public static final double CLEAR_DIST_SQR = 8.0 * 8.0;

	private LastBreathRules() {}

	// ticks until drops despawn; negative = already gone
	public static long despawnTicksLeft(long deathGameTime, long nowGameTime) {
		return DESPAWN_TICKS - (nowGameTime - deathGameTime);
	}

	// countdown ran out + grace — stop nagging, clear the marker
	public static boolean expired(long deathGameTime, long nowGameTime) {
		return despawnTicksLeft(deathGameTime, nowGameTime) + GRACE_TICKS <= 0;
	}

	// player walked back to the spot — marker's job is done
	public static boolean reached(double distSqr) {
		return distSqr <= CLEAR_DIST_SQR;
	}

	public static boolean shouldRemind(long nowGameTime) {
		return nowGameTime % REMIND_INTERVAL == 0;
	}

	// "4:37" style m:ss, clamped at 0:00
	public static String formatCountdown(long ticksLeft) {
		long seconds = Math.max(ticksLeft, 0) / 20;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}
}
