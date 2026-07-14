package me.katoro.autedium.grindfulness.core;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 0 wood, 1 stone, 2 gold, 3 iron, 4 diamond, 5 netherite. modded tools land on iron, sue me
public final class ToolTiers {
	private ToolTiers() {}

	public static int index(ItemStack s) {
		if (s.is(Items.NETHERITE_PICKAXE) || s.is(Items.NETHERITE_AXE)) return 5;
		if (s.is(Items.DIAMOND_PICKAXE) || s.is(Items.DIAMOND_AXE)) return 4;
		if (s.is(Items.IRON_PICKAXE) || s.is(Items.IRON_AXE)) return 3;
		if (s.is(Items.GOLDEN_PICKAXE) || s.is(Items.GOLDEN_AXE)) return 2;
		if (s.is(Items.STONE_PICKAXE) || s.is(Items.STONE_AXE)) return 1;
		if (s.is(Items.WOODEN_PICKAXE) || s.is(Items.WOODEN_AXE)) return 0;
		return 3;
	}
}
