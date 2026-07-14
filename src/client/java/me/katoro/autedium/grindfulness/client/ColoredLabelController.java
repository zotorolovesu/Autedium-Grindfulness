package me.katoro.autedium.grindfulness.client;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// yacl's LabelController nukes every char style w GuiUtils.overrideStyle so labels
// can literally never be colored. this is that class minus the nuking. thats it
public record ColoredLabelController(Option<Component> option) implements Controller<Component> {
	@Override
	public Component formatValue() {
		return option().pendingValue();
	}

	@Override
	public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
		return new Element(screen, widgetDimension);
	}

	public class Element extends AbstractWidget {
		private List<FormattedCharSequence> wrappedText;
		protected MultiLineLabel wrappedTooltip;
		protected boolean focused;
		protected final YACLScreen screen;

		public Element(YACLScreen screen, Dimension<Integer> dim) {
			super(dim);
			this.screen = screen;
			option().addEventListener((opt, event) -> updateTooltip());
			updateTooltip();
			updateText();
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			updateText();

			int y = getDimension().y();
			ActiveTextCollector textCollector = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR);
			boolean available = option().available();
			Style grayed = Style.EMPTY.withColor(0xFFA0A0A0);
			for (FormattedCharSequence text : wrappedText) {
				// the whole point: available text keeps its own styles
				textCollector.accept(
					getDimension().x() + getXPadding(),
					y + getYPadding(),
					available ? text : GuiUtils.overrideStyle(text, grayed)
				);
				y += textRenderer.lineHeight;
			}
		}

		private int getXPadding() {
			return 4;
		}

		private int getYPadding() {
			return 3;
		}

		private void updateText() {
			wrappedText = textRenderer.split(formatValue(), getDimension().width() - getXPadding() * 2);
			setDimension(getDimension().withHeight(wrappedText.size() * textRenderer.lineHeight + getYPadding() * 2));
		}

		private void updateTooltip() {
			this.wrappedTooltip = MultiLineLabel.create(textRenderer, option().tooltip(), screen.width / 3 * 2 - 10);
		}

		@Override
		public boolean matchesSearch(String query) {
			return formatValue().getString().toLowerCase().contains(query.toLowerCase());
		}

		@Override
		public ComponentPath nextFocusPath(FocusNavigationEvent event) {
			return null; // not focusable, its a label
		}

		@Override
		public boolean isFocused() {
			return focused;
		}

		@Override
		public void setFocused(boolean focused) {
			this.focused = focused;
		}

		@Override
		public void updateNarration(NarrationElementOutput builder) {
			builder.add(NarratedElementType.TITLE, formatValue());
		}

		@Override
		public NarrationPriority narrationPriority() {
			return NarrationPriority.FOCUSED;
		}
	}
}
