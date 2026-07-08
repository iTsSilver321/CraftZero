package com.craftzero.ui;

import com.craftzero.inventory.ItemStack;
import org.joml.Vector3i;

public record CraftAction(int gridSize, boolean quickMove, int crafts, ItemStack[] grid,
        ItemStack cursorBefore, Vector3i tablePos) {
    public CraftAction {
        gridSize = gridSize == 3 ? 3 : 2;
        crafts = Math.max(0, crafts);
        grid = copyGrid(grid, gridSize == 3 ? 9 : 4);
        cursorBefore = cursorBefore == null ? null : cursorBefore.copy();
        tablePos = tablePos == null ? null : new Vector3i(tablePos);
    }

    private static ItemStack[] copyGrid(ItemStack[] source, int size) {
        ItemStack[] copy = new ItemStack[size];
        if (source == null) {
            return copy;
        }
        for (int i = 0; i < copy.length && i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].copy();
        }
        return copy;
    }
}
