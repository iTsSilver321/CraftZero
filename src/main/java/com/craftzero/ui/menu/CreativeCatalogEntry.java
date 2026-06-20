package com.craftzero.ui.menu;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

public record CreativeCatalogEntry(ItemStack stack) {
    public CreativeCatalogEntry {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("creative catalog stack cannot be empty");
        }
    }

    public ItemType type() {
        return stack.getType();
    }

    public ItemStack createStack() {
        return stack.copy();
    }
}
