package me.katoro.autedium.grindfulness.pinbook;

import me.katoro.autedium.grindfulness.core.GrindModule;

// registry stub — pin book is client-only, the real stuff lives in PinBookClient
public final class PinBookModule implements GrindModule {
	@Override
	public String id() {
		return "pin_book";
	}

	@Override
	public int fairnessWeight() {
		return 0; // pure info, no teleports ever
	}

	@Override
	public void init() {
		// nothing here, client half hooks in GrindfulnessClient
	}
}
