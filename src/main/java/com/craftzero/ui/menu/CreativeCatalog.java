package com.craftzero.ui.menu;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.world.tile.BrewingRecipeRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CreativeCatalog {
    private CreativeCatalog() {
    }

    public static List<CreativeCatalogEntry> entries() {
        List<CreativeCatalogEntry> entries = new ArrayList<>();
        List<ItemType> types = new ArrayList<>(List.of(ItemType.values()));
        types.sort(Comparator.comparingInt(ItemType::getId).thenComparingInt(ItemType::getDataValue));
        for (ItemType type : types) {
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
