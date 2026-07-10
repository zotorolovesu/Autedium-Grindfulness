package me.katoro.autedium.grindfulness.prospector;

import me.katoro.autedium.grindfulness.core.BlockFamilies;
import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import me.katoro.autedium.grindfulness.net.GrindNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProspectorModule implements GrindModule {
	private static final int GLOW_ARGB = 0x5900FFFF;   // translucent cyan
	private static final int GLOW_TICKS = 100;         // 5s
	private static final int MAX_RESULTS = 64;

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

			long now = level.getGameTime();
			int cooldown = GrindConfig.get().prospectCooldownTicks;
			long last = lastUse.getOrDefault(sp.getUUID(), Long.MIN_VALUE);
			long remaining = cooldown - (now - last);
			if (remaining > 0) {
				sp.sendOverlayMessage(Component.translatable(
					"autedium_grindfulness.prospector.cooldown", (remaining + 19) / 20));
				return InteractionResult.SUCCESS;
			}

			int r = GrindConfig.get().prospectRadius;
			BlockPos center = sp.blockPosition();
			List<BlockPos> hits = new ArrayList<>();
			for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
				if (level.getBlockState(p).is(BlockFamilies.ORES)) {
					hits.add(p.immutable());
					if (hits.size() >= MAX_RESULTS) break;
				}
			}

			lastUse.put(sp.getUUID(), now);
			GrindNetworking.sendGlow(sp, hits, GLOW_ARGB, GLOW_TICKS);
			sp.sendOverlayMessage(Component.translatable("autedium_grindfulness.prospector.found", hits.size()));
			return InteractionResult.SUCCESS;
		});
	}
}
