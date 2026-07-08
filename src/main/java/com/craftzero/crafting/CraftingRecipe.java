package com.craftzero.crafting;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

import java.util.Arrays;
import java.util.List;

/**
 * Represents one Release 1.0 crafting recipe.
 */
public class CraftingRecipe {
    public static final class Ingredient {
        private final ItemType[] acceptedTypes;

        private Ingredient(ItemType[] acceptedTypes) {
            if (acceptedTypes == null || acceptedTypes.length == 0) {
                throw new IllegalArgumentException("ingredient must accept at least one item type");
            }
            this.acceptedTypes = acceptedTypes.clone();
        }

        public static Ingredient of(ItemType type) {
            return new Ingredient(new ItemType[] { type });
        }

        public static Ingredient anyOf(ItemType... types) {
            return new Ingredient(types);
        }

        public boolean matches(ItemType type) {
            if (type == null) {
                return false;
            }
            for (ItemType accepted : acceptedTypes) {
                if (accepted == type) {
                    return true;
                }
            }
            return false;
        }

        List<ItemType> acceptedTypes() {
            return List.of(acceptedTypes);
        }
    }

    private final Ingredient[] pattern;
    private final ItemType outputType;
    private final int outputCount;
    private final boolean shapeless;
    private final int width;
    private final int height;
    private final ItemStack outputStack;

    /**
     * Compatibility constructor for a shaped 2x2 recipe.
     */
    public CraftingRecipe(ItemType[] pattern, ItemType outputType, int outputCount) {
        this(toIngredients(pattern), outputType, outputCount, false, 2, 2);
    }

    /**
     * Compatibility constructor for shapeless recipes.
     */
    public CraftingRecipe(ItemType[] ingredients, ItemType outputType, int outputCount, boolean shapeless) {
        this(toIngredients(ingredients), outputType, outputCount, shapeless,
                ingredients.length == 9 ? 3 : 2,
                ingredients.length == 9 ? 3 : 2);
    }

    /**
     * Compatibility constructor with an explicit grid size.
     */
    public CraftingRecipe(ItemType[] pattern, ItemType outputType, int outputCount, boolean shapeless, int gridSize) {
        this(toIngredients(pattern), outputType, outputCount, shapeless, gridSize, gridSize);
    }

    private CraftingRecipe(Ingredient[] pattern, ItemType outputType, int outputCount, boolean shapeless,
            int width, int height) {
        this(pattern, outputType, outputCount, shapeless, width, height, null);
    }

    private CraftingRecipe(Ingredient[] pattern, ItemType outputType, int outputCount, boolean shapeless,
            int width, int height, ItemStack outputStack) {
        this.pattern = pattern;
        this.outputType = outputType;
        this.outputCount = outputCount;
        this.shapeless = shapeless;
        this.width = width;
        this.height = height;
        this.outputStack = outputStack == null ? null : outputStack.copy();
    }

    public static CraftingRecipe shaped(int width, int height, Ingredient[] pattern, ItemType outputType,
            int outputCount) {
        if (width <= 0 || height <= 0 || pattern.length != width * height) {
            throw new IllegalArgumentException("invalid shaped recipe dimensions");
        }
        return new CraftingRecipe(pattern.clone(), outputType, outputCount, false, width, height);
    }

    public static CraftingRecipe shapeless(Ingredient[] ingredients, ItemType outputType, int outputCount) {
        return new CraftingRecipe(Arrays.stream(ingredients)
                .filter(ingredient -> ingredient != null)
                .toArray(Ingredient[]::new), outputType, outputCount, true, 0, 0);
    }

    public static CraftingRecipe shapelessWithOutputStack(Ingredient[] ingredients, ItemStack output) {
        if (output == null || output.isEmpty()) {
            throw new IllegalArgumentException("dynamic recipe output must not be empty");
        }
        return new CraftingRecipe(Arrays.stream(ingredients)
                .filter(ingredient -> ingredient != null)
                .toArray(Ingredient[]::new), output.getType(), output.getCount(), true, 0, 0, output);
    }

    /**
     * Compatibility helper for old shaped 3x3 registrations.
     */
    public static CraftingRecipe create3x3(ItemType[] pattern, ItemType outputType, int outputCount) {
        return new CraftingRecipe(toIngredients(pattern), outputType, outputCount, false, 3, 3);
    }

    public boolean matches(ItemType[] grid) {
        int gridSize = gridSize(grid);
        if (gridSize == 0) {
            return false;
        }
        return shapeless ? matchesShapeless(grid) : matchesShaped(grid, gridSize);
    }

    private boolean matchesShaped(ItemType[] grid, int gridSize) {
        if (width > gridSize || height > gridSize) {
            return false;
        }
        for (int y = 0; y <= gridSize - height; y++) {
            for (int x = 0; x <= gridSize - width; x++) {
                if (matchesAt(grid, gridSize, x, y, false) || matchesAt(grid, gridSize, x, y, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAt(ItemType[] grid, int gridSize, int offsetX, int offsetY, boolean mirrored) {
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int recipeX = x - offsetX;
                int recipeY = y - offsetY;
                Ingredient expected = null;
                if (recipeX >= 0 && recipeY >= 0 && recipeX < width && recipeY < height) {
                    int sourceX = mirrored ? width - recipeX - 1 : recipeX;
                    expected = pattern[sourceX + recipeY * width];
                }

                ItemType actual = grid[x + y * gridSize];
                if (expected == null) {
                    if (actual != null) {
                        return false;
                    }
                } else if (!expected.matches(actual)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchesShapeless(ItemType[] grid) {
        boolean[] used = new boolean[pattern.length];
        int providedCount = 0;

        for (ItemType actual : grid) {
            if (actual == null) {
                continue;
            }
            providedCount++;
            boolean matched = false;
            for (int i = 0; i < pattern.length; i++) {
                if (!used[i] && pattern[i].matches(actual)) {
                    used[i] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return providedCount == pattern.length;
    }

    public ItemType[] getRemainingItems(ItemType[] grid) {
        ItemType[] remaining = new ItemType[grid.length];
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null) {
                remaining[i] = grid[i].getCraftingRemainder();
            }
        }
        return remaining;
    }

    public ItemType getOutputType() {
        return outputType;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public int getGridSize() {
        return Math.max(width, height) <= 2 ? 2 : 3;
    }

    public int getIngredientCount() {
        int count = 0;
        for (Ingredient ingredient : pattern) {
            if (ingredient != null) {
                count++;
            }
        }
        return count;
    }

    public int getRecipeSize() {
        return shapeless ? pattern.length : width * height;
    }

    public boolean isShapeless() {
        return shapeless;
    }

    public ItemStack getOutput() {
        if (outputStack != null) {
            return outputStack.copy();
        }
        return new ItemStack(outputType, outputCount);
    }

    private static Ingredient[] toIngredients(ItemType[] itemTypes) {
        Ingredient[] ingredients = new Ingredient[itemTypes.length];
        for (int i = 0; i < itemTypes.length; i++) {
            ingredients[i] = itemTypes[i] == null ? null : Ingredient.of(itemTypes[i]);
        }
        return ingredients;
    }

    private static int gridSize(ItemType[] grid) {
        return switch (grid.length) {
            case 4 -> 2;
            case 9 -> 3;
            default -> 0;
        };
    }
}
