package me.katoro.autedium.grindfulness.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GrindConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static GrindConfig instance = new GrindConfig();
	private static Path file;

	public Map<String, Boolean> enabled = new ConcurrentHashMap<>();
	public int veinCapOres = 12;
	public int veinCapLogs = 64;
	public int flowResetTicks = 120;
	public int pityThreshold = 800;
	public int pityRecheck = 100;
	public int pityScanRadius = 32;
	public int prospectRadius = 12;
	public int prospectCooldownTicks = 900;
	public int ledgerMilestone = 500;
	public int ledgerIdleSeconds = 90;
	public int vigilMultiplier = 8;   // capped at 16 in VigilRules
	public int vigilRadius = 8;       // capped at 16 in VigilRules
	public int vigilHungerFloor = 14; // sprint threshold — food is the fuel meter

	public static GrindConfig get() {
		return instance;
	}

	public static void load(Path path) {
		file = path;
		GrindConfig loaded = null;
		if (Files.exists(path)) {
			try {
				loaded = GSON.fromJson(Files.readString(path), GrindConfig.class);
			} catch (IOException | com.google.gson.JsonSyntaxException e) {
				System.err.println("[AuTedium-Grindfulness] bad config, using defaults: " + e);
			}
		}
		instance = loaded != null ? loaded : new GrindConfig();
		instance.enabled = instance.enabled == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(instance.enabled);
		save();
	}

	public static void save() {
		if (file == null) return;
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[AuTedium-Grindfulness] could not save config: " + e);
		}
	}

	public boolean isEnabled(String id) {
		return enabled.getOrDefault(id, true);
	}

	public void setEnabled(String id, boolean value) {
		enabled.put(id, value);
	}
}
