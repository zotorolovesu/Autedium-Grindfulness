package me.katoro.autedium.grindfulness.client;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import me.katoro.autedium.grindfulness.core.FairnessMeter;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.core.ModuleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public final class ConfigScreenFactory {
	private ConfigScreenFactory() {}

	private static final ChatFormatting[] RAINBOW = {
		ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW,
		ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE,
	};
	// raw rgb versions for the yacl label, the ChatFormatting enum codes werent showing there
	private static final int[] RAINBOW_RGB = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0xFF55FF};
	private static final int[] TIER_RGB = {0xAAAAAA, 0x55FF55, 0x55FFFF, 0xFFFF55, 0xFFAA00, 0xFF5555};

	private static final int ACCENT_RGB = 0x55FFAA;   // grindfulness mint, one accent everywhere
	private static final int MUTED_RGB = 0x777777;

	// module id -> group key; anything unmapped lands in "misc" so new modules still auto-appear
	private static final Map<String, String> MODULE_GROUP = Map.of(
		"vein_miner", "mining",
		"flow_haste", "mining",
		"pity_ping", "sense",
		"prospector", "sense",
		"vigil", "ritual",
		"toasts", "feedback",
		"grind_ledger", "feedback"
	);
	private static final String[] GROUP_ORDER = {"mining", "sense", "ritual", "feedback", "misc"};

	// yacl label version: rgb styles, no signature
	public static Component yaclVerdict(int score) {
		int clamped = Math.min(Math.max(score, 0), 5);
		String key = "autedium_grindfulness.verdict." + FairnessMeter.verdictKey(score);
		MutableComponent verdict = Component.empty();
		if (clamped >= 5) {
			String text = net.minecraft.client.resources.language.I18n.get(key);
			for (int i = 0; i < text.length(); i++) {
				final int rgb = RAINBOW_RGB[i % RAINBOW_RGB.length];
				verdict.append(Component.literal(String.valueOf(text.charAt(i)))
					.withStyle(s -> s.withColor(TextColor.fromRgb(rgb)).withBold(true)));
			}
		} else {
			final int rgb = TIER_RGB[clamped];
			verdict = Component.translatable(key)
				.withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
		}
		return Component.translatable("autedium_grindfulness.verdict.prefix")
			.withStyle(s -> s.withColor(TextColor.fromRgb(0xE0E0E0)))
			.append(verdict);
	}

	// ◆◆◆◇◇ 3/5 meter, filled diamonds tinted by tier
	public static Component yaclMeter(int score) {
		int clamped = Math.min(Math.max(score, 0), 5);
		final int fillRgb = TIER_RGB[clamped];
		MutableComponent bar = Component.empty();
		for (int i = 0; i < 5; i++) {
			boolean filled = i < clamped;
			bar.append(Component.literal(filled ? "◆" : "◇")
				.withStyle(s -> s.withColor(TextColor.fromRgb(filled ? fillRgb : 0x3F3F3F))));
		}
		bar.append(Component.literal("  " + clamped + "/5")
			.withStyle(s -> s.withColor(TextColor.fromRgb(MUTED_RGB))));
		return bar;
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

	// "✦ +N fairness" badge, colored by weight tier ("✦ free" for weight 0)
	private static Component weightBadge(int weight) {
		int tier = Math.min(Math.max(weight, 0), 5);
		final int rgb = TIER_RGB[tier];
		String text = weight <= 0
			? net.minecraft.client.resources.language.I18n.get("autedium_grindfulness.config.weight.free")
			: "+" + weight;
		return Component.literal("  ✦ " + text)
			.withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
	}

	private static Component moduleName(GrindModule module) {
		return Component.translatable("autedium_grindfulness.module." + module.id())
			.withStyle(s -> s.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(true))
			.append(weightBadge(module.fairnessWeight()));
	}

	private static OptionDescription moduleDescription(GrindModule module) {
		int weight = module.fairnessWeight();
		int tier = Math.min(Math.max(weight, 0), 5);
		final int rgb = TIER_RGB[tier];
		Component weightLine = weight <= 0
			? Component.translatable("autedium_grindfulness.config.weight.free.desc")
				.withStyle(s -> s.withColor(TextColor.fromRgb(rgb)))
			: Component.translatable("autedium_grindfulness.config.weight.desc", weight)
				.withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
		return OptionDescription.createBuilder()
			.text(Component.translatable("autedium_grindfulness.module." + module.id() + ".desc"))
			.text(Component.empty())
			.text(Component.literal("✦ ").withStyle(s -> s.withColor(TextColor.fromRgb(rgb))).append(weightLine))
			.build();
	}

	private static Component tunableName(String key) {
		return Component.translatable("autedium_grindfulness.option." + key)
			.withStyle(s -> s.withColor(TextColor.fromRgb(0xC8C8C8)));
	}

	private static OptionDescription tunableDesc(String key) {
		return OptionDescription.of(
			Component.translatable("autedium_grindfulness.option." + key + ".desc"),
			Component.empty(),
			Component.translatable("autedium_grindfulness.config.tunable.note")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	// fresh instance = compile-time defaults, so "reset to default" actually works
	private static final GrindConfig DEFAULTS = new GrindConfig();

	private static Option<Integer> intSlider(String key, int defaultVal, int min, int max, int step,
			IntSupplier getter, Consumer<Integer> setter, java.util.function.@Nullable IntFunction<Component> fmt) {
		var builder = Option.<Integer>createBuilder()
			.name(tunableName(key))
			.description(tunableDesc(key))
			.binding(defaultVal, getter::getAsInt, setter::accept)
			.controller(opt -> {
				var c = IntegerSliderControllerBuilder.create(opt).range(min, max).step(step);
				if (fmt != null) c.formatValue(v -> fmt.apply(v));
				return c;
			});
		return builder.build();
	}

	private static Component ticksFmt(int ticks) {
		return Component.literal(String.format("%.1fs", ticks / 20.0))
			.withStyle(s -> s.withColor(TextColor.fromRgb(ACCENT_RGB)));
	}

	private static Component plainFmt(int v, String suffix) {
		return Component.literal(v + suffix).withStyle(s -> s.withColor(TextColor.fromRgb(ACCENT_RGB)));
	}

	// tunables that live under each module's toggle. keyed by module id
	private static List<Option<Integer>> tunablesFor(String moduleId) {
		GrindConfig c = GrindConfig.get();
		List<Option<Integer>> list = new ArrayList<>();
		switch (moduleId) {
			case "vein_miner" -> {
				list.add(intSlider("vein_cap_ores", DEFAULTS.veinCapOres, 2, 64, 1, () -> c.veinCapOres, v -> c.veinCapOres = v, v -> plainFmt(v, " blocks")));
				list.add(intSlider("vein_cap_logs", DEFAULTS.veinCapLogs, 2, 128, 2, () -> c.veinCapLogs, v -> c.veinCapLogs = v, v -> plainFmt(v, " blocks")));
			}
			case "flow_haste" -> list.add(intSlider("flow_reset_ticks", DEFAULTS.flowResetTicks, 20, 600, 20, () -> c.flowResetTicks, v -> c.flowResetTicks = v, ConfigScreenFactory::ticksFmt));
			case "pity_ping" -> {
				list.add(intSlider("pity_threshold", DEFAULTS.pityThreshold, 100, 2000, 50, () -> c.pityThreshold, v -> c.pityThreshold = v, v -> plainFmt(v, " blocks")));
				list.add(intSlider("pity_recheck", DEFAULTS.pityRecheck, 20, 400, 20, () -> c.pityRecheck, v -> c.pityRecheck = v, v -> plainFmt(v, " blocks")));
				list.add(intSlider("pity_scan_radius", DEFAULTS.pityScanRadius, 8, 48, 4, () -> c.pityScanRadius, v -> c.pityScanRadius = v, v -> plainFmt(v, " blocks")));
			}
			case "prospector" -> {
				list.add(intSlider("prospect_radius", DEFAULTS.prospectRadius, 4, 24, 1, () -> c.prospectRadius, v -> c.prospectRadius = v, v -> plainFmt(v, " blocks")));
				list.add(intSlider("prospect_cooldown_ticks", DEFAULTS.prospectCooldownTicks, 100, 2400, 100, () -> c.prospectCooldownTicks, v -> c.prospectCooldownTicks = v, ConfigScreenFactory::ticksFmt));
			}
			case "vigil" -> {
				list.add(intSlider("vigil_multiplier", DEFAULTS.vigilMultiplier, 1, 16, 1, () -> c.vigilMultiplier, v -> c.vigilMultiplier = v, v -> plainFmt(v, "x")));
				list.add(intSlider("vigil_radius", DEFAULTS.vigilRadius, 1, 16, 1, () -> c.vigilRadius, v -> c.vigilRadius = v, v -> plainFmt(v, " blocks")));
				list.add(intSlider("vigil_hunger_floor", DEFAULTS.vigilHungerFloor, 0, 20, 1, () -> c.vigilHungerFloor, v -> c.vigilHungerFloor = v, v -> plainFmt(v, " / 20")));
			}
			case "grind_ledger" -> {
				list.add(intSlider("ledger_milestone", DEFAULTS.ledgerMilestone, 100, 2000, 100, () -> c.ledgerMilestone, v -> c.ledgerMilestone = v, v -> plainFmt(v, " blocks")));
				list.add(intSlider("ledger_idle_seconds", DEFAULTS.ledgerIdleSeconds, 15, 300, 15, () -> c.ledgerIdleSeconds, v -> c.ledgerIdleSeconds = v, v -> plainFmt(v, "s")));
			}
			default -> {}
		}
		return list;
	}

	private static Component groupName(String key) {
		return Component.literal("◈ ").withStyle(s -> s.withColor(TextColor.fromRgb(ACCENT_RGB)))
			.append(Component.translatable("autedium_grindfulness.group." + key)
				.withStyle(s -> s.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(true)));
	}

	public static Screen create(@Nullable Screen parent) {
		// unsaved toggle state, this drives the live verdict
		Map<String, Boolean> pending = new HashMap<>();
		for (GrindModule m : ModuleRegistry.all()) {
			pending.put(m.id(), m.enabled());
		}

		int initialScore = FairnessMeter.score(pending);
		Component[] verdictHolder = { yaclVerdict(initialScore) };
		StateManager<Component> verdictState = StateManager.createSimple(
			Component.empty(), () -> verdictHolder[0], c -> verdictHolder[0] = c);
		Component[] meterHolder = { yaclMeter(initialScore) };
		StateManager<Component> meterState = StateManager.createSimple(
			Component.empty(), () -> meterHolder[0], c -> meterHolder[0] = c);

		var category = ConfigCategory.createBuilder()
			.name(Component.translatable("autedium_grindfulness.config.category"))
			// header: live verdict + fairness meter + design-law tagline (root group, above everything)
			.option(coloredLabel("verdict", verdictState))
			.option(coloredLabel("meter", meterState))
			.option(coloredLabel("tagline", StateManager.createImmutable(
				Component.translatable("autedium_grindfulness.config.tagline")
					.withStyle(s -> s.withColor(TextColor.fromRgb(MUTED_RGB)).withItalic(true)))));

		// bucket modules into groups, registry order preserved inside each group
		Map<String, List<GrindModule>> byGroup = new LinkedHashMap<>();
		for (GrindModule module : ModuleRegistry.all()) {
			byGroup.computeIfAbsent(MODULE_GROUP.getOrDefault(module.id(), "misc"), k -> new ArrayList<>()).add(module);
		}

		for (String groupKey : GROUP_ORDER) {
			List<GrindModule> modules = byGroup.get(groupKey);
			if (modules == null || modules.isEmpty()) continue;
			var group = OptionGroup.createBuilder()
				.name(groupName(groupKey))
				.description(OptionDescription.of(
					Component.translatable("autedium_grindfulness.group." + groupKey + ".desc")));
			for (GrindModule module : modules) {
				String id = module.id();
				group.option(Option.<Boolean>createBuilder()
					.name(moduleName(module))
					.description(moduleDescription(module))
					.binding(true,
						() -> GrindConfig.get().isEnabled(id),
						value -> GrindConfig.get().setEnabled(id, value))
					.controller(opt -> BooleanControllerBuilder.create(opt).coloured(true).onOffFormatter())
					.addListener((opt, event) -> {
						if (event == OptionEventListener.Event.STATE_CHANGE || event == OptionEventListener.Event.INITIAL) {
							pending.put(id, opt.pendingValue());
							int score = FairnessMeter.score(pending);
							verdictState.set(yaclVerdict(score));
							meterState.set(yaclMeter(score));
						}
					})
					.build());
				List<Option<Integer>> tunables = tunablesFor(id);
				if (!tunables.isEmpty()) { // yacl's options() throws on empty — modules without tunables (toasts) crash the screen
					group.options(tunables);
				}
			}
			category.group(group.build());
		}

		return YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("autedium_grindfulness.config.title"))
			.category(category.build())
			.save(GrindConfig::save)
			.build()
			.generateScreen(parent); // yacl gets mad if u reuse generateScreen, fresh instance every time
	}

	private static Option<Component> coloredLabel(String name, StateManager<Component> state) {
		return Option.<Component>createBuilder()
			.name(Component.literal(name))
			.description(OptionDescription.EMPTY)
			.stateManager(state)
			.customController(ColoredLabelController::new)
			.build();
	}
}
