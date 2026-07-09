package me.katoro.autedium.grindfulness.veinminer;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VeinScannerTest {
	@Test
	void findsDiagonallyConnectedVein() {
		Set<BlockPos> vein = Set.of(
			new BlockPos(0, 0, 0),
			new BlockPos(1, 1, 1),   // diagonal neighbor
			new BlockPos(1, 1, 2),
			new BlockPos(9, 9, 9)    // disconnected — must NOT be found
		);
		List<BlockPos> found = VeinScanner.scan(new BlockPos(0, 0, 0), vein::contains, 12);
		assertEquals(2, found.size());          // origin itself excluded
		assertFalse(found.contains(new BlockPos(9, 9, 9)));
		assertFalse(found.contains(new BlockPos(0, 0, 0)));
	}

	@Test
	void respectsCap() {
		// 5x5x5 solid cube around origin = 124 matching neighbors
		List<BlockPos> found = VeinScanner.scan(BlockPos.ZERO,
			p -> Math.abs(p.getX()) <= 2 && Math.abs(p.getY()) <= 2 && Math.abs(p.getZ()) <= 2, 12);
		assertEquals(12, found.size());
	}

	@Test
	void hardCapDefendsAgainstSillyConfig() {
		List<BlockPos> found = VeinScanner.scan(BlockPos.ZERO, p -> true, 100000);
		assertEquals(128, found.size());
	}
}
