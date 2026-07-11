package me.katoro.autedium.grindfulness.toasts;

import me.katoro.autedium.grindfulness.core.GrindModule;

public final class ToastsModule implements GrindModule {
	@Override
	public String id() {
		return "toasts";
	}

	@Override
	public int fairnessWeight() {
		return 0; // pure feedback, never counts against the verdict
	}

	@Override
	public void init() {
		// nothing here, the real stuff lives in ToastsClient
	}
}
