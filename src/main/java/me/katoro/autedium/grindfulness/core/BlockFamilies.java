package me.katoro.autedium.grindfulness.core;

import me.katoro.autedium.grindfulness.GrindfulnessMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockFamilies {
	public static final TagKey<Block> STONE_FAMILY = TagKey.create(Registries.BLOCK, GrindfulnessMod.id("stone_family"));
	public static final TagKey<Block> LOG_FAMILY = TagKey.create(Registries.BLOCK, GrindfulnessMod.id("log_family"));
	public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, GrindfulnessMod.id("ores"));

	public enum Family { STONE, LOG, NONE }

	private BlockFamilies() {}

	public static Family family(BlockState state) {
		if (state.is(LOG_FAMILY)) return Family.LOG;
		if (state.is(STONE_FAMILY)) return Family.STONE;
		return Family.NONE;
	}
}
