package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.crafting.CraftingRecipe;
import com.craftzero.crafting.CraftingRegistry;
import com.craftzero.world.BlockType;

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Manages the Inventory Screen state and interaction logic.
 * Handles opening/closing, slot hover detection, and click events.
 */
public class InventoryScreen {

    // Layout constants (in screen pixels, will be scaled)
    public static final int SLOT_SIZE = 36; // Visual slot size
    public static final int SLOT_SPACING = 4; // Gap between slots
    public static final int PADDING = 14; // Window edge padding

    // Grid layout: 9 columns, 3 main rows + 1 hotbar row
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;
    public static final int HOTBAR_ROWS = 1;
    public static final int HOTBAR_GAP = 8; // Extra gap above hotbar

    // Crafting area constants
    public static final int CRAFTING_COLS = 2;
    public static final int CRAFTING_ROWS = 2;
    public static final int CRAFTING_GAP = 20; // Gap between inventory and crafting
    public static final int CRAFTING_ARROW_WIDTH = 30; // Arrow between grid and output

    // Calculated window dimensions (extended for crafting area)
    public static final int INVENTORY_WIDTH = COLS * SLOT_SIZE + (COLS - 1) * SLOT_SPACING;
    public static final int CRAFTING_WIDTH = CRAFTING_COLS * SLOT_SIZE + (CRAFTING_COLS - 1) * SLOT_SPACING
            + CRAFTING_ARROW_WIDTH + SLOT_SIZE; // grid + arrow + output
    public static final int WINDOW_WIDTH = PADDING * 2 + INVENTORY_WIDTH + CRAFTING_GAP + CRAFTING_WIDTH;
    public static final int WINDOW_HEIGHT = PADDING * 2
            + (MAIN_ROWS + HOTBAR_ROWS) * SLOT_SIZE
            + (MAIN_ROWS + HOTBAR_ROWS - 1) * SLOT_SPACING
            + HOTBAR_GAP;

    private boolean isOpen = false;
    private Inventory inventory;

    // Mouse state
    private int hoveredSlot = -1; // -1 = no slot hovered

    // Dragging state
    private boolean isRightClickDragging = false;
    private Set<Integer> draggedSlots = new HashSet<>();

    // Window position (centered, calculated on open)
    private int windowX;
    private int windowY;
    private int screenWidth;
    private int screenHeight;

    public InventoryScreen(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Toggle the inventory screen open/closed.
     */
    public void toggle(int screenWidth, int screenHeight) {
        isOpen = !isOpen;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        if (isOpen) {
            // Center the window
            windowX = (screenWidth - WINDOW_WIDTH) / 2;
            windowY = (screenHeight - WINDOW_HEIGHT) / 2;
            Input.setCursorLocked(false);
        } else {
            Input.setCursorLocked(true);
            hoveredSlot = -1;
        }
    }

    public void open(int screenWidth, int screenHeight) {
        if (!isOpen) {
            toggle(screenWidth, screenHeight);
        }
    }

    public void close() {
        if (isOpen) {
            isOpen = false;
            Input.setCursorLocked(true);
            hoveredSlot = -1;

            // If holding an item, drop it back into inventory (or drop on ground - future)
            if (inventory.getCursorItem() != null) {
                // For now, just clear it (would be dropped in full game)
                inventory.setCursorItem(null);
            }
        }
    }

    /**
     * Update hover state and handle clicks.
     * Called every frame when inventory is open.
     */
    public void update() {
        if (!isOpen)
            return;

        // Get mouse position
        double mx = Input.getMouseX();
        double my = Input.getMouseY();

        // Determine which slot (if any) the mouse is over
        hoveredSlot = getSlotAtPosition((int) mx, (int) my);

        // Handle clicks
        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            if (hoveredSlot == -1 && inventory.getCursorItem() != null) {
                // Click outside with cursor item - throw it
                itemToThrow = inventory.getCursorItem();
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

    // Item to be thrown (set when clicking outside with cursor item)
    private ItemStack itemToThrow = null;

    /**
     * Get and clear the item to throw (for Main to handle).
     */
    public ItemStack getAndClearItemToThrow() {
        ItemStack item = itemToThrow;
        itemToThrow = null;
        return item;
    }

    /**
     * Determine which slot the mouse is over.
     * Returns slot index 0-35 (0-26 = main, 27-35 = hotbar), or -1 if none.
     * 
     * Layout:
     * - Slots 0-26: Main inventory (3 rows x 9 cols)
     * - Slots 27-35: Hotbar (1 row x 9 cols)
     * - Slots 36-39: Crafting grid (2x2)
     * - Slot 40: Crafting output
     */
    private int getSlotAtPosition(int mx, int my) {
        // Check if inside window bounds
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH ||
                my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }

        // Local coordinates within window
        int localX = mx - windowX - PADDING;
        int localY = my - windowY - PADDING;

        if (localX < 0 || localY < 0)
            return -1;

        int cellWidth = SLOT_SIZE + SLOT_SPACING;
        int cellHeight = SLOT_SIZE + SLOT_SPACING;

        // Check crafting area first (right side)
        int craftingStartX = INVENTORY_WIDTH + CRAFTING_GAP;
        if (localX >= craftingStartX) {
            int craftLocalX = localX - craftingStartX;

            // Crafting grid (2x2)
            int gridWidth = CRAFTING_COLS * cellWidth;
            if (craftLocalX < gridWidth && localY < CRAFTING_ROWS * cellHeight) {
                int col = craftLocalX / cellWidth;
                int row = localY / cellHeight;
                int slotLocalX = craftLocalX % cellWidth;
                int slotLocalY = localY % cellHeight;

                if (col < CRAFTING_COLS && row < CRAFTING_ROWS
                        && slotLocalX < SLOT_SIZE && slotLocalY < SLOT_SIZE) {
                    return 36 + row * CRAFTING_COLS + col; // Slots 36-39
                }
            }

            // Crafting output slot (after arrow)
            int outputX = gridWidth + CRAFTING_ARROW_WIDTH;
            int outputY = (CRAFTING_ROWS * cellHeight - SLOT_SIZE) / 2; // Centered vertically
            if (craftLocalX >= outputX && craftLocalX < outputX + SLOT_SIZE
                    && localY >= outputY && localY < outputY + SLOT_SIZE) {
                return 40; // Output slot
            }

            return -1; // In crafting area but not on a slot
        }

        // Inventory area (left side)
        int col = localX / cellWidth;
        int slotLocalX = localX % cellWidth;

        // Check if actually on the slot (not in spacing)
        if (col >= COLS || slotLocalX >= SLOT_SIZE)
            return -1;

        // Determine row, accounting for hotbar gap
        int mainGridHeight = MAIN_ROWS * cellHeight;
        int hotbarStartY = mainGridHeight + HOTBAR_GAP;

        int slotLocalY;

        if (localY < mainGridHeight) {
            // Main inventory area
            int row = localY / cellHeight;
            slotLocalY = localY % cellHeight;
            if (row >= MAIN_ROWS || slotLocalY >= SLOT_SIZE)
                return -1;
            return row * COLS + col; // Index 0-26
        } else if (localY >= hotbarStartY && localY < hotbarStartY + cellHeight) {
            // Hotbar area
            slotLocalY = (localY - hotbarStartY) % cellHeight;
            if (slotLocalY >= SLOT_SIZE)
                return -1;
            return 27 + col; // Index 27-35
        }

        return -1;
    }

    /**
     * Handle a click on a slot.
     * 
     * @param slotIndex    The slot clicked (-1 if outside)
     * @param isRightClick True for right-click, false for left-click
     */
    private void handleClick(int slotIndex, boolean isRightClick) {
        if (slotIndex == -1)
            return;

        ItemStack cursorItem = inventory.getCursorItem();
        ItemStack slotItem = getItemInSlot(slotIndex);

        // Special handling for crafting output slot (40)
        if (slotIndex == 40) {
            // SHIFT+CLICK Output
            if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
                for (int i = 0; i < 64; i++) { // Max stack size safety loop
                    ItemStack output = getItemInSlot(40);
                    if (output == null)
                        break;

                    // Try to add to inventory (main or hotbar)
                    if (inventory.addItem(output)) {
                        inventory.consumeCraftingIngredients();
                    } else {
                        break; // Inventory full
                    }
                }
                return;
            }

            if (slotItem == null)
                return;

            // Regular click on output
            if (cursorItem == null) {
                // Pick up result
                inventory.setCursorItem(slotItem);
                inventory.consumeCraftingIngredients();
            } else if (cursorItem.getType() == slotItem.getType()) {
                // Stack result onto cursor
                if (cursorItem.getCount() + slotItem.getCount() <= cursorItem.getMaxStackSize()) {
                    cursorItem.add(slotItem.getCount());
                    inventory.consumeCraftingIngredients();
                }
            }
            return;
        }

        // Shift-click: Quick-move between hotbar and main inventory
        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
            if (slotItem != null && !slotItem.isEmpty()) {
                boolean isHotbarSlot = slotIndex >= 27;
                ItemStack[] targetSlots = isHotbarSlot ? inventory.getMainInventory() : inventory.getHotbar();

                // Try to stack with existing items first
                for (int i = 0; i < targetSlots.length && !slotItem.isEmpty(); i++) {
                    ItemStack target = targetSlots[i];
                    if (target != null && target.getType() == slotItem.getType()) {
                        int space = target.getMaxStackSize() - target.getCount();
                        int toMove = Math.min(space, slotItem.getCount());
                        if (toMove > 0) {
                            target.add(toMove);
                            slotItem.remove(toMove);
                        }
                    }
                }

                // Try to find an empty slot
                if (!slotItem.isEmpty()) {
                    for (int i = 0; i < targetSlots.length; i++) {
                        if (targetSlots[i] == null || targetSlots[i].isEmpty()) {
                            targetSlots[i] = slotItem;
                            setItemInSlot(slotIndex, null);
                            break;
                        }
                    }
                }

                // Clean up if stack is now empty
                if (slotItem != null && slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                }
            }
            return; // Don't do regular click behavior
        }

        if (isRightClick) {
            // Right-click logic
            if (cursorItem == null && slotItem != null) {
                // Pick up half the stack
                int half = (slotItem.getCount() + 1) / 2;
                ItemStack picked = new ItemStack(slotItem.getType(), half);
                slotItem.remove(half);
                if (slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                }
                inventory.setCursorItem(picked);
            } else if (cursorItem != null && slotItem == null) {
                // Place one item
                setItemInSlot(slotIndex, new ItemStack(cursorItem.getType(), 1));
                cursorItem.remove(1);
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (cursorItem != null && slotItem != null
                    && cursorItem.getType() == slotItem.getType()) {
                // Add one to stack
                if (slotItem.getCount() < slotItem.getMaxStackSize()) {
                    slotItem.add(1);
                    cursorItem.remove(1);
                    if (cursorItem.isEmpty()) {
                        inventory.setCursorItem(null);
                    }
                }
            }
        } else {
            // Left-click logic
            if (cursorItem == null && slotItem != null) {
                // Pick up entire stack
                inventory.setCursorItem(slotItem);
                setItemInSlot(slotIndex, null);
            } else if (cursorItem != null && slotItem == null) {
                // Place entire stack
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(null);
            } else if (cursorItem != null && slotItem != null) {
                if (cursorItem.getType() == slotItem.getType()) {
                    // Merge stacks
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

    /**
     * Get item from combined slot index.
     */
    private ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex >= 36 && slotIndex <= 39) {
            // Crafting grid
            return inventory.getCraftingGrid()[slotIndex - 36];
        } else if (slotIndex == 40) {
            // Crafting output - return recipe result
            CraftingRecipe recipe = CraftingRegistry.findRecipe(inventory.getCraftingPattern());
            return recipe != null ? recipe.getOutput() : null;
        } else if (slotIndex >= 27) {
            return inventory.getHotbar()[slotIndex - 27];
        } else {
            return inventory.getMainInventory()[slotIndex];
        }
    }

    /**
     * Set item in combined slot index.
     */
    private void setItemInSlot(int slotIndex, ItemStack item) {
        if (slotIndex >= 36 && slotIndex <= 39) {
            // Crafting grid
            inventory.getCraftingGrid()[slotIndex - 36] = item;
        } else if (slotIndex == 40) {
            // Output slot - cannot directly set
            return;
        } else if (slotIndex >= 27) {
            inventory.getHotbar()[slotIndex - 27] = item;
        } else {
            inventory.getMainInventory()[slotIndex] = item;
        }
    }

    // Getters for renderer
    public boolean isOpen() {
        return isOpen;
    }

    public int getHoveredSlot() {
        return hoveredSlot;
    }

    public int getWindowX() {
        return windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
