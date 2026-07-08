package me.katoro.autedium.grindfulness.core;

public interface GrindModule {
	String id();

	int fairnessWeight();

	// hook ur events here, runs once at startup
	void init();

	default boolean enabled() {
		return GrindConfig.get().isEnabled(id());
	}
}
