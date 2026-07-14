package me.katoro.autedium.grindfulness.veinminer;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class VeinMinerModule implements GrindModule {
	// destroyBlock re-fires AFTER on this same thread, without this we recurse into oblivion
	private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);

	// wood/stone/gold/iron/diamond/netherite. ores land on 6/6/8/12/16/20 w default config
	private static final int[] ORE_CAP_BONUS = {-6, -6, -4, 0, 4, 8};
	// logs: 32/40/44/48/56/64 w default 64
	private static final int[] LOG_CAP_BONUS = {-32, -24, -20, -16, -8, 0};

	@Override
	public String id() {
		return "vein_miner";
	}

	@Override
	public int fairnessWeight() {
		return 1;
	}

	@Override
	public void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!enabled() || level.isClientSide() || CHAINING.get()) return;
			if (!(player instanceof ServerPlayer sp) || !sp.isShiftKeyDown()) return;

			// better tool = bigger chains. config = iron baseline for ores, netherite for logs
			int tier = me.katoro.autedium.grindfulness.core.ToolTiers.index(sp.getMainHandItem());
			int cap;
			if (state.is(BlockFamilies.ORES)) {
				cap = Math.max(1, GrindConfig.get().veinCapOres + ORE_CAP_BONUS[tier]);
			} else if (state.is(BlockFamilies.LOG_FAMILY)) {
				cap = Math.max(1, GrindConfig.get().veinCapLogs + LOG_CAP_BONUS[tier]);
			} else {
				return;
			}

			Block target = state.getBlock();
			List<BlockPos> vein = VeinScanner.scan(pos, p -> level.getBlockState(p).is(target), cap);

			CHAINING.set(true);
			try {
				for (BlockPos p : vein) {
					// tool died mid vein -> stop. destroyBlock already charges
					// durability/hunger/drops so the cost stays fully vanilla
					if (sp.getMainHandItem().isEmpty()) break;
					sp.gameMode.destroyBlock(p);
				}
			} finally {
				CHAINING.set(false);
			}
		});
	}
}
