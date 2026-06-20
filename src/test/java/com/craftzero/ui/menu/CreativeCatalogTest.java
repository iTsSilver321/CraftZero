package com.craftzero.ui.menu;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CreativeCatalogTest {

    @Test
    @DisplayName("Creative catalog should include every implemented non-potion item type")
    void includesEveryImplementedItemType() {
        List<CreativeCatalogEntry> entries = CreativeCatalog.entries();
        Set<ItemType> visibleTypes = entries.stream()
                .map(CreativeCatalogEntry::type)
                .collect(Collectors.toSet());
        for (ItemType type : ItemType.values()) {
            assertTrue(visibleTypes.contains(type), "Missing creative catalog item " + type);
        }
    }

    @Test
    @DisplayName("Creative catalog should include legal Release 1.0 potion and splash variants")
    void includesPotionVariants() {
        List<ItemStack> potions = CreativeCatalog.entries().stream()
                .map(CreativeCatalogEntry::createStack)
                .filter(stack -> stack.getType() == ItemType.POTION)
                .toList();

        assertTrue(potions.stream().anyMatch(stack -> stack.getPotionData().type() == PotionType.WATER
                && !stack.getPotionData().splash()));
        assertTrue(potions.stream().anyMatch(stack -> stack.getPotionData().type() == PotionType.SWIFTNESS
                && stack.getPotionData().extended()));
        assertTrue(potions.stream().anyMatch(stack -> stack.getPotionData().type() == PotionType.HARMING
                && stack.getPotionData().enhanced()));
        assertTrue(potions.stream().anyMatch(stack -> stack.getPotionData().type() == PotionType.POISON
                && stack.getPotionData().splash()));
    }

    @Test
    @DisplayName("Creative durable items should be count one while stackables are max stacks")
    void stackCountsMatchCreativeRules() {
        for (CreativeCatalogEntry entry : CreativeCatalog.entries()) {
            ItemStack stack = entry.createStack();
            if (stack.getType().isDamageable() || stack.getType().getMaxStackSize() <= 1) {
                assertEquals(1, stack.getCount(), stack.getType().name());
            } else if (stack.getType() != ItemType.POTION) {
                assertEquals(stack.getType().getMaxStackSize(), stack.getCount(), stack.getType().name());
            }
        }
    }
}
