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

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			java.util.Map<String, Boolean> state = new java.util.HashMap<>();
			me.katoro.autedium.grindfulness.core.ModuleRegistry.all()
				.forEach(m -> state.put(m.id(), m.enabled()));
			int score = me.katoro.autedium.grindfulness.core.FairnessMeter.score(state);
			// chat instead of a popup, popups r annoying. player can be null this early so defer a tick
			client.execute(() -> {
				if (client.player != null) {
					client.player.sendSystemMessage(
						net.minecraft.network.chat.Component.translatable("autedium_grindfulness.chat.join")
							.append(ConfigScreenFactory.verdictText(score)));
				}
			});
		});
	}
}
