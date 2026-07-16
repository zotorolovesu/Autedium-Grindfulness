package me.katoro.autedium.grindfulness.client.pinbook;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.pinbook.PinMath;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// /pin add|del|list|track|untrack — client-only waypoints. info ONLY, no teleports ever.
public final class PinBookClient {
	private static final int HUD_INTERVAL = 40; // ticks between action-bar refreshes

	private static PinStore store;
	private static String tracked; // pin name currently tracked, null = none (in-memory, per session)
	private static int tickCounter;

	private PinBookClient() {}

	public static void init() {
		store = new PinStore(FabricLoader.getInstance().getConfigDir().resolve("autedium_grindfulness_pins.json"));

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> tracked = null);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
			dispatcher.register(ClientCommands.literal("pin")
				.then(ClientCommands.literal("add")
					.then(ClientCommands.argument("name", StringArgumentType.string())
						.executes(ctx -> add(ctx))))
				.then(ClientCommands.literal("del")
					.then(ClientCommands.argument("name", StringArgumentType.string())
						.executes(ctx -> del(ctx))))
				.then(ClientCommands.literal("list").executes(ctx -> list(ctx)))
				.then(ClientCommands.literal("track")
					.then(ClientCommands.argument("name", StringArgumentType.string())
						.executes(ctx -> track(ctx))))
				.then(ClientCommands.literal("untrack").executes(ctx -> untrack(ctx)))));

		ClientTickEvents.END_CLIENT_TICK.register(PinBookClient::tick);
	}

	private static boolean enabled() {
		return GrindConfig.get().isEnabled("pin_book");
	}

	// per world/server key so pins dont bleed between saves: server address, or level name for singleplayer
	private static String worldKey(Minecraft client) {
		if (client.getCurrentServer() != null) return "server:" + client.getCurrentServer().ip;
		if (client.hasSingleplayerServer()) return "sp:" + client.getSingleplayerServer().getWorldData().getLevelName();
		return "unknown";
	}

	private static int add(CommandContext<FabricClientCommandSource> ctx) {
		if (!enabled()) return 0;
		var src = ctx.getSource();
		var player = src.getPlayer();
		String name = StringArgumentType.getString(ctx, "name");
		var pin = new PinStore.Pin(player.blockPosition().getX(), player.blockPosition().getY(),
			player.blockPosition().getZ(), src.getLevel().dimension().identifier().toString());
		store.put(worldKey(src.getClient()), name, pin);
		src.sendFeedback(Component.translatable("autedium_grindfulness.pin.added", name, pin.x(), pin.y(), pin.z())
			.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int del(CommandContext<FabricClientCommandSource> ctx) {
		if (!enabled()) return 0;
		var src = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "name");
		if (!store.remove(worldKey(src.getClient()), name)) {
			src.sendError(Component.translatable("autedium_grindfulness.pin.missing", name));
			return 0;
		}
		if (name.equals(tracked)) tracked = null;
		src.sendFeedback(Component.translatable("autedium_grindfulness.pin.removed", name).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int list(CommandContext<FabricClientCommandSource> ctx) {
		if (!enabled()) return 0;
		var src = ctx.getSource();
		var pins = store.forWorld(worldKey(src.getClient()));
		if (pins.isEmpty()) {
			src.sendFeedback(Component.translatable("autedium_grindfulness.pin.empty").withStyle(ChatFormatting.GRAY));
			return 1;
		}
		src.sendFeedback(Component.translatable("autedium_grindfulness.pin.list", pins.size()).withStyle(ChatFormatting.AQUA));
		pins.forEach((name, pin) -> src.sendFeedback(Component.literal("  ◈ ")
			.withStyle(ChatFormatting.DARK_AQUA)
			.append(Component.literal(name + (name.equals(tracked) ? " ★" : "")).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("  " + pin.x() + ", " + pin.y() + ", " + pin.z() + "  " + pin.dim())
				.withStyle(ChatFormatting.GRAY))));
		return 1;
	}

	private static int track(CommandContext<FabricClientCommandSource> ctx) {
		if (!enabled()) return 0;
		var src = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "name");
		if (!store.forWorld(worldKey(src.getClient())).containsKey(name)) {
			src.sendError(Component.translatable("autedium_grindfulness.pin.missing", name));
			return 0;
		}
		tracked = name;
		src.sendFeedback(Component.translatable("autedium_grindfulness.pin.tracking", name).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int untrack(CommandContext<FabricClientCommandSource> ctx) {
		if (!enabled()) return 0;
		tracked = null;
		ctx.getSource().sendFeedback(Component.translatable("autedium_grindfulness.pin.untracked").withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static void tick(Minecraft client) {
		if (tracked == null || client.player == null || client.level == null) return;
		if (++tickCounter < HUD_INTERVAL) return;
		tickCounter = 0;
		if (!enabled()) return;

		var pin = store.forWorld(worldKey(client)).get(tracked);
		if (pin == null) { // deleted out from under us
			tracked = null;
			return;
		}
		var player = client.player;
		if (!client.level.dimension().identifier().toString().equals(pin.dim())) {
			player.sendOverlayMessage(Component.translatable("autedium_grindfulness.pin.bar_other_dim", tracked)
				.withStyle(ChatFormatting.GRAY));
			return;
		}
		double dx = pin.x() + 0.5 - player.getX();
		double dy = pin.y() + 0.5 - player.getY();
		double dz = pin.z() + 0.5 - player.getZ();
		String arrow = PinMath.arrow(player.getYRot(), dx, dz);
		int dist = PinMath.distanceBlocks(dx, dy, dz);
		player.sendOverlayMessage(Component.translatable("autedium_grindfulness.pin.bar", arrow, tracked, dist)
			.withStyle(ChatFormatting.AQUA));
	}
}
