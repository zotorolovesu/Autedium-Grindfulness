package me.katoro.autedium.grindfulness.net;

import me.katoro.autedium.grindfulness.GrindfulnessMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PityHintPayload(int count, int threshold) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PityHintPayload> TYPE =
		new CustomPacketPayload.Type<>(GrindfulnessMod.id("pity_hint"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PityHintPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, PityHintPayload::count,
		ByteBufCodecs.VAR_INT, PityHintPayload::threshold,
		PityHintPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
