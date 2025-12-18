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

    // === MINECRAFT-ACCURATE LAYOUT (Texture: 176x166 at 2x scale) ===
    public static final float GUI_SCALE = 2.0f;

    // Texture dimensions (in texture pixels)
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 166;

    // Slot size in texture pixels (item is 16x16, slot outline is 18x18)
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;

    // Scaled dimensions for screen
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE); // 36 px
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE); // 32 px
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE); // 352 px
    public static final int WINDOW_HEIGHT = (int) (TEX_HEIGHT * GUI_SCALE); // 332 px

    // === SLOT POSITIONS (in texture pixels, scaled on render) ===
    // Main inventory (27 slots): 3 rows x 9 cols, starting at (8, 84)
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;

    // Hotbar (9 slots): 1 row x 9 cols, starting at (8, 142)
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;

    // Crafting grid 2x2: adjusted for player inventory crafting
    public static final int TEX_CRAFT_GRID_X = 88;
    public static final int TEX_CRAFT_GRID_Y = 26;

    // Crafting output: adjusted
    public static final int TEX_CRAFT_OUTPUT_X = 144;
    public static final int TEX_CRAFT_OUTPUT_Y = 36;

    // Grid dimensions
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;
    public static final int HOTBAR_ROWS = 1;
    public static final int CRAFTING_COLS = 2;
    public static final int CRAFTING_ROWS = 2;

    // Legacy constants for compatibility (can be removed later)
    public static final int SLOT_SPACING = 0; // Slots are adjacent in texture
    public static final int PADDING = 0; // No padding, texture handles it

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
     * Determine which slot the mouse is over using Minecraft-accurate texture
     * coordinates.
     * Returns slot index 0-35 (0-26 = main, 27-35 = hotbar), or -1 if none.
     * 
     * Layout:
     * - Slots 0-26: Main inventory (3 rows x 9 cols) at TEX_MAIN_INV
     * - Slots 27-35: Hotbar (1 row x 9 cols) at TEX_HOTBAR
     * - Slots 36-39: Crafting grid (2x2) at TEX_CRAFT_GRID
     * - Slot 40: Crafting output at TEX_CRAFT_OUTPUT
     */
    private int getSlotAtPosition(int mx, int my) {
        // Check if inside window bounds
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH ||
                my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }

        // Convert to texture-space coordinates (divide by scale)
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;

        // Check crafting grid (2x2) - slots 36-39
        if (texX >= TEX_CRAFT_GRID_X && texX < TEX_CRAFT_GRID_X + CRAFTING_COLS * TEX_SLOT_SIZE &&
                texY >= TEX_CRAFT_GRID_Y && texY < TEX_CRAFT_GRID_Y + CRAFTING_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_CRAFT_GRID_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_CRAFT_GRID_Y) / TEX_SLOT_SIZE);
            if (col < CRAFTING_COLS && row < CRAFTING_ROWS) {
                return 36 + row * CRAFTING_COLS + col;
            }
        }

        // Check crafting output - slot 40
        if (texX >= TEX_CRAFT_OUTPUT_X && texX < TEX_CRAFT_OUTPUT_X + TEX_SLOT_SIZE &&
                texY >= TEX_CRAFT_OUTPUT_Y && texY < TEX_CRAFT_OUTPUT_Y + TEX_SLOT_SIZE) {
            return 40;
        }

        // Check main inventory (3 rows x 9 cols) - slots 0-26
        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + COLS * TEX_SLOT_SIZE &&
                texY >= TEX_MAIN_INV_Y && texY < TEX_MAIN_INV_Y + MAIN_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_MAIN_INV_Y) / TEX_SLOT_SIZE);
            if (col < COLS && row < MAIN_ROWS) {
                return row * COLS + col;
            }
        }

        // Check hotbar (1 row x 9 cols) - slots 27-35
        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + COLS * TEX_SLOT_SIZE &&
                texY >= TEX_HOTBAR_Y && texY < TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            if (col < COLS) {
                return 27 + col;
            }
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
