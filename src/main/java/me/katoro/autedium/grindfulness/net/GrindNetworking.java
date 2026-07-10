package me.katoro.autedium.grindfulness.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class GrindNetworking {
	private GrindNetworking() {}

	public static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(GlowPingPayload.TYPE, GlowPingPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PityHintPayload.TYPE, PityHintPayload.CODEC);
	}

	public static void sendGlow(ServerPlayer player, List<BlockPos> positions, int argb, int durationTicks) {
		if (positions.isEmpty()) return;
		ServerPlayNetworking.send(player, new GlowPingPayload(positions, argb, durationTicks));
	}

	public static void sendPityHint(ServerPlayer player, int count, int threshold) {
		ServerPlayNetworking.send(player, new PityHintPayload(count, threshold));
	}
}
