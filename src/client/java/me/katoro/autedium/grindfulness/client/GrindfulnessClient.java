package me.katoro.autedium.grindfulness.client;

import me.katoro.autedium.grindfulness.client.render.GlowRenderer;
import me.katoro.autedium.grindfulness.net.GlowPingPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GrindfulnessClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GlowRenderer.init();

		ClientPlayNetworking.registerGlobalReceiver(GlowPingPayload.TYPE, (payload, context) ->
			GlowRenderer.add(payload.positions(), payload.argb(), payload.durationTicks()));
	}
}
