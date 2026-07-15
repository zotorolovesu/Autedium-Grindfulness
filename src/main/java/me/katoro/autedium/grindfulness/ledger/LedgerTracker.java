package me.katoro.autedium.grindfulness.ledger;

import java.util.HashMap;
import java.util.Map;

// pure session-stats brain, no MC imports so it gets real unit tests
public final class LedgerTracker {
	public enum BreakResult { MILESTONE, NONE }

	// grindTicks = active grind time of the burst (last break - burst start)
	public record IdleSummary(Map<String, Integer> countsByFamily, int total, int burstCount, int bestStreak, long grindTicks) {}

	private final Map<String, Integer> counts = new HashMap<>();
	private int total;
	private long lastBreak = Long.MIN_VALUE;
	private long burstStart;
	private int burstCount;
	private int bestStreak;
	private boolean summaryFired;

	// now is whatever monotonic clock the caller uses (ticks); thresholds injected so config stays live
	public BreakResult onBreak(String family, long now, int milestone, long idleWindow) {
		if (burstCount == 0 || summaryFired || now - lastBreak > idleWindow) {
			burstStart = now;
			burstCount = 0;
			summaryFired = false;
		}
		burstCount++;
		lastBreak = now;
		counts.merge(family, 1, Integer::sum);
		total++;
		return (milestone > 0 && total % milestone == 0) ? BreakResult.MILESTONE : BreakResult.NONE;
	}

	// snapshot of the flow module's streak, taken at break time
	public void recordStreak(int streak) {
		if (streak > bestStreak) bestStreak = streak;
	}

	// null unless: burst >= floor, idle window elapsed, not already fired for this burst
	public IdleSummary pollIdle(long now, long idleWindow, int burstFloor) {
		if (summaryFired || burstCount < burstFloor) return null;
		if (lastBreak == Long.MIN_VALUE || now - lastBreak <= idleWindow) return null;
		summaryFired = true;
		return new IdleSummary(Map.copyOf(counts), total, burstCount, bestStreak, lastBreak - burstStart);
	}

	public int total() {
		return total;
	}

	public int count(String family) {
		return counts.getOrDefault(family, 0);
	}

	public int bestStreak() {
		return bestStreak;
	}
}
