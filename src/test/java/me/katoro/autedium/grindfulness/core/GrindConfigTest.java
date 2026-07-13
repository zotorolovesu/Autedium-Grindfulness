package me.katoro.autedium.grindfulness.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GrindConfigTest {
	@Test
	void roundTrip(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("cfg.json");
		GrindConfig.load(file);
		assertTrue(GrindConfig.get().isEnabled("vein_miner")); // default on
		GrindConfig.get().setEnabled("vein_miner", false);
		GrindConfig.save();

		GrindConfig.load(file); // reload from disk
		assertFalse(GrindConfig.get().isEnabled("vein_miner"));
		assertEquals(800, GrindConfig.get().pityThreshold);
	}

	@Test
	void malformedFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("cfg.json");
		Files.writeString(file, "{not json!!!");
		GrindConfig.load(file);
		assertTrue(GrindConfig.get().isEnabled("prospector"));
		assertEquals(12, GrindConfig.get().veinCapOres);

		String rewritten = Files.readString(file);
		assertTrue(rewritten.contains("\"veinCapOres\": 12"));
	}

	@Test
	void unknownIdDefaultsTrue(@TempDir Path dir) throws Exception {
		GrindConfig.load(dir.resolve("cfg.json"));
		assertTrue(GrindConfig.get().isEnabled("some_future_module"));
	}
}
