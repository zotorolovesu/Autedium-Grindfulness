package me.katoro.autedium.grindfulness.brewqueue;

import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// brewing stand ingredient queue: right-click a busy stand with more
// ingredients and they queue up (paid from your hand NOW), auto-inserting as
// the slot frees. same ingredients, same blaze powder, same brew time — only
// the re-loading clicks are removed. items are conserved exactly: queued items
// exist only in the queue and come back out into the stand, or onto the ground
// if the stand dies / the module is switched off / the server stops. weight 1.
public final class BrewQueueModule implements GrindModule {
	// vanilla ingredient slot index — javap'd BrewingStandBlockEntity.canPlaceItem
	// bytecode: `iload_1; iconst_3; if_icmpne` (INGREDIENT_SLOT is private, value 3)
	private static final int INGREDIENT_SLOT = 3;

	// per-stand queue, keyed by dimension + pos. server in-memory only —
	// deliberately not persisted; SERVER_STOPPING dumps everything in-world
	private record StandKey(ResourceKey<Level> dimension, BlockPos pos) {}

	private static final Map<StandKey, ArrayDeque<ItemStack>> QUEUES = new ConcurrentHashMap<>();

	@Override
	public String id() {
		return "brew_queue";
	}

	@Override
	public int fairnessWeight() {
		return 1;
	}

	@Override
	public void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			// server-only + main hand only; client PASSes and vanilla's "server
			// opens the menu" flow simply never fires when we intercept
			if (!enabled() || level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (player.isSpectator() || player.isShiftKeyDown()) return InteractionResult.PASS;

			BlockPos pos = hit.getBlockPos();
			if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) return InteractionResult.PASS;

			ItemStack held = player.getItemInHand(hand);
			if (held.isEmpty() || !level.potionBrewing().isIngredient(held)) return InteractionResult.PASS;
			// slot free → vanilla gui can take the item itself, stay out of the way
			if (stand.getItem(INGREDIENT_SLOT).isEmpty()) return InteractionResult.PASS;

			int cap = Math.clamp(GrindConfig.get().brewQueueCap, BrewQueueRules.MIN_CAP, BrewQueueRules.MAX_CAP);
			ArrayDeque<ItemStack> queue = QUEUES.computeIfAbsent(key(level, pos), k -> new ArrayDeque<>());
			// queue full → PASS so the click still opens the gui like vanilla
			if (queue.size() >= cap) return InteractionResult.PASS;
			queue.addLast(held.copyWithCount(1));

			held.shrink(1); // paid on queue — the item now exists ONLY in the queue
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable(
					"autedium_grindfulness.brew_queue.queued", queue.size(), cap));
			}
			return InteractionResult.SUCCESS_SERVER;
		});

		// prompt drop when a player breaks a queued stand (the tick sweep below
		// would catch it a tick later anyway; this puts the items with the drops).
		// deliberately NOT gated on enabled() — conservation beats the toggle
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level.isClientSide()) return;
			ArrayDeque<ItemStack> queue = QUEUES.remove(key(level, pos));
			if (queue != null) dropAll((ServerLevel) level, pos, queue);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (QUEUES.isEmpty()) return;
			if (!enabled()) {
				// module toggled off: give every queued item back in-world.
				// lenient — unloaded chunks retry next tick, server's still running
				dumpLoaded(server);
				return;
			}
			Iterator<Map.Entry<StandKey, ArrayDeque<ItemStack>>> it = QUEUES.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<StandKey, ArrayDeque<ItemStack>> entry = it.next();
				ServerLevel level = server.getLevel(entry.getKey().dimension());
				if (level == null) continue; // dimension not around right now, keep waiting
				BlockPos pos = entry.getKey().pos();
				if (!level.isLoaded(pos)) continue; // frozen chunk, frozen queue

				ArrayDeque<ItemStack> queue = entry.getValue();
				if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) {
					// stand gone without a player break (explosion, /setblock, whatever)
					dropAll(level, pos, queue);
					it.remove();
					continue;
				}
				if (stand.getItem(INGREDIENT_SLOT).isEmpty() && !queue.isEmpty()) {
					stand.setItem(INGREDIENT_SLOT, queue.pollFirst()); // blaze powder + brew time stay vanilla
					stand.setChanged();
				}
				if (queue.isEmpty()) it.remove();
			}
		});

		// world's closing and there is no next tick: force the chunks and drop
		// EVERYTHING, then clear — a static map surviving into the next
		// singleplayer world would dump world A's items into world B
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Map.Entry<StandKey, ArrayDeque<ItemStack>> entry : QUEUES.entrySet()) {
				ServerLevel level = server.getLevel(entry.getKey().dimension());
				if (level == null) continue; // dimension deleted mid-run — nowhere to drop
				BlockPos pos = entry.getKey().pos();
				level.getChunk(pos); // force-load so the drop actually lands
				dropAll(level, pos, entry.getValue());
			}
			QUEUES.clear();
		});
	}

	private static StandKey key(Level level, BlockPos pos) {
		return new StandKey(level.dimension(), pos.immutable());
	}

	private static void dropAll(ServerLevel level, BlockPos pos, ArrayDeque<ItemStack> queue) {
		ItemStack stack;
		while ((stack = queue.pollFirst()) != null) {
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		}
	}

	private static void dumpLoaded(MinecraftServer server) {
		Iterator<Map.Entry<StandKey, ArrayDeque<ItemStack>>> it = QUEUES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<StandKey, ArrayDeque<ItemStack>> entry = it.next();
			ServerLevel level = server.getLevel(entry.getKey().dimension());
			BlockPos pos = entry.getKey().pos();
			if (level == null || !level.isLoaded(pos)) continue; // retry next tick
			dropAll(level, pos, entry.getValue());
			it.remove();
		}
	}
}
