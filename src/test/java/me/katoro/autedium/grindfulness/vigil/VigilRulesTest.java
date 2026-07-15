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
		// sneak no longer part of the deal (rework 2) — movement is the input breaker
		assertFalse(VigilRules.shouldBreak(true, 14, 14, false, false));
		assertTrue(VigilRules.shouldBreak(false, 20, 14, false, false)); // night falls
		assertTrue(VigilRules.shouldBreak(true, 13, 14, false, false));  // hunger drops below floor
		assertTrue(VigilRules.shouldBreak(true, 20, 14, true, false));   // moved
		assertTrue(VigilRules.shouldBreak(true, 20, 14, false, true));   // damaged
	}

	@Test
	void remainingDaytime() {
		assertEquals(12000, VigilRules.remainingDaytime(0));
		assertEquals(1, VigilRules.remainingDaytime(11999));
		assertEquals(0, VigilRules.remainingDaytime(12000)); // night
		assertEquals(0, VigilRules.remainingDaytime(18000));
		assertEquals(12000, VigilRules.remainingDaytime(24000)); // wraps to dawn
		assertEquals(6000, VigilRules.remainingDaytime(24000 + 6000));
	}

	@Test
	void maxWaitHoursCapsAtRemainingDaytimeInHalfSteps() {
		assertEquals(8.0, VigilRules.maxWaitHours(12000)); // full day, slider ceiling wins
		assertEquals(8.0, VigilRules.maxWaitHours(8000));
		assertEquals(3.5, VigilRules.maxWaitHours(3700)); // snapped DOWN to 0.5 step
		assertEquals(3.5, VigilRules.maxWaitHours(3999));
		assertEquals(1.0, VigilRules.maxWaitHours(1000));
		assertEquals(0.5, VigilRules.maxWaitHours(999)); // below MIN — caller refuses
		assertEquals(0.0, VigilRules.maxWaitHours(0));
	}

	@Test
	void clampWaitHoursSanitizesClientRequests() {
		assertEquals(4.0, VigilRules.clampWaitHours(4.0, 12000));
		assertEquals(4.0, VigilRules.clampWaitHours(3.99, 12000)); // snapped to 0.5 grid
		assertEquals(8.0, VigilRules.clampWaitHours(999.0, 12000)); // slider ceiling
		assertEquals(1.0, VigilRules.clampWaitHours(0.0, 12000));  // floor at MIN
		assertEquals(3.5, VigilRules.clampWaitHours(8.0, 3700));   // remaining-daytime cap
		assertEquals(0.0, VigilRules.clampWaitHours(2.0, 999));    // dusk too close: refused
	}

	@Test
	void durationTargetMath() {
		assertEquals(8000, VigilRules.targetTicks(8.0));
		assertEquals(2500, VigilRules.targetTicks(2.5));
		assertEquals(7, VigilRules.deliveredPerTick(8)); // multiplier - 1 extras per tick
		assertEquals(0, VigilRules.deliveredPerTick(1));
		assertEquals(15, VigilRules.deliveredPerTick(999)); // capped at 16

		// 8000 target at 7/tick = ceil(8000/7) = 1143 real ticks
		assertEquals(1143, VigilRules.channelDurationTicks(8000, 8));
		assertEquals(1000, VigilRules.channelDurationTicks(1000, 2)); // 1/tick
		assertEquals(Long.MAX_VALUE, VigilRules.channelDurationTicks(1000, 1)); // never completes
	}

	@Test
	void liveEstimatesMatchDurationMath() {
		// 8h at 8x: 1143 ticks -> 57.15 real seconds
		assertEquals(1143 / 20.0, VigilRules.estimatedRealSeconds(8.0, 8), 1e-9);
		// exhaustion = perTick * duration; food points = exhaustion / 4
		float exh = VigilRules.estimatedExhaustion(8.0, 8);
		assertEquals(VigilRules.exhaustionPerTick(8) * 1143, exh, 1e-4);
		assertEquals(exh / 4.0, VigilRules.estimatedFoodCost(8.0, 8), 1e-4);
		assertTrue(Double.isInfinite(VigilRules.estimatedRealSeconds(8.0, 1)));
	}

	// DELIVERED-TARGET HONESTY: summing deliveredPerTick over channelDurationTicks
	// must reach the target, and not a full tick's worth early
	@Test
	void deliveredReachesTargetExactlyAtDuration() {
		for (int mult : new int[]{2, 3, 8, 16}) {
			long target = VigilRules.targetTicks(3.5);
			long dur = VigilRules.channelDurationTicks(target, mult);
			long per = VigilRules.deliveredPerTick(mult);
			assertTrue(dur * per >= target);
			assertTrue((dur - 1) * per < target);
		}
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
