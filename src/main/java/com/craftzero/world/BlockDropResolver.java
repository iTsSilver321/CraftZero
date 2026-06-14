package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

import java.util.Random;

/**
 * Contextual Release 1.0 block drops.
 */
public final class BlockDropResolver {
    private static final float GRAVEL_FLINT_CHANCE = 0.10f;

    private BlockDropResolver() {
    }

    public static ItemStack getDrop(BlockType type, Random random) {
        if (type == null) {
            return null;
        }
        if (type == BlockType.GRAVEL && random != null && random.nextFloat() < GRAVEL_FLINT_CHANCE) {
            return new ItemStack(ItemType.FLINT, 1);
        }
        if (type == BlockType.REDSTONE_ORE) {
            return new ItemStack(ItemType.REDSTONE, randomBetween(random, 4, 5));
        }
        if (type == BlockType.LAPIS_ORE) {
            return new ItemStack(ItemType.LAPIS_LAZULI, randomBetween(random, 4, 8));
        }

        ItemType droppedItem = type.getDroppedItem();
        if (droppedItem == null) {
            return null;
        }
        int count = type == BlockType.DOUBLE_STONE_SLAB ? 2 : 1;
        return new ItemStack(droppedItem, count);
    }

    private static int randomBetween(Random random, int min, int max) {
        if (random == null) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }
}
