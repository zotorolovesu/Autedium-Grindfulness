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
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ConfigScreenFactory {
	private ConfigScreenFactory() {}

	public static Component verdictText(int score) {
		ChatFormatting color = switch (Math.min(Math.max(score, 0), 5)) {
			case 0, 1 -> ChatFormatting.GREEN;
			case 2, 3 -> ChatFormatting.YELLOW;
			default -> ChatFormatting.RED;
		};
		return Component.translatable("autedium_grindfulness.verdict.prefix")
			.append(Component.translatable("autedium_grindfulness.verdict." + FairnessMeter.verdictKey(score)).withStyle(color));
	}

	public static Screen create(@Nullable Screen parent) {
		// unsaved toggle state, this drives the live verdict
		Map<String, Boolean> pending = new HashMap<>();
		for (GrindModule m : ModuleRegistry.all()) {
			pending.put(m.id(), m.enabled());
		}

		Component[] verdictHolder = { verdictText(FairnessMeter.score(pending)) };
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
						verdictState.set(verdictText(FairnessMeter.score(pending)));
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
