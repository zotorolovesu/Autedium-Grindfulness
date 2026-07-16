package me.katoro.autedium.grindfulness.encore;

import me.katoro.autedium.grindfulness.core.GrindModule;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;

// breeding-cooldown buyout: an adult animal on post-breed cooldown accepts a
// DOUBLE feed (2 breeding items, both consumed) to become love-ready again
// instantly. never forces love, never touches babies or already-in-love —
// only the age countdown is bought out; the usual 1-item feed then starts
// love as normal. weight 3.
public final class EncoreModule implements GrindModule {
	@Override
	public String id() {
		return "encore";
	}

	@Override
	public int fairnessWeight() {
		return 3;
	}

	@Override
	public void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!enabled() || level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
			if (!(entity instanceof Animal animal)) return InteractionResult.PASS;

			ItemStack held = sp.getMainHandItem();
			if (!EncoreRules.canBuyout(animal.getAge(), animal.isFood(held), held.getCount())) {
				return InteractionResult.PASS; // vanilla handles feeding/love/babies
			}

			Component itemName = held.getHoverName(); // before shrink — a 2-stack becomes "Air" after
			held.shrink(EncoreRules.ITEMS_REQUIRED);
			animal.setAge(0);
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					animal.getX(), animal.getY() + animal.getBbHeight(), animal.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
			}
			sp.sendOverlayMessage(Component.translatable(
				"autedium_grindfulness.encore.buyout", itemName));
			// consume the interaction so vanilla doesn't also feed this click
			return InteractionResult.SUCCESS;
		});
	}
}
