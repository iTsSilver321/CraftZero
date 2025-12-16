package com.craftzero.inventory;

import com.craftzero.world.BlockType;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int CRAFTING_SIZE = 4; // 2x2 grid

    private ItemStack[] hotbar;
    private ItemStack[] mainInventory;
    private ItemStack[] craftingGrid; // 2x2 crafting grid
    private int selectedSlot;

    // Item currently being dragged by the mouse cursor
    private ItemStack cursorItem;

    public Inventory() {
        this.hotbar = new ItemStack[HOTBAR_SIZE];
        this.mainInventory = new ItemStack[MAIN_SIZE];
        this.craftingGrid = new ItemStack[CRAFTING_SIZE];
        this.selectedSlot = 0;
        this.cursorItem = null;
    }

    public ItemStack getSelectedSocket() {
        return hotbar[selectedSlot];
    }

    public ItemStack getItemInHand() {
        return hotbar[selectedSlot];
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            this.selectedSlot = slot;
        }
    }

    public void scroll(int direction) {
        selectedSlot -= direction; // Scroll up (neg) -> next slot
        if (selectedSlot < 0)
            selectedSlot = HOTBAR_SIZE - 1;
        if (selectedSlot >= HOTBAR_SIZE)
            selectedSlot = 0;
    }

    public ItemStack[] getHotbar() {
        return hotbar;
    }

    public ItemStack[] getMainInventory() {
        return mainInventory;
    }

    public ItemStack[] getCraftingGrid() {
        return craftingGrid;
    }

    /**
     * Get the crafting grid as BlockType array for recipe matching.
     */
    public BlockType[] getCraftingPattern() {
        BlockType[] pattern = new BlockType[4];
        for (int i = 0; i < 4; i++) {
            pattern[i] = (craftingGrid[i] != null && !craftingGrid[i].isEmpty())
                    ? craftingGrid[i].getType()
                    : null;
        }
        return pattern;
    }

    /**
     * Clear the crafting grid (when closing inventory or taking output).
     */
    public void clearCraftingGrid() {
        for (int i = 0; i < CRAFTING_SIZE; i++) {
            craftingGrid[i] = null;
        }
    }

    /**
     * Consume one item from each crafting slot (after crafting).
     */
    public void consumeCraftingIngredients() {
        for (int i = 0; i < CRAFTING_SIZE; i++) {
            if (craftingGrid[i] != null && !craftingGrid[i].isEmpty()) {
                craftingGrid[i].remove(1);
                if (craftingGrid[i].isEmpty()) {
                    craftingGrid[i] = null;
                }
            }
        }
    }

    public ItemStack getCursorItem() {
        return cursorItem;
    }

    public void setCursorItem(ItemStack item) {
        this.cursorItem = item;
    }

    /**
     * Add an item to the inventory.
     * Tries to merge with existing stacks first, then places in empty slots.
     * 
     * @return true if at least one item was added (or fully added? Logic usually
     *         implies fully added for success)
     *         For Shift-Click, we want to return true if *any* amount was moved, or
     *         if *all* was moved?
     *         Usually if it returns true, the source item is cleared.
     *         Let's make it return true if the input item is EMPTY (fully
     *         consumed).
     */
    public boolean addItem(ItemStack item) {
        if (item == null || item.isEmpty())
            return true;

        // 1. Try to merge with existing stacks in Hotbar
        if (mergeToArrays(hotbar, item))
            return true;
        // 2. Try to merge with existing stacks in Main Inventory
        if (mergeToArrays(mainInventory, item))
            return true;

        // 3. Try to add to empty slots in Hotbar
        if (addToEmptySlots(hotbar, item))
            return true;
        // 4. Try to add to empty slots in Main Inventory
        if (addToEmptySlots(mainInventory, item))
            return true;

        return item.isEmpty();
    }

    private boolean mergeToArrays(ItemStack[] slots, ItemStack item) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].getType() == item.getType()) {
                int space = slots[i].getMaxStackSize() - slots[i].getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, item.getCount());
                    slots[i].add(toAdd);
                    item.remove(toAdd);
                    if (item.isEmpty())
                        return true;
                }
            }
        }
        return false;
    }

    private boolean addToEmptySlots(ItemStack[] slots, ItemStack item) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = new ItemStack(item.getType(), item.getCount());
                item.remove(item.getCount()); // Fully added
                return true;
            }
        }
        return false;
    }

}
