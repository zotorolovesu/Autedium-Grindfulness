package me.katoro.autedium.grindfulness.encore;

// pure eligibility math for the breeding-cooldown buyout — no MC imports.
// vanilla: adult breeds, then getAge() counts down a ~5-min cooldown before
// it can love again. encore: pay DOUBLE the breeding item (2 instead of the
// eventual 1) to zero that cooldown. per extra baby the pair costs 4 bypass
// + 2 love items vs vanilla's 2 + wait — resources for time, vigil precedent.
public final class EncoreRules {
	public static final int ITEMS_REQUIRED = 2;

	private EncoreRules() {}

	// only an ADULT on cooldown qualifies: age > 0 is the post-breed countdown.
	// babies (age < 0) and ready adults (age == 0) never — vanilla handles those.
	public static boolean canBuyout(int age, boolean isFood, int stackCount) {
		return age > 0 && isFood && stackCount >= ITEMS_REQUIRED;
	}
}
