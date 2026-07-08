package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

import java.util.HashMap;
import java.util.Map;

public final class FuelRegistry {
    private static final Map<ItemType, Integer> DURATIONS = new HashMap<>();

    static {
        DURATIONS.put(ItemType.COAL, 1600);
        DURATIONS.put(ItemType.CHARCOAL, 1600);
        DURATIONS.put(ItemType.LAVA_BUCKET, 20000);
        DURATIONS.put(ItemType.SAPLING, 100);
        DURATIONS.put(ItemType.SPRUCE_SAPLING, 100);
        DURATIONS.put(ItemType.BIRCH_SAPLING, 100);
        DURATIONS.put(ItemType.BLAZE_ROD, 2400);

        DURATIONS.put(ItemType.BOOKSHELF, 300);
        DURATIONS.put(ItemType.CHEST, 300);
        DURATIONS.put(ItemType.CRAFTING_TABLE, 300);
        DURATIONS.put(ItemType.FENCE, 300);
        DURATIONS.put(ItemType.FENCE_GATE, 300);
        DURATIONS.put(ItemType.JUKEBOX, 300);
        DURATIONS.put(ItemType.LOCKED_CHEST, 300);
        DURATIONS.put(ItemType.NOTE_BLOCK, 300);
        DURATIONS.put(ItemType.OAK_LOG, 300);
        DURATIONS.put(ItemType.OAK_PLANKS, 300);
        DURATIONS.put(ItemType.OAK_STAIRS, 300);
        DURATIONS.put(ItemType.SPRUCE_LOG, 300);
        DURATIONS.put(ItemType.BIRCH_LOG, 300);
        DURATIONS.put(ItemType.STICK, 100);
        DURATIONS.put(ItemType.TRAPDOOR, 300);
        DURATIONS.put(ItemType.WOODEN_PRESSURE_PLATE, 300);
        DURATIONS.put(ItemType.BROWN_MUSHROOM_BLOCK, 300);
        DURATIONS.put(ItemType.RED_MUSHROOM_BLOCK, 300);
    }

    private FuelRegistry() {
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return DURATIONS.getOrDefault(stack.getType(), 0);
    }

    public static boolean isFuel(ItemStack stack) {
        return getBurnTime(stack) > 0;
    }
}
