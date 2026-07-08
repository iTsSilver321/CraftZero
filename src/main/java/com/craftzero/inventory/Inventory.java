package com.craftzero.inventory;

import com.craftzero.crafting.CraftingRecipe;

import java.util.function.Consumer;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int CRAFTING_SIZE = 4; // 2x2 grid

    private ItemStack[] hotbar;
    private ItemStack[] mainInventory;
    private ItemStack[] craftingGrid; // 2x2 crafting grid
    private ItemStack[] armor;
    private int selectedSlot;

    // Item currently being dragged by the mouse cursor
    private ItemStack cursorItem;
    private Consumer<ItemStack> itemAddedListener = stack -> {
    };
    private Consumer<ItemStack> craftedItemListener = stack -> {
    };

    public Inventory() {
        this.hotbar = new ItemStack[HOTBAR_SIZE];
        this.mainInventory = new ItemStack[MAIN_SIZE];
        this.craftingGrid = new ItemStack[CRAFTING_SIZE];
        this.armor = new ItemStack[4];
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

    public ItemStack[] getArmor() {
        return armor;
    }

    /**
     * Get the crafting grid as BlockType array for recipe matching.
     */
    public ItemType[] getCraftingPattern() {
        ItemType[] pattern = new ItemType[4];
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
        consumeCraftingIngredients(null);
    }

    public void consumeCraftingIngredients(CraftingRecipe recipe) {
        CraftingGridOps.consumeIngredients(this, craftingGrid, recipe, null);
    }

    public ItemStack getCursorItem() {
        return cursorItem;
    }

    public void setCursorItem(ItemStack item) {
        this.cursorItem = item;
    }

    public void setItemAddedListener(Consumer<ItemStack> listener) {
        this.itemAddedListener = listener == null ? stack -> {
        } : listener;
    }

    public void setCraftedItemListener(Consumer<ItemStack> listener) {
        this.craftedItemListener = listener == null ? stack -> {
        } : listener;
    }

    void notifyCrafted(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            craftedItemListener.accept(stack.copy());
        }
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

        ItemStack original = item.copy();
        int originalCount = item.getCount();
        ItemStackOps.mergeIntoSlots(SlotAccess.of(hotbar), item);
        if (!item.isEmpty()) {
            ItemStackOps.mergeIntoSlots(SlotAccess.of(mainInventory), item);
        }
        if (!item.isEmpty()) {
            ItemStackOps.placeIntoEmptySlots(SlotAccess.of(hotbar), item);
        }
        if (!item.isEmpty()) {
            ItemStackOps.placeIntoEmptySlots(SlotAccess.of(mainInventory), item);
        }
        int added = originalCount - item.getCount();
        if (added > 0) {
            original.setCount(added);
            itemAddedListener.accept(original);
        }
        return item.isEmpty();
    }

    public boolean canAddItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return true;
        }
        SlotAccess playerSlots = SlotAccess.concat(SlotAccess.of(hotbar), SlotAccess.of(mainInventory));
        return ItemStackOps.canFullyMoveInto(playerSlots, item);
    }

    public int countAddable(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }
        SlotAccess playerSlots = SlotAccess.concat(SlotAccess.of(hotbar), SlotAccess.of(mainInventory));
        return ItemStackOps.countAddable(playerSlots, item);
    }

    /**
     * Clear all items from the inventory (hotbar, main, and crafting).
     */
    public void clearInventory() {
        for (int i = 0; i < hotbar.length; i++) {
            hotbar[i] = null;
        }
        for (int i = 0; i < mainInventory.length; i++) {
            mainInventory[i] = null;
        }
        for (int i = 0; i < armor.length; i++) {
            armor[i] = null;
        }
        clearCraftingGrid();
    }
}
