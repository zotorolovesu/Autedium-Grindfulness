package me.katoro.autedium.grindfulness.pinbook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// pure yaw→8-way bearing math, no MC imports
class PinMathTest {
	// mc yaw 0 faces +Z (south). pin straight ahead = ↑
	@Test
	void deadAheadIsUp() {
		assertEquals("↑", PinMath.arrow(0, 0, 10));
	}

	@Test
	void behindIsDown() {
		assertEquals("↓", PinMath.arrow(0, 0, -10));
	}

	// facing south (yaw 0), +X is east which is to the player's LEFT
	@Test
	void eastWhileFacingSouthIsLeft() {
		assertEquals("←", PinMath.arrow(0, 10, 0));
	}

	@Test
	void westWhileFacingSouthIsRight() {
		assertEquals("→", PinMath.arrow(0, -10, 0));
	}

	// yaw 90 = west (-X); pin at -X is now dead ahead
	@Test
	void yawRotationFollowsTarget() {
		assertEquals("↑", PinMath.arrow(90, -10, 0));
		assertEquals("↓", PinMath.arrow(90, 10, 0));
	}

	@Test
	void diagonalsHitOrdinalArrows() {
		assertEquals("↗", PinMath.arrow(0, -10, 10)); // 45° clockwise of facing
		assertEquals("↖", PinMath.arrow(0, 10, 10));  // 45° counter-clockwise
	}

	// octant boundaries round to nearest arrow, ±22.5° stays consistent
	@Test
	void boundaryRoundsSanely() {
		assertEquals(0, PinMath.octant(20, 0, 10));  // 20° off → still "ahead"
		assertEquals(7, PinMath.octant(30, 0, 10));  // 30° off → next octant (counter-clockwise)
	}

	// yaw wraps: mc yaw can be any accumulated value, huge spins must not break it
	@Test
	void wrapsCrazyYaw() {
		assertEquals("↑", PinMath.arrow(720, 0, 10));
		assertEquals("↑", PinMath.arrow(-1080, 0, 10));
	}

	@Test
	void wrapDegreesRange() {
		assertEquals(-180.0, PinMath.wrapDegrees(180));
		assertEquals(0.0, PinMath.wrapDegrees(360));
		assertEquals(-170.0, PinMath.wrapDegrees(190));
		assertEquals(170.0, PinMath.wrapDegrees(-190));
	}

	@Test
	void distanceRounds() {
		assertEquals(5, PinMath.distanceBlocks(3, 0, 4));
		assertEquals(1, PinMath.distanceBlocks(1, 0.2, 0));
		assertEquals(0, PinMath.distanceBlocks(0, 0, 0));
	}
}
