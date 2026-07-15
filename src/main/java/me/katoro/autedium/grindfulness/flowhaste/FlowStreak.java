package me.katoro.autedium.grindfulness.flowhaste;

public final class FlowStreak {
	private String family;
	private long lastBreak = Long.MIN_VALUE;
	private int count;

	// new streak count after this break
	public int onBreak(String newFamily, long gameTime, int resetTicks) {
		if (!newFamily.equals(family) || gameTime - lastBreak > resetTicks) {
			count = 0;
		}
		family = newFamily;
		lastBreak = gameTime;
		return ++count;
	}

	public int count() {
		return count;
	}

	// 5 breaks = haste I, 15 = II, 30 = III, under 5 u get nothing (-1)
	public static int amplifier(int streak) {
		if (streak >= 30) return 2;
		if (streak >= 15) return 1;
		if (streak >= 5) return 0;
		return -1;
	}
}
