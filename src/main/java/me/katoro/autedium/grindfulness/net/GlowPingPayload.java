package me.katoro.autedium.grindfulness.net;

import me.katoro.autedium.grindfulness.GrindfulnessMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record GlowPingPayload(List<BlockPos> positions, int argb, int durationTicks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GlowPingPayload> TYPE =
		new CustomPacketPayload.Type<>(GrindfulnessMod.id("glow_ping"));

	public static final StreamCodec<RegistryFriendlyByteBuf, GlowPingPayload> CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), GlowPingPayload::positions,
		ByteBufCodecs.INT, GlowPingPayload::argb,
		ByteBufCodecs.VAR_INT, GlowPingPayload::durationTicks,
		GlowPingPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
