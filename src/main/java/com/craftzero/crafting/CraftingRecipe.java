package com.craftzero.crafting;

import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ItemStack;

/**
 * Represents a crafting recipe.
 * Supports both 2x2 (4 slots) and 3x3 (9 slots) grid patterns.
 */
public class CraftingRecipe {

    // Pattern: null = empty slot, ItemType = required item
    // 2x2 Layout: [0][1] 3x3 Layout: [0][1][2]
    // [2][3] [3][4][5]
    // [6][7][8]
    private final ItemType[] pattern;
    private final ItemType outputType;
    private final int outputCount;
    private final boolean shapeless;
    private final int gridSize; // 2 or 3

    /**
     * Create a shaped 2x2 recipe.
     */
    public CraftingRecipe(ItemType[] pattern, ItemType outputType, int outputCount) {
        this.pattern = pattern;
        this.outputType = outputType;
        this.outputCount = outputCount;
        this.shapeless = false;
        this.gridSize = 2;
    }

    /**
     * Create a shapeless recipe (any arrangement works).
     */
    public CraftingRecipe(ItemType[] ingredients, ItemType outputType, int outputCount, boolean shapeless) {
        this.pattern = ingredients;
        this.outputType = outputType;
        this.outputCount = outputCount;
        this.shapeless = shapeless;
        this.gridSize = ingredients.length == 9 ? 3 : 2;
    }

    /**
     * Create a shaped 3x3 recipe.
     */
    public static CraftingRecipe create3x3(ItemType[] pattern, ItemType outputType, int outputCount) {
        return new CraftingRecipe(pattern, outputType, outputCount, false, 3);
    }

    /**
     * Full constructor with all options.
     */
    public CraftingRecipe(ItemType[] pattern, ItemType outputType, int outputCount, boolean shapeless,
            int gridSize) {
        this.pattern = pattern;
        this.outputType = outputType;
        this.outputCount = outputCount;
        this.shapeless = shapeless;
        this.gridSize = gridSize;
    }

    /**
     * Check if this recipe matches the given crafting grid.
     * 
     * @param grid Array of ItemTypes (4 for 2x2, 9 for 3x3)
     */
    public boolean matches(ItemType[] grid) {
        // Grid size must match recipe size
        int expectedSize = gridSize == 3 ? 9 : 4;
        if (grid.length != expectedSize) {
            return false;
        }

        if (shapeless) {
            return matchesShapeless(grid);
        } else {
            return matchesShaped(grid);
        }
    }

    private boolean matchesShaped(ItemType[] grid) {
        // Direct match
        if (matchesPatternAt(grid, pattern)) {
            return true;
        }
        // Try all positions for single-item recipes (2x2 only)
        if (gridSize == 2 && isSingleItemPattern()) {
            for (int i = 0; i < 4; i++) {
                if (grid[i] == pattern[0] && countNonNull(grid) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPatternAt(ItemType[] grid, ItemType[] pat) {
        int size = pat.length;
        if (grid.length != size)
            return false;

        for (int i = 0; i < size; i++) {
            if (pat[i] == null) {
                if (grid[i] != null)
                    return false;
            } else {
                if (grid[i] != pat[i])
                    return false;
            }
        }
        return true;
    }

    private boolean matchesShapeless(ItemType[] grid) {
        // Count required ingredients
        int[] required = new int[ItemType.values().length];
        for (ItemType b : pattern) {
            if (b != null)
                required[b.ordinal()]++;
        }

        // Count provided
        int[] provided = new int[ItemType.values().length];
        for (ItemType b : grid) {
            if (b != null)
                provided[b.ordinal()]++;
        }

        // Must match exactly
        for (int i = 0; i < required.length; i++) {
            if (required[i] != provided[i])
                return false;
        }
        return true;
    }

    private boolean isSingleItemPattern() {
        int count = 0;
        for (ItemType b : pattern) {
            if (b != null)
                count++;
        }
        return count == 1;
    }

    private int countNonNull(ItemType[] arr) {
        int c = 0;
        for (ItemType b : arr) {
            if (b != null)
                c++;
        }
        return c;
    }

    public ItemType getOutputType() {
        return outputType;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public int getGridSize() {
        return gridSize;
    }

    public ItemStack getOutput() {
        return new ItemStack(outputType, outputCount);
    }
}
