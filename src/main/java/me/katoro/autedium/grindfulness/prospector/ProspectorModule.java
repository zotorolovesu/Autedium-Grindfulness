package me.katoro.autedium.grindfulness.prospector;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.net.GrindNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProspectorModule implements GrindModule {
	private static final int GLOW_ARGB = 0x59F2F2F2;   // white edges, greyish fill (renderer dims the fill)
	private static final int GLOW_TICKS = 100;         // 5s base
	private static final int MAX_RESULTS = 64;
	// scaling caps. these floors/ceilings are load bearing for fairness, dont raise them:
	// no cooldown floor = permanent xray radar, radius past 20 = why even mine
	private static final int COOLDOWN_FLOOR_TICKS = 400;      // 20s min no matter the enchants
	private static final int EFFICIENCY_DISCOUNT_TICKS = 80;  // -4s per efficiency lvl
	private static final int FORTUNE_BONUS_TICKS = 20;        // +1s glow per fortune lvl

	private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

	@Override
	public String id() {
		return "prospector";
	}

	@Override
	public int fairnessWeight() {
		return 2;
	}

	@Override
	public void init() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!enabled() || level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer sp) || !sp.isShiftKeyDown()) return InteractionResult.PASS;
			if (!sp.getMainHandItem().is(ItemTags.PICKAXES)) return InteractionResult.PASS;

			var stack = sp.getMainHandItem();
			int tier = me.katoro.autedium.grindfulness.core.ToolTiers.index(stack);
			long now = level.getGameTime();
			int goldBonus = tier == 2 ? 120 : 0; // gold loves shiny, -6s extra
			int cooldown = Math.max(COOLDOWN_FLOOR_TICKS,
				GrindConfig.get().prospectCooldownTicks - goldBonus
					- enchantLevel(level, stack, Enchantments.EFFICIENCY) * EFFICIENCY_DISCOUNT_TICKS);
			Long last = lastUse.get(sp.getUUID());
			if (last != null) {
				long remaining = cooldown - (now - last);
				if (remaining > 0) {
					sp.sendOverlayMessage(Component.translatable(
						"autedium_grindfulness.prospector.cooldown", (remaining + 19) / 20));
					return InteractionResult.SUCCESS;
				}
			}

			int r = radiusFor(stack, GrindConfig.get().prospectRadius);
			int glowTicks = GLOW_TICKS + enchantLevel(level, stack, Enchantments.FORTUNE) * FORTUNE_BONUS_TICKS;
			BlockPos center = sp.blockPosition();
			// collect EVERYTHING then keep the nearest N. capping mid-scan skipped ores
			// right in front of u cause betweenClosed walks from the cube corner
			List<BlockPos> hits = new ArrayList<>();
			for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
				if (level.getBlockState(p).is(BlockFamilies.ORES)) {
					hits.add(p.immutable());
				}
			}
			int keep = MAX_RESULTS + tier * 8; // 64..104, payload cap is 256 so fine
			if (hits.size() > keep) {
				hits.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(center)));
				hits = new ArrayList<>(hits.subList(0, keep));
			}

			lastUse.put(sp.getUUID(), now);
			// pings cost durability so a chest-loot gold pick is a legit dowsing rod
			// that dies after ~5 uses instead of a free forever-wand. self balancing
			stack.hurtAndBreak(6, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			GrindNetworking.sendGlow(sp, hits, GLOW_ARGB, glowTicks);
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.prospector.found", hits.size()));
			return InteractionResult.SUCCESS;
		});
	}

	// better pick = wider scan + more hits. caps at 20 radius (netherite) on purpose, see note up top
	private static final int[] RADIUS_BONUS = {0, 0, 1, 2, 6, 8};

	private static int radiusFor(ItemStack stack, int base) {
		return base + RADIUS_BONUS[me.katoro.autedium.grindfulness.core.ToolTiers.index(stack)];
	}

	private static int enchantLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
		var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		return EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(key), stack);
	}
}
