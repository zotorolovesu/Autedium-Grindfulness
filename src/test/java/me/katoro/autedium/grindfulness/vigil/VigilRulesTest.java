package me.katoro.autedium.grindfulness.vigil;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VigilRulesTest {

	@Test
	void dayBoundaries() {
		assertTrue(VigilRules.isDay(0));
		assertTrue(VigilRules.isDay(11999));
		assertFalse(VigilRules.isDay(12000));
		assertFalse(VigilRules.isDay(23999));
		assertTrue(VigilRules.isDay(24000)); // wraps
		assertFalse(VigilRules.isDay(24000 + 13000));
	}

	@Test
	void canStartNeedsDayAndFood() {
		assertTrue(VigilRules.canStart(true, 14, 14));
		assertTrue(VigilRules.canStart(true, 20, 14));
		assertFalse(VigilRules.canStart(false, 20, 14)); // night
		assertFalse(VigilRules.canStart(true, 13, 14));  // under floor
	}

	@Test
	void breakConditions() {
		assertFalse(VigilRules.shouldBreak(true, 14, 14, false, false, true));
		assertTrue(VigilRules.shouldBreak(false, 20, 14, false, false, true)); // night falls
		assertTrue(VigilRules.shouldBreak(true, 13, 14, false, false, true));  // hunger drops below floor
		assertTrue(VigilRules.shouldBreak(true, 20, 14, true, false, true));   // moved
		assertTrue(VigilRules.shouldBreak(true, 20, 14, false, true, true));   // damaged
		assertTrue(VigilRules.shouldBreak(true, 20, 14, false, false, false)); // released sneak (input)
	}

	@Test
	void configCaps() {
		assertEquals(16, VigilRules.clampRadius(999));
		assertEquals(1, VigilRules.clampRadius(0));
		assertEquals(8, VigilRules.clampRadius(8));
		assertEquals(16, VigilRules.clampMultiplier(64));
		assertEquals(1, VigilRules.clampMultiplier(-3));
		assertEquals(8, VigilRules.clampMultiplier(8));
	}

	@Test
	void exhaustionScalesLinearlyWithMultiplier() {
		float one = VigilRules.exhaustionPerTick(1);
		assertEquals(VigilRules.EXHAUSTION_BASE_PER_TICK, one, 1e-9);
		assertEquals(8 * one, VigilRules.exhaustionPerTick(8), 1e-6); // 8x time = 8x burn
		assertEquals(16 * one, VigilRules.exhaustionPerTick(999), 1e-6); // capped
	}

	@Test
	void extraTickRateIsMultiplierMinusOneTimesVanilla() {
		double vanillaRate = 3.0 / VigilRules.SECTION_VOLUME;
		assertEquals(7 * vanillaRate, VigilRules.extraTickRate(8, 3), 1e-12);
		assertEquals(0.0, VigilRules.extraTickRate(1, 3), 1e-12); // 1x = vanilla, zero extra
		assertEquals(15 * vanillaRate, VigilRules.extraTickRate(16, 3), 1e-12);
	}

	@Test
	void cubeVolume() {
		assertEquals(17 * 17 * 17, VigilRules.cubeVolume(8));
		assertEquals(27, VigilRules.cubeVolume(1));
	}

	// MULTIPLIER HONESTY: expected extra ticks per block per tick, measured over
	// many simulated ticks of attempt-sampling, must converge to (mult-1) * speed / 4096
	@Test
	void multiplierHonestyMonteCarlo() {
		Random rng = new Random(42);
		int multiplier = 8;
		int speed = 3;
		int volume = VigilRules.cubeVolume(8);
		int ticks = 200_000;
		long totalAttempts = 0;
		for (int i = 0; i < ticks; i++) {
			totalAttempts += VigilRules.extraTickAttempts(multiplier, speed, volume, rng.nextDouble());
		}
		double perBlockPerTick = (double) totalAttempts / ticks / volume;
		double expected = VigilRules.extraTickRate(multiplier, speed);
		assertEquals(expected, perBlockPerTick, expected * 0.02); // within 2%
	}

	@Test
	void attemptsWholePlusFractional() {
		// mean = volume * rate; whole part always granted, fraction is a bernoulli roll
		int volume = VigilRules.cubeVolume(8); // 4913
		double mean = volume * VigilRules.extraTickRate(8, 3); // ~25.18
		int whole = (int) mean;
		assertEquals(whole, VigilRules.extraTickAttempts(8, 3, volume, 0.999999));
		assertEquals(whole + 1, VigilRules.extraTickAttempts(8, 3, volume, 0.0));
	}

	@Test
	void multiplierOneMeansNoExtraTicksEver() {
		assertEquals(0, VigilRules.extraTickAttempts(1, 3, 4913, 0.0));
	}
}
