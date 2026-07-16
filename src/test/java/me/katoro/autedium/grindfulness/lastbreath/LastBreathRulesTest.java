package me.katoro.autedium.grindfulness.lastbreath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastBreathRulesTest {
	@Test
	void countdownStartsAtFullDespawnWindow() {
		assertEquals(6000, LastBreathRules.despawnTicksLeft(100, 100));
	}

	@Test
	void countdownDecreasesWithTime() {
		assertEquals(5900, LastBreathRules.despawnTicksLeft(100, 200));
		assertEquals(0, LastBreathRules.despawnTicksLeft(100, 6100));
		assertEquals(-50, LastBreathRules.despawnTicksLeft(100, 6150));
	}

	@Test
	void expiryIncludesGraceWindow() {
		long death = 1000;
		assertFalse(LastBreathRules.expired(death, death + 6000));            // just despawned, still in grace
		assertFalse(LastBreathRules.expired(death, death + 6000 + 199));      // last grace tick
		assertTrue(LastBreathRules.expired(death, death + 6000 + 200));       // grace over
		assertTrue(LastBreathRules.expired(death, death + 999999));
	}

	@Test
	void reachedIsEightBlockRadius() {
		assertTrue(LastBreathRules.reached(0.0));
		assertTrue(LastBreathRules.reached(64.0));   // exactly 8 blocks
		assertFalse(LastBreathRules.reached(64.01));
		assertFalse(LastBreathRules.reached(100.0));
	}

	@Test
	void remindCadenceIsEveryHundredTicks() {
		assertTrue(LastBreathRules.shouldRemind(0));
		assertTrue(LastBreathRules.shouldRemind(100));
		assertTrue(LastBreathRules.shouldRemind(123400));
		assertFalse(LastBreathRules.shouldRemind(101));
		assertFalse(LastBreathRules.shouldRemind(99));
	}

	@Test
	void glowResendBeatsItsOwnDuration() {
		// column must be re-sent before the previous glow runs out, and stay
		// far under the 1200-tick payload clamp
		assertTrue(LastBreathRules.GLOW_TICKS > LastBreathRules.REMIND_INTERVAL);
		assertTrue(LastBreathRules.GLOW_TICKS < 1200);
	}

	@Test
	void countdownFormatIsMinutesSeconds() {
		assertEquals("5:00", LastBreathRules.formatCountdown(6000));
		assertEquals("0:59", LastBreathRules.formatCountdown(1199)); // 59.95s floors to 59
		assertEquals("1:01", LastBreathRules.formatCountdown(1220));
		assertEquals("0:00", LastBreathRules.formatCountdown(0));
		assertEquals("0:00", LastBreathRules.formatCountdown(-500)); // clamped, never negative
	}
}
