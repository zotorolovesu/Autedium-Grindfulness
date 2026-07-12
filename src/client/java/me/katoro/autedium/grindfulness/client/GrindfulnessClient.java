package me.katoro.autedium.grindfulness.client;

import me.katoro.autedium.grindfulness.client.render.GlowRenderer;
import me.katoro.autedium.grindfulness.client.toasts.ToastsClient;
import me.katoro.autedium.grindfulness.net.GlowPingPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GrindfulnessClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GlowRenderer.init();
		ToastsClient.init();

		ClientPlayNetworking.registerGlobalReceiver(GlowPingPayload.TYPE, (payload, context) ->
			GlowRenderer.add(payload.positions(), payload.argb(), payload.durationTicks()));

		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("grindfulness")
				.executes(ctx -> {
					var client = ctx.getSource().getClient();
					// cant swap screens while chat is closing, gotta defer
					client.execute(() -> client.setScreenAndShow(ConfigScreenFactory.create(null)));
					return 1;
				})));
	}
}
