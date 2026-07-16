package me.katoro.autedium.grindfulness.autograze;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoGrazeRulesTest {
	@Test
	void thresholdIsAtOrBelow() {
		assertTrue(AutoGrazeRules.hungryEnough(10, 10));
		assertTrue(AutoGrazeRules.hungryEnough(3, 10));
		assertFalse(AutoGrazeRules.hungryEnough(11, 10));
		assertFalse(AutoGrazeRules.hungryEnough(20, 19)); // full hunger never grazes
	}

	@Test
	void thresholdClampsToOneThroughNineteen() {
		// via the real caller — a hand-edited 999 behaves like 19, not force-feed-at-full
		assertTrue(AutoGrazeRules.hungryEnough(19, 999));
		assertFalse(AutoGrazeRules.hungryEnough(20, 999));
		assertTrue(AutoGrazeRules.hungryEnough(1, -5)); // negative clamps to 1
		assertFalse(AutoGrazeRules.hungryEnough(2, -5));
	}

	@Test
	void slotClampsAndMapsToHotbarIndex() {
		assertEquals(0, AutoGrazeRules.slotToIndex(1));
		assertEquals(8, AutoGrazeRules.slotToIndex(9));
		assertEquals(0, AutoGrazeRules.slotToIndex(-3)); // clamped, never a negative index
		assertEquals(8, AutoGrazeRules.slotToIndex(40)); // never reaches armor/offhand slots
	}

	@Test
	void hurtTimeAloneLocksCombat() {
		assertTrue(AutoGrazeRules.combatLocked(5, 1000, 0, 0, 0));
		assertFalse(AutoGrazeRules.combatLocked(0, 1000, 0, 0, 0));
	}

	@Test
	void receivedDamageLocksForHundredTicks() {
		assertTrue(AutoGrazeRules.combatLocked(0, 500, 450, 0, 0));  // hurt 50 ticks ago
		assertTrue(AutoGrazeRules.combatLocked(0, 500, 401, 0, 0));  // 99 ticks ago, still locked
		assertFalse(AutoGrazeRules.combatLocked(0, 500, 400, 0, 0)); // exactly 100 ticks = free
		assertFalse(AutoGrazeRules.combatLocked(0, 500, 300, 0, 0));
	}

	@Test
	void dealtDamageLocksForHundredTicks() {
		assertTrue(AutoGrazeRules.combatLocked(0, 500, 0, 450, 0));
		assertFalse(AutoGrazeRules.combatLocked(0, 500, 0, 350, 0));
	}

	@Test
	void neverFoughtDoesNotReadAsCombat() {
		// vanilla timestamps default to 0; a fresh player at tick 50 must not be locked
		assertFalse(AutoGrazeRules.combatLocked(0, 50, 0, 0, 0));
	}

	@Test
	void cooldownGatesEatRate() {
		assertTrue(AutoGrazeRules.cooldownOver(100, 100));
		assertTrue(AutoGrazeRules.cooldownOver(101, 100));
		assertFalse(AutoGrazeRules.cooldownOver(99, 100));
		assertTrue(AutoGrazeRules.cooldownOver(0, Long.MIN_VALUE)); // never eaten = ready
	}

	@Test
	void verdictNeedsAllThree() {
		assertTrue(AutoGrazeRules.shouldEat(8, 10, false, true));
		assertFalse(AutoGrazeRules.shouldEat(8, 10, true, true));   // combat lockout wins
		assertFalse(AutoGrazeRules.shouldEat(8, 10, false, false)); // still chewing
		assertFalse(AutoGrazeRules.shouldEat(15, 10, false, true)); // not hungry enough
	}

	@Test
	void environmentalDamageLocksViaObservedTick() {
		// burning player: hurtTime pulses to 0 between fire ticks, mob timestamps stay 0.
		// observed-damage tick must carry the 100-tick lockout across the gaps.
		assertTrue(AutoGrazeRules.combatLocked(0, 150, 0, 0, 100));   // 50 ticks after last burn pulse
		assertFalse(AutoGrazeRules.combatLocked(0, 250, 0, 0, 100));  // 150 ticks later — lockout over
		assertFalse(AutoGrazeRules.combatLocked(0, 150, 0, 0, 0));    // never damaged
	}
}
