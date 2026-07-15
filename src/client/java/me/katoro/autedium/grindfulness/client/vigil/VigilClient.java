package me.katoro.autedium.grindfulness.client.vigil;

import com.mojang.blaze3d.platform.InputConstants;
import me.katoro.autedium.grindfulness.GrindfulnessMod;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.vigil.VigilRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

// the wait key. V opens the skyrim-style vigil screen — but only if the client
// can already SEE it's eligible (day, fed, enough daylight left). that check is
// pure courtesy; the server revalidates everything on confirm.
public final class VigilClient {
	private static KeyMapping waitKey;

	private VigilClient() {}

	public static void init() {
		KeyMapping.Category category = KeyMapping.Category.register(GrindfulnessMod.id("autedium"));
		waitKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.autedium_grindfulness.vigil", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (waitKey.consumeClick()) tryOpen(client);
		});
	}

	private static void tryOpen(Minecraft client) {
		var player = client.player;
		var level = client.level;
		if (player == null || level == null || client.gui.screen() != null) return;
		if (!GrindConfig.get().isEnabled("vigil")) return;

		long dayTime = level.getDefaultClockTime();
		boolean day = VigilRules.isDay(dayTime);
		if (!VigilRules.canStart(day, player.getFoodData().getFoodLevel(), GrindConfig.get().vigilHungerFloor)) {
			player.sendOverlayMessage(Component.translatable(
				day ? "autedium_grindfulness.vigil.too_hungry" : "autedium_grindfulness.vigil.not_day"));
			return;
		}
		double maxHours = VigilRules.maxWaitHours(VigilRules.remainingDaytime(dayTime));
		if (maxHours < VigilRules.MIN_WAIT_HOURS) {
			player.sendOverlayMessage(Component.translatable("autedium_grindfulness.vigil.dusk"));
			return;
		}
		client.setScreenAndShow(new VigilScreen(maxHours));
	}
}
