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
        DURATIONS.put(ItemType.OAK_LOG, 300);
        DURATIONS.put(ItemType.OAK_PLANKS, 300);
        DURATIONS.put(ItemType.STICK, 100);
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
