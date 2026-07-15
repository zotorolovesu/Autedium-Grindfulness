package me.katoro.autedium.grindfulness.net;

import me.katoro.autedium.grindfulness.GrindfulnessMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// first serverbound payload in the mod: the skyrim-wait request. hours travel
// as half-hour steps (2..16) so the wire stays an int; server clamps anyway.
public record VigilWaitPayload(int halfHours) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<VigilWaitPayload> TYPE =
		new CustomPacketPayload.Type<>(GrindfulnessMod.id("vigil_wait"));

	public static final StreamCodec<RegistryFriendlyByteBuf, VigilWaitPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, VigilWaitPayload::halfHours,
		VigilWaitPayload::new
	);

	public double hours() {
		return halfHours / 2.0;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
