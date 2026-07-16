package me.katoro.autedium.grindfulness.torchcadence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorchCadenceRulesTest {
	@Test
	void checksEveryTenTicks() {
		assertTrue(TorchCadenceRules.shouldCheck(0));
		assertTrue(TorchCadenceRules.shouldCheck(10));
		assertTrue(TorchCadenceRules.shouldCheck(123450));
		assertFalse(TorchCadenceRules.shouldCheck(1));
		assertFalse(TorchCadenceRules.shouldCheck(9));
		assertFalse(TorchCadenceRules.shouldCheck(11));
	}

	@Test
	void thresholdIsStrictLessThan() {
		assertTrue(TorchCadenceRules.lightTooLow(4, 5));
		assertFalse(TorchCadenceRules.lightTooLow(5, 5));  // at threshold = bright enough
		assertFalse(TorchCadenceRules.lightTooLow(14, 5));
		assertTrue(TorchCadenceRules.lightTooLow(0, 1));
		assertFalse(TorchCadenceRules.lightTooLow(0, 0)); // threshold 0 = module effectively off
	}

	@Test
	void clampedThresholdDrivesTheDecision() {
		// a json-edited threshold of 999 behaves like 14, not "always place"
		assertTrue(TorchCadenceRules.lightTooLow(13, 999));
		assertFalse(TorchCadenceRules.lightTooLow(14, 999));
		assertFalse(TorchCadenceRules.lightTooLow(0, -3)); // negative clamps to 0 = off
	}

	@Test
	void placeOnlyWhenDarkAndNotClustered() {
		assertTrue(TorchCadenceRules.shouldPlace(3, 5, false));
		assertFalse(TorchCadenceRules.shouldPlace(3, 5, true));   // dark but torch nearby
		assertFalse(TorchCadenceRules.shouldPlace(7, 5, false));  // bright enough
		assertFalse(TorchCadenceRules.shouldPlace(7, 5, true));
	}
}
