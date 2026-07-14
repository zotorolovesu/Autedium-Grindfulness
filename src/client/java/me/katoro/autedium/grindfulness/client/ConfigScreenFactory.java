package me.katoro.autedium.grindfulness.client;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import me.katoro.autedium.grindfulness.core.FairnessMeter;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.core.ModuleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ConfigScreenFactory {
	private ConfigScreenFactory() {}

	private static final ChatFormatting[] RAINBOW = {
		ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW,
		ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE,
	};
	// raw rgb versions for the yacl label, the ChatFormatting enum codes werent showing there
	private static final int[] RAINBOW_RGB = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0xFF55FF};
	private static final int[] TIER_RGB = {0xAAAAAA, 0x55FF55, 0x55FFFF, 0xFFFF55, 0xFFAA00, 0xFF5555};

	// yacl label version: rgb styles, no signature
	public static Component yaclVerdict(int score) {
		int clamped = Math.min(Math.max(score, 0), 5);
		String key = "autedium_grindfulness.verdict." + FairnessMeter.verdictKey(score);
		net.minecraft.network.chat.MutableComponent verdict = Component.empty();
		if (clamped >= 5) {
			String text = net.minecraft.client.resources.language.I18n.get(key);
			for (int i = 0; i < text.length(); i++) {
				final int rgb = RAINBOW_RGB[i % RAINBOW_RGB.length];
				verdict.append(Component.literal(String.valueOf(text.charAt(i)))
					.withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)).withBold(true)));
			}
		} else {
			final int rgb = TIER_RGB[clamped];
			verdict = Component.translatable(key)
				.withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)));
		}
		return Component.translatable("autedium_grindfulness.verdict.prefix").append(verdict);
	}

	public static Component verdictText(int score) {
		int clamped = Math.min(Math.max(score, 0), 5);
		// full gradient, one vibe per tier
		ChatFormatting color = switch (clamped) {
			case 0 -> ChatFormatting.GRAY;
			case 1 -> ChatFormatting.GREEN;
			case 2 -> ChatFormatting.AQUA;
			case 3 -> ChatFormatting.YELLOW;
			case 4 -> ChatFormatting.GOLD;
			default -> ChatFormatting.RED;
		};
		String key = "autedium_grindfulness.verdict." + FairnessMeter.verdictKey(score);
		Component verdict;
		if (clamped >= 5) {
			// max tier goes full 2012 forum signature rainbow. intentional. no regrets
			MutableComponent rainbow = Component.empty();
			String text = net.minecraft.client.resources.language.I18n.get(key);
			for (int i = 0; i < text.length(); i++) {
				rainbow.append(Component.literal(String.valueOf(text.charAt(i)))
					.withStyle(RAINBOW[i % RAINBOW.length], ChatFormatting.BOLD));
			}
			verdict = rainbow;
		} else {
			verdict = Component.translatable(key).withStyle(color);
		}
		return Component.translatable("autedium_grindfulness.verdict.prefix")
			.append(verdict)
			.append(Component.literal(" — KatoroCodesShit").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	public static Screen create(@Nullable Screen parent) {
		// unsaved toggle state, this drives the live verdict
		Map<String, Boolean> pending = new HashMap<>();
		for (GrindModule m : ModuleRegistry.all()) {
			pending.put(m.id(), m.enabled());
		}

		Component[] verdictHolder = { yaclVerdict(FairnessMeter.score(pending)) };
		StateManager<Component> verdictState = StateManager.createSimple(
			Component.empty(), () -> verdictHolder[0], c -> verdictHolder[0] = c);

		var category = ConfigCategory.createBuilder()
			.name(Component.translatable("autedium_grindfulness.config.category"))
			.option(LabelOption.createBuilder().state(verdictState).build());

		for (GrindModule module : ModuleRegistry.all()) {
			String id = module.id();
			category.option(Option.<Boolean>createBuilder()
				.name(Component.translatable("autedium_grindfulness.module." + id))
				.description(OptionDescription.of(Component.translatable("autedium_grindfulness.module." + id + ".desc")))
				.binding(true,
					() -> GrindConfig.get().isEnabled(id),
					value -> GrindConfig.get().setEnabled(id, value))
				.controller(TickBoxControllerBuilder::create)
				.addListener((opt, event) -> {
					if (event == OptionEventListener.Event.STATE_CHANGE || event == OptionEventListener.Event.INITIAL) {
						pending.put(id, opt.pendingValue());
						verdictState.set(yaclVerdict(FairnessMeter.score(pending)));
					}
				})
				.build());
		}

		return YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("autedium_grindfulness.config.title"))
			.category(category.build())
			.save(GrindConfig::save)
			.build()
			.generateScreen(parent); // yacl gets mad if u reuse generateScreen, fresh instance every time
	}
}
