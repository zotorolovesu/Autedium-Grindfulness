package me.katoro.autedium.grindfulness.torchcadence;

// pure decision math for auto-torching — no MC imports, plain JUnit territory.
// the module feeds in block light + "is there a torch nearby" and this says
// place or don't. torches always come out of the held stack (full cost paid).
public final class TorchCadenceRules {
	public static final int CHECK_INTERVAL = 10; // server ticks between checks
	public static final int SPACING = 2;         // no new torch if one within 2 blocks
	public static final int MIN_THRESHOLD = 0;
	public static final int MAX_THRESHOLD = 14;

	private TorchCadenceRules() {}

	public static boolean shouldCheck(long gameTime) {
		return gameTime % CHECK_INTERVAL == 0;
	}

	// threshold is user-editable json — clamped so a hand-edited 999 doesn't
	// carpet the world in torches
	public static boolean lightTooLow(int blockLight, int threshold) {
		return blockLight < Math.clamp(threshold, MIN_THRESHOLD, MAX_THRESHOLD);
	}

	// the whole verdict: dark enough AND not clustering
	public static boolean shouldPlace(int blockLight, int threshold, boolean torchNearby) {
		return lightTooLow(blockLight, threshold) && !torchNearby;
	}
}
