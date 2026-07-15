package me.katoro.autedium.grindfulness.client.vigil;

import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.net.VigilWaitPayload;
import me.katoro.autedium.grindfulness.vigil.VigilRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

// skyrim-style wait menu. plain vanilla Screen, NOT YACL — three widgets and
// two lines of live math. slider picks 1-8 game hours in 0.5 steps, live-capped
// at remaining daytime (the cap was computed when the screen opened; the server
// re-clamps on confirm anyway, so a sunset mid-menu can't oversell).
public final class VigilScreen extends Screen {
	private final double maxHours; // >= 1.0, guaranteed by VigilClient before opening
	private double hours;

	public VigilScreen(double maxHours) {
		super(Component.translatable("autedium_grindfulness.vigil.screen.title"));
		this.maxHours = maxHours;
		this.hours = VigilRules.MIN_WAIT_HOURS;
	}

	@Override
	protected void init() {
		int cx = width / 2;
		int cy = height / 2;

		addRenderableWidget(new HoursSlider(cx - 100, cy - 34, 200, 20));
		addRenderableWidget(Button.builder(
				Component.translatable("autedium_grindfulness.vigil.screen.confirm"),
				b -> confirm())
			.bounds(cx - 102, cy + 34, 100, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
			.bounds(cx + 2, cy + 34, 100, 20).build());
	}

	private void confirm() {
		ClientPlayNetworking.send(new VigilWaitPayload((int) Math.round(hours * 2)));
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int cy = height / 2;
		graphics.centeredText(font, title, cx, cy - 58, 0xFFFFFFFF);

		int multiplier = VigilRules.clampMultiplier(GrindConfig.get().vigilMultiplier);
		double seconds = VigilRules.estimatedRealSeconds(hours, multiplier);
		double food = VigilRules.estimatedFoodCost(hours, multiplier);
		String clock = Double.isInfinite(seconds) ? "∞"
			: String.format(Locale.ROOT, "%d:%02d", (long) seconds / 60, (long) seconds % 60);
		graphics.centeredText(font,
			Component.translatable("autedium_grindfulness.vigil.screen.real_time", clock),
			cx, cy - 4, 0xFFA0A0A0);
		String foodText = Double.isInfinite(food) ? "∞"
			: String.format(Locale.ROOT, "%.1f", food);
		graphics.centeredText(font,
			Component.translatable("autedium_grindfulness.vigil.screen.hunger", foodText),
			cx, cy + 10, 0xFFA0A0A0);
	}

	@Override
	public boolean isPauseScreen() {
		return false; // time keeps flowing — that's the whole point
	}

	// 1..maxHours in 0.5 steps. AbstractSliderButton value is 0..1; we snap it
	// to the step grid so the label never shows a fake in-between value.
	private final class HoursSlider extends AbstractSliderButton {
		private final int steps; // number of 0.5 increments above MIN

		HoursSlider(int x, int y, int w, int h) {
			super(x, y, w, h, CommonComponents.EMPTY, 0.0);
			this.steps = (int) Math.round((maxHours - VigilRules.MIN_WAIT_HOURS) * 2);
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("autedium_grindfulness.vigil.screen.hours",
				String.format(Locale.ROOT, "%.1f", hours)));
		}

		@Override
		protected void applyValue() {
			int step = steps == 0 ? 0 : (int) Math.round(value * steps);
			hours = VigilRules.MIN_WAIT_HOURS + step * 0.5;
			value = steps == 0 ? 0.0 : (double) step / steps;
		}
	}
}
