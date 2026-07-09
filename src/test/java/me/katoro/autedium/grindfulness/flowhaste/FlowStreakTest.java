package me.katoro.autedium.grindfulness.flowhaste;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowStreakTest {
	@Test
	void streakBuildsWithinWindow() {
		FlowStreak s = new FlowStreak();
		long t = 0;
		for (int i = 1; i <= 5; i++) {
			assertEquals(i, s.onBreak("stone", t, 120));
			t += 20; // 1s between breaks
		}
	}

	@Test
	void idleGapResets() {
		FlowStreak s = new FlowStreak();
		s.onBreak("stone", 0, 120);
		s.onBreak("stone", 20, 120);
		assertEquals(1, s.onBreak("stone", 20 + 121, 120)); // >6s gap
	}

	@Test
	void familySwitchResets() {
		FlowStreak s = new FlowStreak();
		s.onBreak("stone", 0, 120);
		s.onBreak("stone", 10, 120);
		assertEquals(1, s.onBreak("log", 20, 120));
	}

	@Test
	void amplifierTiers() {
		assertEquals(-1, FlowStreak.amplifier(4));
		assertEquals(0, FlowStreak.amplifier(5));   // Haste I
		assertEquals(0, FlowStreak.amplifier(14));
		assertEquals(1, FlowStreak.amplifier(15));  // Haste II
		assertEquals(2, FlowStreak.amplifier(30));  // Haste III
		assertEquals(2, FlowStreak.amplifier(500));
	}
}
