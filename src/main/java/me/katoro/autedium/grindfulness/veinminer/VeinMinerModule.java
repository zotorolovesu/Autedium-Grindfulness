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

			int cap;
			if (state.is(BlockFamilies.ORES)) {
				cap = GrindConfig.get().veinCapOres;
			} else if (state.is(BlockFamilies.LOG_FAMILY)) {
				cap = GrindConfig.get().veinCapLogs;
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
