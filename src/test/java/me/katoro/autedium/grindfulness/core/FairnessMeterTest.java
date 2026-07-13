package me.katoro.autedium.grindfulness.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FairnessMeterTest {
	private record TestModule(String id, int fairnessWeight) implements GrindModule {
		public void init() {}
	}

	@BeforeEach
	void setup() {
		ModuleRegistry.clearForTests();
		ModuleRegistry.register(new TestModule("vein_miner", 1));
		ModuleRegistry.register(new TestModule("flow_haste", 1));
		ModuleRegistry.register(new TestModule("pity_ping", 1));
		ModuleRegistry.register(new TestModule("prospector", 2));
		ModuleRegistry.register(new TestModule("toasts", 0));
	}

	private static Map<String, Boolean> only(String... on) {
		var m = new java.util.HashMap<String, Boolean>();
		for (String s : on) m.put(s, true);
		return m;
	}

	@Test
	void allOnIsMaxUnfair() {
		int s = FairnessMeter.score(only("vein_miner", "flow_haste", "pity_ping", "prospector", "toasts"));
		assertEquals(5, s);
		assertEquals("wow_kinda_unfair", FairnessMeter.verdictKey(s));
	}

	@Test
	void veinAloneIsBalanced() {
		assertEquals("balanced", FairnessMeter.verdictKey(FairnessMeter.score(only("vein_miner"))));
	}

	@Test
	void toastsAreFree() {
		assertEquals("vanilla_purist", FairnessMeter.verdictKey(FairnessMeter.score(only("toasts"))));
	}

	@Test
	void tiers() {
		assertEquals("vanilla_purist", FairnessMeter.verdictKey(0));
		assertEquals("balanced", FairnessMeter.verdictKey(1));
		assertEquals("comfy", FairnessMeter.verdictKey(2));
		assertEquals("pushing_it", FairnessMeter.verdictKey(3));
		assertEquals("grinders_gambit", FairnessMeter.verdictKey(4));
		assertEquals("wow_kinda_unfair", FairnessMeter.verdictKey(5));
		assertEquals("wow_kinda_unfair", FairnessMeter.verdictKey(9));
		assertEquals("vanilla_purist", FairnessMeter.verdictKey(-3));
	}
}
