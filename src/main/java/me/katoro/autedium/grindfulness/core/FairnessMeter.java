package me.katoro.autedium.grindfulness.core;

import java.util.Map;

public final class FairnessMeter {
	private FairnessMeter() {}

	// add up weights of whatevers on. works on unsaved toggle state too, thats what the live label uses
	public static int score(Map<String, Boolean> enabledById) {
		int total = 0;
		for (GrindModule m : ModuleRegistry.all()) {
			if (enabledById.getOrDefault(m.id(), false)) {
				total += m.fairnessWeight();
			}
		}
		return total;
	}

	// translation key suffix -> autedium_grindfulness.verdict.<key>
	public static String verdictKey(int score) {
		return switch (Math.min(Math.max(score, 0), 5)) {
			case 0 -> "vanilla_purist";
			case 1 -> "balanced";
			case 2 -> "comfy";
			case 3 -> "pushing_it";
			case 4 -> "grinders_gambit";
			default -> "wow_kinda_unfair";
		};
	}
}
