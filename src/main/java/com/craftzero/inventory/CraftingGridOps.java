package com.craftzero.inventory;

import com.craftzero.crafting.CraftingRecipe;

import java.util.List;
import java.util.function.Supplier;

/**
 * Shared crafting-grid result and ingredient handling for the player inventory
 * and crafting table screens.
 */
public final class CraftingGridOps {
    private static final int QUICK_MOVE_SAFETY_LIMIT = 64;

    private CraftingGridOps() {
    }

    public static boolean takeOutputToCursor(Inventory inventory, ItemStack[] grid, CraftingRecipe recipe,
            List<ItemStack> overflowItems) {
        if (inventory == null || grid == null || recipe == null) {
            return false;
        }

        ItemStack output = recipe.getOutput();
        ItemStack cursor = inventory.getCursorItem();
        if (ItemStackOps.isEmpty(cursor)) {
            inventory.setCursorItem(output);
        } else if (ItemStackOps.canMerge(cursor, output)
                && cursor.getCount() + output.getCount() <= cursor.getMaxStackSize()) {
            cursor.add(output.getCount());
        } else {
            return false;
        }

        consumeIngredients(inventory, grid, recipe, overflowItems);
        inventory.notifyCrafted(output);
        return true;
    }

    public static int quickMoveOutputToInventory(Inventory inventory, ItemStack[] grid,
            Supplier<CraftingRecipe> recipeFinder, List<ItemStack> overflowItems) {
        if (inventory == null || grid == null || recipeFinder == null) {
            return 0;
        }

        int crafted = 0;
        for (int i = 0; i < QUICK_MOVE_SAFETY_LIMIT; i++) {
            CraftingRecipe recipe = recipeFinder.get();
            if (recipe == null) {
                break;
            }

            ItemStack output = recipe.getOutput();
            ItemStack craftedOutput = output.copy();
            if (!inventory.canAddItem(output)) {
                break;
            }
            if (!inventory.addItem(output)) {
                break;
            }

            consumeIngredients(inventory, grid, recipe, overflowItems);
            inventory.notifyCrafted(craftedOutput);
            crafted++;
        }
        return crafted;
    }

    public static void consumeIngredients(Inventory inventory, ItemStack[] grid, CraftingRecipe recipe,
            List<ItemStack> overflowItems) {
        if (inventory == null || grid == null) {
            return;
        }

        ItemType[] remainingItems = recipe == null ? new ItemType[grid.length]
                : recipe.getRemainingItems(craftingPattern(grid));
        for (int i = 0; i < grid.length; i++) {
            ItemStack stack = grid[i];
            if (ItemStackOps.isEmpty(stack)) {
                continue;
            }

            stack.remove(1);
            if (stack.isEmpty()) {
                grid[i] = null;
            }
            if (remainingItems[i] != null) {
                insertRemainder(inventory, grid, i, new ItemStack(remainingItems[i], 1), overflowItems);
            }
        }
    }

    private static void insertRemainder(Inventory inventory, ItemStack[] grid, int slot, ItemStack remainder,
            List<ItemStack> overflowItems) {
        if (ItemStackOps.isEmpty(remainder)) {
            return;
        }
        if (ItemStackOps.isEmpty(grid[slot])) {
            grid[slot] = remainder;
            return;
        }
        if (!inventory.addItem(remainder) && !ItemStackOps.isEmpty(remainder) && overflowItems != null) {
            overflowItems.add(remainder);
        }
    }

    private static ItemType[] craftingPattern(ItemStack[] grid) {
        ItemType[] pattern = new ItemType[grid.length];
        for (int i = 0; i < grid.length; i++) {
            pattern[i] = !ItemStackOps.isEmpty(grid[i]) ? grid[i].getType() : null;
        }
        return pattern;
    }
}
