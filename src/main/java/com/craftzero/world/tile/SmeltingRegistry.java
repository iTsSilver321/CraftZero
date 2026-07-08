package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

import java.util.HashMap;
import java.util.Map;

public final class SmeltingRegistry {
    private static final Map<ItemType, ItemStack> RECIPES = new HashMap<>();

    static {
        register(ItemType.IRON_ORE, ItemType.IRON_INGOT);
        register(ItemType.GOLD_ORE, ItemType.GOLD_INGOT);
        register(ItemType.OAK_LOG, ItemType.CHARCOAL);
        register(ItemType.SPRUCE_LOG, ItemType.CHARCOAL);
        register(ItemType.BIRCH_LOG, ItemType.CHARCOAL);
        register(ItemType.RAW_PORKCHOP, ItemType.COOKED_PORKCHOP);
        register(ItemType.RAW_BEEF, ItemType.STEAK);
        register(ItemType.RAW_CHICKEN, ItemType.COOKED_CHICKEN);
        register(ItemType.RAW_FISH, ItemType.COOKED_FISH);
        register(ItemType.SAND, ItemType.GLASS);
        register(ItemType.COBBLESTONE, ItemType.STONE);
        register(ItemType.CLAY_BALL, ItemType.BRICK_ITEM);
        register(ItemType.CACTUS, ItemType.CACTUS_GREEN);
    }

    private SmeltingRegistry() {
    }

    private static void register(ItemType input, ItemType output) {
        RECIPES.put(input, new ItemStack(output, 1));
    }

    public static ItemStack getResult(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        ItemStack result = RECIPES.get(input.getType());
        return result != null ? result.copy() : null;
    }
}
