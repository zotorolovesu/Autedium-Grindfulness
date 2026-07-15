package me.katoro.autedium.grindfulness.client;

import me.katoro.autedium.grindfulness.client.render.GlowRenderer;
import me.katoro.autedium.grindfulness.client.toasts.ToastsClient;
import me.katoro.autedium.grindfulness.net.GlowPingPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GrindfulnessClient implements ClientModInitializer {
	// per-char color lerp. is this necessary? no. does it look sick? yes
	private static net.minecraft.network.chat.Component gradient(String text, int from, int to, boolean bold) {
		var out = net.minecraft.network.chat.Component.empty();
		int n = Math.max(1, text.length() - 1);
		for (int i = 0; i < text.length(); i++) {
			float t = (float) i / n;
			int r = (int) (((from >> 16 & 0xFF) * (1 - t)) + ((to >> 16 & 0xFF) * t));
			int g = (int) (((from >> 8 & 0xFF) * (1 - t)) + ((to >> 8 & 0xFF) * t));
			int b = (int) (((from & 0xFF) * (1 - t)) + ((to & 0xFF) * t));
			final int rgb = r << 16 | g << 8 | b;
			out.append(net.minecraft.network.chat.Component.literal(String.valueOf(text.charAt(i)))
				.withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)).withBold(bold)));
		}
		return out;
	}

	@Override
	public void onInitializeClient() {
		GlowRenderer.init();
		ToastsClient.init();
		me.katoro.autedium.grindfulness.client.vigil.VigilClient.init();

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
				if (client.player == null) return;
				var p = client.player;
				p.sendSystemMessage(gradient("━━━━━━ ⛏ AuTedium – Grindfulness ⛏ ━━━━━━", 0x55FFFF, 0xFF55FF, true));
				p.sendSystemMessage(net.minecraft.network.chat.Component.literal("  ")
					.append(net.minecraft.network.chat.Component.translatable("autedium_grindfulness.chat.join")
						.withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)));
				p.sendSystemMessage(net.minecraft.network.chat.Component.literal("  ")
					.append(ConfigScreenFactory.verdictText(score)));
				p.sendSystemMessage(gradient("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", 0xFF55FF, 0x55FFFF, false));
			});
		});
	}
}
