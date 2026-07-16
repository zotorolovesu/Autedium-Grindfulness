package me.katoro.autedium.grindfulness.core;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

// per-player in-memory state, evicted automatically on disconnect — the
// boilerplate four modules were each hand-rolling (and flow-haste forgot,
// hence the old memory-leak watch-list entry). only ever touched from the
// server thread, so a plain HashMap.
public final class PerPlayer<T> {
	private final Map<UUID, T> map = new HashMap<>();

	public PerPlayer() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> map.remove(handler.getPlayer().getUUID()));
	}

	public T get(UUID id) {
		return map.get(id);
	}

	public T getOrDefault(UUID id, T fallback) {
		return map.getOrDefault(id, fallback);
	}

	public void put(UUID id, T value) {
		map.put(id, value);
	}

	public T computeIfAbsent(UUID id, Function<UUID, T> compute) {
		return map.computeIfAbsent(id, compute);
	}

	public T remove(UUID id) {
		return map.remove(id);
	}

	public boolean contains(UUID id) {
		return map.containsKey(id);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Iterable<Map.Entry<UUID, T>> entries() {
		return map.entrySet();
	}

	public void clear() {
		map.clear();
	}
}
