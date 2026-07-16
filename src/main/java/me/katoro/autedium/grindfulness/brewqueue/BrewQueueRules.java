package me.katoro.autedium.grindfulness.brewqueue;

// cap bounds for the user-editable config value — module clamps with
// Math.clamp so a hand-edited 999 doesn't turn a brewing stand into a chest.
// cost is untouched: ingredients are paid the moment they're queued, blaze
// powder and brew time stay vanilla.
public final class BrewQueueRules {
	public static final int MIN_CAP = 1;
	public static final int MAX_CAP = 8;

	private BrewQueueRules() {}
}
