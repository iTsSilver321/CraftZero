package com.craftzero.ui.menu;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.world.tile.BrewingRecipeRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class CreativeCatalog {
    private static final Set<ItemType> COMMAND_ONLY_ITEMS = EnumSet.of(
            ItemType.FIRE,
            ItemType.MOB_SPAWNER,
            ItemType.DOUBLE_STONE_SLAB,
            ItemType.DOUBLE_SANDSTONE_SLAB,
            ItemType.DOUBLE_WOODEN_SLAB,
            ItemType.DOUBLE_COBBLESTONE_SLAB,
            ItemType.DOUBLE_BRICK_SLAB,
            ItemType.DOUBLE_STONE_BRICK_SLAB,
            ItemType.LOCKED_CHEST,
            ItemType.INFESTED_STONE,
            ItemType.INFESTED_COBBLESTONE,
            ItemType.INFESTED_STONE_BRICK);

    private CreativeCatalog() {
    }

    public static boolean isCommandOnly(ItemType type) {
        return type != null && COMMAND_ONLY_ITEMS.contains(type);
    }

    public static List<CreativeCatalogEntry> entries() {
        List<CreativeCatalogEntry> entries = new ArrayList<>();
        List<ItemType> types = new ArrayList<>(List.of(ItemType.values()));
        types.sort(Comparator.comparingInt(ItemType::getId).thenComparingInt(ItemType::getDataValue));
        for (ItemType type : types) {
            if (isCommandOnly(type)) {
                continue;
            }
            if (type == ItemType.POTION) {
                for (PotionData potion : BrewingRecipeRegistry.creativePotions()) {
                    ItemStack stack = new ItemStack(ItemType.POTION, 1);
                    stack.setPotionData(potion);
                    entries.add(new CreativeCatalogEntry(stack));
                }
                continue;
            }
            entries.add(new CreativeCatalogEntry(stackFor(type)));
        }
        return List.copyOf(entries);
    }

    private static ItemStack stackFor(ItemType type) {
        int count = type.getMaxStackSize() <= 1 || type.isDamageable() ? 1 : type.getMaxStackSize();
        return new ItemStack(type, count);
    }
}
