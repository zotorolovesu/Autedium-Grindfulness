package me.katoro.autedium.grindfulness.vigil;

import net.minecraft.world.phys.Vec3;

// per-player channel state. in-memory only, server-side, cleared on disconnect.
final class VigilState {
	final Vec3 anchor;       // where the player knelt — any drift breaks the channel
	final long targetTicks;  // hours x 1000 game-time-equivalents to deliver (rework 2)
	long delivered;          // (multiplier - 1) tick-equivalents accumulated so far

	VigilState(Vec3 anchor, long targetTicks) {
		this.anchor = anchor;
		this.targetTicks = targetTicks;
	}
}
