package me.katoro.autedium.grindfulness.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleRegistry {
	private static final List<GrindModule> MODULES = new ArrayList<>();

	private ModuleRegistry() {}

	public static void register(GrindModule module) {
		MODULES.add(module);
	}

	public static List<GrindModule> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<String> ids() {
		return MODULES.stream().map(GrindModule::id).toList();
	}

	static void clearForTests() {
		MODULES.clear();
	}
}
