package me.katoro.autedium.grindfulness.pityping;

import com.mojang.serialization.Codec;
import me.katoro.autedium.grindfulness.GrindfulnessMod;
import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.net.GrindNetworking;
import me.katoro.autedium.grindfulness.veinminer.VeinScanner;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class PityPingModule implements GrindModule {
	public static final AttachmentType<Integer> PITY_COUNT = AttachmentRegistry.create(
		GrindfulnessMod.id("pity_count"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
	);

	private static final int GLOW_ARGB = 0x59FFD700; // translucent gold
	private static final int GLOW_TICKS = 200;       // 10s
	private static final int MAX_COUNT_Y = 16;       // only deep mining counts
	private static final int HINT_INTERVAL = 100;

	@Override
	public String id() {
		return "pity_ping";
	}

	@Override
	public int fairnessWeight() {
		return 1;
	}

	@Override
	public void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!enabled() || level.isClientSide() || !(player instanceof ServerPlayer sp)) return;

			if (isDiamondOre(state)) {
				sp.setAttached(PITY_COUNT, 0); // they found one on their own, streak over
				return;
			}

			if (pos.getY() >= MAX_COUNT_Y) return;
			if (!state.is(BlockFamilies.STONE_FAMILY)) return;
			if (!sp.getMainHandItem().is(ItemTags.PICKAXES)) return;

			int count = sp.getAttachedOrElse(PITY_COUNT, 0) + 1;
			int threshold = GrindConfig.get().pityThreshold;

			if (count >= threshold) {
				BlockPos found = findNearestDiamond(level, pos, GrindConfig.get().pityScanRadius);
				if (found != null) {
					Block target = level.getBlockState(found).getBlock();
					List<BlockPos> vein = new ArrayList<>(VeinScanner.scan(found, p -> level.getBlockState(p).is(target), 8));
					vein.add(found);
					GrindNetworking.sendGlow(sp, vein, GLOW_ARGB, GLOW_TICKS);
					count = 0; // revealed -> reset. mining it also resets but belt n suspenders
				} else {
					// nothing in range?? back off n retry after pityRecheck more blocks
					count = threshold - GrindConfig.get().pityRecheck;
				}
			} else if (count % HINT_INTERVAL == 0) {
				GrindNetworking.sendPityHint(sp, count, threshold);
			}

			sp.setAttached(PITY_COUNT, count);
		});
	}

	private static boolean isDiamondOre(BlockState state) {
		return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
	}

	// full cube scan, ~275k reads at r=32. sounds insane but it runs once per pityRecheck blocks so its fine
	private static BlockPos findNearestDiamond(Level level, BlockPos center, int radius) {
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (isDiamondOre(level.getBlockState(p))) {
				double d = p.distSqr(center);
				if (d < bestDist) {
					bestDist = d;
					best = p.immutable();
				}
			}
		}
		return best;
	}
}
