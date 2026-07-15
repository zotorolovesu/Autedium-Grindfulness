package me.katoro.autedium.grindfulness.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LedgerTrackerTest {
	private static final int MILESTONE = 500;
	private static final long IDLE = 1800; // 90s in ticks
	private static final int FLOOR = 50;

	// returns the timestamp of the LAST break
	private static long breakN(LedgerTracker t, int n, long start) {
		long now = start;
		for (int i = 0; i < n; i++) {
			t.onBreak("STONE", now, MILESTONE, IDLE);
			if (i < n - 1) now += 10;
		}
		return now;
	}

	@Test
	void milestoneFiresExactlyOnBoundary() {
		LedgerTracker t = new LedgerTracker();
		long now = 0;
		for (int i = 1; i <= 499; i++) {
			assertEquals(LedgerTracker.BreakResult.NONE, t.onBreak("STONE", now, MILESTONE, IDLE));
			now += 10;
		}
		assertEquals(LedgerTracker.BreakResult.MILESTONE, t.onBreak("STONE", now, MILESTONE, IDLE));
		now += 10;
		for (int i = 501; i <= 999; i++) {
			assertEquals(LedgerTracker.BreakResult.NONE, t.onBreak("LOG", now, MILESTONE, IDLE));
			now += 10;
		}
		assertEquals(LedgerTracker.BreakResult.MILESTONE, t.onBreak("LOG", now, MILESTONE, IDLE));
		assertEquals(1000, t.total());
		assertEquals(500, t.count("STONE"));
		assertEquals(500, t.count("LOG"));
	}

	@Test
	void idleFiresOncePerBurst() {
		LedgerTracker t = new LedgerTracker();
		long end = breakN(t, 60, 0);
		assertNull(t.pollIdle(end + IDLE - 1, IDLE, FLOOR)); // window not elapsed yet
		LedgerTracker.IdleSummary s = t.pollIdle(end + IDLE + 100, IDLE, FLOOR);
		assertNotNull(s);
		assertEquals(60, s.burstCount());
		assertEquals(60, s.countsByFamily().get("STONE"));
		assertNull(t.pollIdle(end + IDLE + 200, IDLE, FLOOR)); // already fired
	}

	@Test
	void idleNeedsBurstFloor() {
		LedgerTracker t = new LedgerTracker();
		long end = breakN(t, 49, 0);
		assertNull(t.pollIdle(end + IDLE * 2, IDLE, FLOOR));
	}

	@Test
	void newBreakStartsFreshBurstAfterSummary() {
		LedgerTracker t = new LedgerTracker();
		long end = breakN(t, 60, 0);
		assertNotNull(t.pollIdle(end + IDLE + 1, IDLE, FLOOR));
		// next break resets the fired flag and starts a new burst
		long start2 = end + IDLE + 500;
		long end2 = breakN(t, 60, start2);
		LedgerTracker.IdleSummary s = t.pollIdle(end2 + IDLE + 1, IDLE, FLOOR);
		assertNotNull(s);
		assertEquals(60, s.burstCount());
		assertEquals(120, s.total()); // session totals keep accumulating
	}

	@Test
	void idleGapWithoutSummarySplitsBurst() {
		LedgerTracker t = new LedgerTracker();
		long end = breakN(t, 30, 0); // under floor, no summary possible
		long start2 = end + IDLE + 500;
		long end2 = breakN(t, 55, start2);
		LedgerTracker.IdleSummary s = t.pollIdle(end2 + IDLE + 1, IDLE, FLOOR);
		assertNotNull(s);
		assertEquals(55, s.burstCount()); // old 30 not part of this burst
	}

	@Test
	void bestStreakIsMaxSnapshot() {
		LedgerTracker t = new LedgerTracker();
		t.recordStreak(12);
		t.recordStreak(38);
		t.recordStreak(7);
		assertEquals(38, t.bestStreak());
	}

	@Test
	void grindTicksSpansBurst() {
		LedgerTracker t = new LedgerTracker();
		long end = breakN(t, 60, 1000); // last break at 1000 + 59*10
		LedgerTracker.IdleSummary s = t.pollIdle(end + IDLE + 1, IDLE, FLOOR);
		assertNotNull(s);
		assertEquals(590, s.grindTicks());
	}
}
