package me.katoro.autedium.grindfulness.client.pinbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

// client-side pin persistence: one json file in the fabric config dir,
// keyed per world/server so ur base pins dont bleed into other saves
public final class PinStore {
	public record Pin(int x, int y, int z, String dim) {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type SHAPE = new TypeToken<Map<String, Map<String, Pin>>>() {}.getType();

	private final Path file;
	// worldKey -> pinName -> pin
	private Map<String, Map<String, Pin>> pins = new LinkedHashMap<>();

	public PinStore(Path file) {
		this.file = file;
		if (Files.exists(file)) {
			try {
				Map<String, Map<String, Pin>> loaded = GSON.fromJson(Files.readString(file), SHAPE);
				if (loaded != null) pins = loaded;
			} catch (IOException | com.google.gson.JsonSyntaxException e) {
				System.err.println("[AuTedium-Grindfulness] bad pin file, starting fresh: " + e);
			}
		}
	}

	public Map<String, Pin> forWorld(String worldKey) {
		return pins.computeIfAbsent(worldKey, k -> new LinkedHashMap<>());
	}

	public void put(String worldKey, String name, Pin pin) {
		forWorld(worldKey).put(name, pin);
		save();
	}

	public boolean remove(String worldKey, String name) {
		boolean removed = forWorld(worldKey).remove(name) != null;
		if (removed) save();
		return removed;
	}

	private void save() {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(pins, SHAPE));
		} catch (IOException e) {
			System.err.println("[AuTedium-Grindfulness] could not save pins: " + e);
		}
	}
}
