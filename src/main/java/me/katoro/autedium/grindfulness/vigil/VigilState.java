package me.katoro.autedium.grindfulness.vigil;

import net.minecraft.world.phys.Vec3;

// per-player channel state. in-memory only, server-side, cleared on disconnect.
final class VigilState {
	final Vec3 anchor;      // where the player knelt — any drift breaks the channel
	final long startTick;   // for the action-bar elapsed timer

	VigilState(Vec3 anchor, long startTick) {
		this.anchor = anchor;
		this.startTick = startTick;
	}
}
