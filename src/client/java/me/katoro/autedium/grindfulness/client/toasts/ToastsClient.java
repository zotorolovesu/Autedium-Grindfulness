package me.katoro.autedium.grindfulness.client.toasts;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.net.PityHintPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

public final class ToastsClient {
	private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(4000L);
	private static final Map<BlockFamilies.Family, Integer> SESSION_COUNTS = new EnumMap<>(BlockFamilies.Family.class);

	private ToastsClient() {}

	private static boolean enabled() {
		return GrindConfig.get().isEnabled("toasts");
	}

	public static void init() {
		ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
			if (!enabled()) return;
			BlockFamilies.Family family = BlockFamilies.family(state);
			if (family == BlockFamilies.Family.NONE) return;

			int n = SESSION_COUNTS.merge(family, 1, Integer::sum);
			if (isMilestone(n)) {
				toast(
					Component.translatable("autedium_grindfulness.toast.milestone.title"),
					Component.translatable("autedium_grindfulness.toast.milestone." + family.name().toLowerCase(), n)
				);
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(PityHintPayload.TYPE, (payload, context) -> {
			if (!enabled()) return;
			toast(
				Component.translatable("autedium_grindfulness.toast.pity.title"),
				Component.translatable("autedium_grindfulness.toast.pity.body", payload.count(), payload.threshold())
			);
		});
	}

	static boolean isMilestone(int n) {
		return n == 100 || n == 500 || (n > 0 && n % 1000 == 0);
	}

	private static void toast(Component title, Component body) {
		Minecraft client = Minecraft.getInstance();
		SystemToast.add(client.gui.toastManager(), TOAST_ID, title, body);
	}
}
