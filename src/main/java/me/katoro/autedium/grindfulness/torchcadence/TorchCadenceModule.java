package me.katoro.autedium.grindfulness.torchcadence;

import me.katoro.autedium.grindfulness.core.GrindConfig;
import me.katoro.autedium.grindfulness.core.GrindModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

// auto-torch while caving: sneak with a torch in either hand and dark spots
// get lit as you walk. every torch comes out of your stack — the click is
// removed, the cost is not. weight 1.
public final class TorchCadenceModule implements GrindModule {
	@Override
	public String id() {
		return "torch_cadence";
	}

	@Override
	public int fairnessWeight() {
		return 1;
	}

	@Override
	public void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!enabled()) return;
			for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
				// per-dimension game time so the cadence stays honest per level
				if (!TorchCadenceRules.shouldCheck(sp.level().getGameTime())) continue;
				// creative skipped: vanilla placement wouldn't consume, this would
				if (!sp.isShiftKeyDown() || sp.isDeadOrDying() || sp.isSpectator() || sp.isCreative()) continue;

				ItemStack torches = torchStack(sp);
				if (torches == null) continue;

				ServerLevel level = (ServerLevel) sp.level();
				BlockPos feet = sp.blockPosition();
				int light = level.getBrightness(LightLayer.BLOCK, feet);
				if (!TorchCadenceRules.shouldPlace(light,
						GrindConfig.get().torchLightThreshold,
						torchNearby(level, feet))) {
					continue;
				}
				BlockState feetState = level.getBlockState(feet);
				// water/lava is replaceable but a torch can't live in it — don't delete fluid sources
				if (!feetState.canBeReplaced() || !feetState.getFluidState().isEmpty()) continue;

				BlockState placed = placeableTorch(level, feet);
				if (placed == null) continue; // no valid support below or on any side

				level.setBlockAndUpdate(feet, placed);
				SoundType sound = placed.getSoundType();
				level.playSound(null, feet, sound.getPlaceSound(), SoundSource.BLOCKS,
					(sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
				torches.shrink(1); // one per check max, full cost paid
			}
		});
	}

	// torch in either hand — main hand wins if both
	private static ItemStack torchStack(ServerPlayer sp) {
		if (sp.getMainHandItem().is(Items.TORCH)) return sp.getMainHandItem();
		if (sp.getOffhandItem().is(Items.TORCH)) return sp.getOffhandItem();
		return null;
	}

	// cluster guard: any torch (standing or wall) within a 2-block cube
	private static boolean torchNearby(ServerLevel level, BlockPos center) {
		int r = TorchCadenceRules.SPACING;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) return true;
		}
		return false;
	}

	// standing torch if the floor holds it, otherwise first wall that does
	private static BlockState placeableTorch(ServerLevel level, BlockPos pos) {
		BlockState standing = Blocks.TORCH.defaultBlockState();
		if (standing.canSurvive(level, pos)) return standing;
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockState wall = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, dir);
			if (wall.canSurvive(level, pos)) return wall;
		}
		return null;
	}
}
