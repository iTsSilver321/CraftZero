package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.crafting.CraftingRecipe;
import com.craftzero.crafting.CraftingRegistry;
import com.craftzero.world.BlockType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Crafting Table Screen with 3x3 grid AND player inventory.
 * Layout matches Minecraft: crafting at top, inventory below.
 * Slot mapping:
 * 0-8: Crafting grid (3x3)
 * 9: Crafting output
 * 10-36: Main inventory (27 slots, 3 rows)
 * 37-45: Hotbar (9 slots)
 */
public class CraftingTableScreen {

    // === MINECRAFT-ACCURATE LAYOUT (crafting.png: 176x166 at 2x scale) ===
    public static final float GUI_SCALE = 2.0f;

    // Texture dimensions
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 166;

    // Slot size in texture pixels
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;

    // Scaled dimensions for screen
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE); // 36 px
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE); // 32 px
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE); // 352 px
    public static final int WINDOW_HEIGHT = (int) (TEX_HEIGHT * GUI_SCALE); // 332 px

    // === SLOT POSITIONS (in texture pixels) ===
    // Crafting grid 3x3: starts at (30, 17)
    public static final int TEX_CRAFT_GRID_X = 30;
    public static final int TEX_CRAFT_GRID_Y = 17;

    // Crafting output: at (124, 35)
    public static final int TEX_CRAFT_OUTPUT_X = 124;
    public static final int TEX_CRAFT_OUTPUT_Y = 35;

    // Main inventory (27 slots): starts at (8, 84)
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;

    // Hotbar (9 slots): starts at (8, 142)
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;

    // Grid dimensions
    public static final int CRAFTING_COLS = 3;
    public static final int CRAFTING_ROWS = 3;
    public static final int INVENTORY_COLS = 9;
    public static final int INVENTORY_ROWS = 3;
    public static final int HOTBAR_ROWS = 1;

    // Legacy constants for compatibility
    public static final int SLOT_SPACING = 0;
    public static final int PADDING = 0;
    public static final int CRAFTING_TO_OUTPUT_GAP = 0;
    public static final int CRAFTING_TO_INVENTORY_GAP = 0;
    public static final int INVENTORY_TO_HOTBAR_GAP = 0;
    public static final int CELL_WIDTH = SLOT_SIZE;
    public static final int CELL_HEIGHT = SLOT_SIZE;
    public static final int CRAFTING_GRID_WIDTH = CRAFTING_COLS * SLOT_SIZE;
    public static final int CRAFTING_GRID_HEIGHT = CRAFTING_ROWS * SLOT_SIZE;
    public static final int INVENTORY_WIDTH = INVENTORY_COLS * SLOT_SIZE;
    public static final int INVENTORY_HEIGHT = INVENTORY_ROWS * SLOT_SIZE;
    public static final int HOTBAR_HEIGHT = SLOT_SIZE;

    private boolean isOpen = false;
    private Inventory inventory;

    // 3x3 crafting grid
    private ItemStack[] craftingGrid = new ItemStack[9];

    // Mouse state
    private int hoveredSlot = -1;

    // Window position
    private int windowX;
    private int windowY;

    // Items to throw when closing
    private List<ItemStack> itemsToThrow = new ArrayList<>();

    // Flag to open inventory after closing
    // Flag to open inventory after closing
    private boolean openInventoryAfterClose = false;

    // Dragging state
    private boolean isRightClickDragging = false;
    private Set<Integer> draggedSlots = new HashSet<>();

    public CraftingTableScreen(Inventory inventory) {
        this.inventory = inventory;
    }

    public void open(int screenWidth, int screenHeight) {
        if (!isOpen) {
            isOpen = true;
            windowX = (screenWidth - WINDOW_WIDTH) / 2;
            windowY = (screenHeight - WINDOW_HEIGHT) / 2;
            Input.setCursorLocked(false);
            hoveredSlot = -1;
        }
    }

    public void close() {
        if (isOpen) {
            isOpen = false;
            Input.setCursorLocked(true);
            hoveredSlot = -1;

            // Drop all items in crafting grid
            for (int i = 0; i < 9; i++) {
                if (craftingGrid[i] != null && !craftingGrid[i].isEmpty()) {
                    itemsToThrow.add(craftingGrid[i]);
                    craftingGrid[i] = null;
                }
            }

            // Drop cursor item too
            if (inventory.getCursorItem() != null) {
                itemsToThrow.add(inventory.getCursorItem());
                inventory.setCursorItem(null);
            }
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean shouldOpenInventoryAfterClose() {
        boolean value = openInventoryAfterClose;
        openInventoryAfterClose = false;
        return value;
    }

    public List<ItemStack> getAndClearItemsToThrow() {
        List<ItemStack> items = new ArrayList<>(itemsToThrow);
        itemsToThrow.clear();
        return items;
    }

    public void update() {
        if (!isOpen)
            return;

        // Handle ESC to close
        if (Input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            close();
            return;
        }

        // Handle E to close (E opens inventory, not crafting table)
        if (Input.isKeyPressed(GLFW_KEY_E)) {
            close();
            return;
        }

        // Get mouse position
        double mx = Input.getMouseX();
        double my = Input.getMouseY();

        hoveredSlot = getSlotAtPosition((int) mx, (int) my);

        // Handle clicks
        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            if (hoveredSlot == -1 && inventory.getCursorItem() != null) {
                itemsToThrow.add(inventory.getCursorItem());
                inventory.setCursorItem(null);
            } else {
                handleClick(hoveredSlot, false);
            }
        }

        // Handle Right-Click (Single & Drag)
        if (Input.isButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            if (!isRightClickDragging) {
                // Just started clicking/dragging
                isRightClickDragging = true;
                draggedSlots.clear();
                handleClick(hoveredSlot, true);
                if (hoveredSlot != -1)
                    draggedSlots.add(hoveredSlot);
            } else {
                // Continuing to drag
                if (hoveredSlot != -1 && !draggedSlots.contains(hoveredSlot)) {
                    handleClick(hoveredSlot, true); // distribute item
                    draggedSlots.add(hoveredSlot);
                }
            }
        } else {
            // Released right click
            isRightClickDragging = false;
            draggedSlots.clear();
        }
    }

    /**
     * Get slot at mouse position using Minecraft-accurate texture coordinates.
     * Returns:
     * 0-8: Crafting grid (3x3)
     * 9: Crafting output
     * 10-36: Main inventory
     * 37-45: Hotbar
     * -1: None
     */
    private int getSlotAtPosition(int mx, int my) {
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH ||
                my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }

        // Convert to texture-space coordinates (divide by scale)
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;

        // Check crafting grid (3x3) - slots 0-8
        if (texX >= TEX_CRAFT_GRID_X && texX < TEX_CRAFT_GRID_X + CRAFTING_COLS * TEX_SLOT_SIZE &&
                texY >= TEX_CRAFT_GRID_Y && texY < TEX_CRAFT_GRID_Y + CRAFTING_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_CRAFT_GRID_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_CRAFT_GRID_Y) / TEX_SLOT_SIZE);
            if (col < CRAFTING_COLS && row < CRAFTING_ROWS) {
                return row * CRAFTING_COLS + col;
            }
        }

        // Check crafting output - slot 9
        if (texX >= TEX_CRAFT_OUTPUT_X && texX < TEX_CRAFT_OUTPUT_X + TEX_SLOT_SIZE &&
                texY >= TEX_CRAFT_OUTPUT_Y && texY < TEX_CRAFT_OUTPUT_Y + TEX_SLOT_SIZE) {
            return 9;
        }

        // Check main inventory (3 rows x 9 cols) - slots 10-36
        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + INVENTORY_COLS * TEX_SLOT_SIZE &&
                texY >= TEX_MAIN_INV_Y && texY < TEX_MAIN_INV_Y + INVENTORY_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_MAIN_INV_Y) / TEX_SLOT_SIZE);
            if (col < INVENTORY_COLS && row < INVENTORY_ROWS) {
                return 10 + row * INVENTORY_COLS + col;
            }
        }

        // Check hotbar (1 row x 9 cols) - slots 37-45
        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + INVENTORY_COLS * TEX_SLOT_SIZE &&
                texY >= TEX_HOTBAR_Y && texY < TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            if (col < INVENTORY_COLS) {
                return 37 + col;
            }
        }

        return -1;
    }

    private void handleClick(int slotIndex, boolean isRightClick) {
        if (slotIndex == -1)
            return;

        ItemStack cursorItem = inventory.getCursorItem();
        ItemStack slotItem = getItemInSlot(slotIndex);

        // --- Handle Shift+Click ---
        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            if (slotIndex == 9) {
                // Shift-Click Output: Move to inventory max possible
                // Loop until ingredients run out or inventory full
                // Safety limiter to prevent infinite loop
                for (int i = 0; i < 64; i++) { // Max stack size
                    ItemStack output = getItemInSlot(9);
                    if (output == null)
                        break;

                    // Try to add to inventory
                    if (inventory.addItem(output)) {
                        consumeCraftingIngredients();
                        // Re-check recipe for next iteration
                        // getItemInSlot(9) does this via findRecipe3x3
                    } else {
                        break; // Inventory full
                    }
                }
            } else if (slotIndex >= 10 && slotIndex <= 45) {
                // Quick Move Inventory (Hotbar <-> Main)
                // Logic: 10-36 (Main) -> 37-45 (Hotbar) and vice versa
                // Implementing simplified logic: just try to move within inventory
                // For now, let's keep it simple or implement if critical.
                // User asked for "add shift + click functionality to the crafting table too"
                // which usually implies handling the OUTPUT slot primarily, but let's see.
                // Since Inventory class handles add, we can try to remove from slot and add to
                // inventory?
                // But inventory.add() auto-merges. We need to be careful not to add to SAME
                // slot.

                // For now, focusing on Output Shift-Click as requested specifically for
                // crafting convenience.
                // Inventory shift-click is defined in InventoryScreen typically.
            }
            return;
        }

        // Handle crafting output slot (slot 9)
        if (slotIndex == 9) {
            if (slotItem == null)
                return;

            if (cursorItem == null) {
                // Pick up result
                inventory.setCursorItem(slotItem);
                consumeCraftingIngredients();
            } else if (cursorItem.getType() == slotItem.getType()) {
                // Stack result onto cursor
                if (cursorItem.getCount() + slotItem.getCount() <= cursorItem.getMaxStackSize()) {
                    cursorItem.add(slotItem.getCount());
                    consumeCraftingIngredients();
                }
            }
            return;
        }

        // Regular slot interaction (Grid/Inventory)
        if (isRightClick) {
            if (cursorItem == null) {
                // Pick up half
                if (slotItem != null && !slotItem.isEmpty()) {
                    int halfCount = (slotItem.getCount() + 1) / 2;
                    ItemStack taken = new ItemStack(slotItem.getType(), halfCount);
                    slotItem.remove(halfCount);
                    if (slotItem.isEmpty()) {
                        setItemInSlot(slotIndex, null);
                    }
                    inventory.setCursorItem(taken);
                }
            } else {
                // Place one item
                // "The item is subtracted only when it reaches an empty slot" - User Request
                // for Drag
                // But specifically for drag. For single click, it should stack too.

                if (slotItem == null) {
                    setItemInSlot(slotIndex, new ItemStack(cursorItem.getType(), 1));
                    cursorItem.remove(1);
                    if (cursorItem.isEmpty()) {
                        inventory.setCursorItem(null);
                    }
                } else if (slotItem.getType() == cursorItem.getType()) {
                    int space = slotItem.getMaxStackSize() - slotItem.getCount();
                    if (space > 0) {
                        slotItem.add(1);
                        cursorItem.remove(1);
                        if (cursorItem.isEmpty()) {
                            inventory.setCursorItem(null);
                        }
                    }
                }
            }
        } else {
            // Left click - swap or merge
            if (cursorItem == null) {
                if (slotItem != null && !slotItem.isEmpty()) {
                    inventory.setCursorItem(slotItem);
                    setItemInSlot(slotIndex, null);
                }
            } else {
                if (slotItem == null) {
                    setItemInSlot(slotIndex, cursorItem);
                    inventory.setCursorItem(null);
                } else if (slotItem.getType() == cursorItem.getType()) {
                    int space = slotItem.getMaxStackSize() - slotItem.getCount();
                    int toAdd = Math.min(space, cursorItem.getCount());
                    slotItem.add(toAdd);
                    cursorItem.remove(toAdd);
                    if (cursorItem.isEmpty()) {
                        inventory.setCursorItem(null);
                    }
                } else {
                    // Swap
                    setItemInSlot(slotIndex, cursorItem);
                    inventory.setCursorItem(slotItem);
                }
            }
        }
    }

    private ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 9) {
            return craftingGrid[slotIndex];
        } else if (slotIndex == 9) {
            CraftingRecipe recipe = CraftingRegistry.findRecipe3x3(getCraftingPattern());
            return recipe != null ? recipe.getOutput() : null;
        } else if (slotIndex >= 10 && slotIndex <= 36) {
            // Main inventory (10-36 maps to mainInventory 0-26)
            return inventory.getMainInventory()[slotIndex - 10];
        } else if (slotIndex >= 37 && slotIndex <= 45) {
            // Hotbar (37-45 maps to hotbar 0-8)
            return inventory.getHotbar()[slotIndex - 37];
        }
        return null;
    }

    private void setItemInSlot(int slotIndex, ItemStack item) {
        if (slotIndex >= 0 && slotIndex < 9) {
            craftingGrid[slotIndex] = item;
        } else if (slotIndex >= 10 && slotIndex <= 36) {
            inventory.getMainInventory()[slotIndex - 10] = item;
        } else if (slotIndex >= 37 && slotIndex <= 45) {
            inventory.getHotbar()[slotIndex - 37] = item;
        }
    }

    private BlockType[] getCraftingPattern() {
        BlockType[] pattern = new BlockType[9];
        for (int i = 0; i < 9; i++) {
            pattern[i] = (craftingGrid[i] != null && !craftingGrid[i].isEmpty())
                    ? craftingGrid[i].getType()
                    : null;
        }
        return pattern;
    }

    private void consumeCraftingIngredients() {
        for (int i = 0; i < 9; i++) {
            if (craftingGrid[i] != null && !craftingGrid[i].isEmpty()) {
                craftingGrid[i].remove(1);
                if (craftingGrid[i].isEmpty()) {
                    craftingGrid[i] = null;
                }
            }
        }
    }

    // Getters for renderer
    public int getWindowX() {
        return windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public int getHoveredSlot() {
        return hoveredSlot;
    }

    public ItemStack[] getCraftingGrid() {
        return craftingGrid;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
