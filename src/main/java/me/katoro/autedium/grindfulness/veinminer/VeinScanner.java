package me.katoro.autedium.grindfulness.veinminer;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class VeinScanner {
	// hard ceiling no matter what the config says. someone WILL put 99999 in there
	public static final int HARD_CAP = 128;

	private VeinScanner() {}

	// BFS flood fill, 26 neighbors so diagonals count. origin not included in the result
	public static List<BlockPos> scan(BlockPos origin, Predicate<BlockPos> matches, int cap) {
		int limit = Math.min(cap, HARD_CAP);
		List<BlockPos> found = new ArrayList<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);

		while (!queue.isEmpty() && found.size() < limit) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1 && found.size() < limit; dx++) {
				for (int dy = -1; dy <= 1 && found.size() < limit; dy++) {
					for (int dz = -1; dz <= 1 && found.size() < limit; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;
						BlockPos next = current.offset(dx, dy, dz);
						if (seen.add(next) && matches.test(next)) {
							found.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
		return found;
	}
}
