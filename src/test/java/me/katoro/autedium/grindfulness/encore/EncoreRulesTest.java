package me.katoro.autedium.grindfulness.encore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncoreRulesTest {
	@Test
	void onlyAdultsOnCooldownQualify() {
		assertTrue(EncoreRules.canBuyout(6000, true, 2));  // mid-cooldown adult
		assertTrue(EncoreRules.canBuyout(1, true, 2));     // last tick of cooldown
		assertFalse(EncoreRules.canBuyout(0, true, 2));    // ready adult — vanilla's job
		assertFalse(EncoreRules.canBuyout(-24000, true, 2)); // baby — never
	}

	@Test
	void needsTheRightFoodAndDoubleCount() {
		assertFalse(EncoreRules.canBuyout(6000, false, 64)); // wrong item
		assertFalse(EncoreRules.canBuyout(6000, true, 1));   // only one — vanilla feed, no buyout
		assertTrue(EncoreRules.canBuyout(6000, true, 2));    // exactly the price
		assertTrue(EncoreRules.canBuyout(6000, true, 64));   // full stack fine, still consumes 2
	}
}
