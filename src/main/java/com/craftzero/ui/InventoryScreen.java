package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.CraftingGridOps;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.inventory.ItemStack;
import com.craftzero.crafting.CraftingRecipe;
import com.craftzero.crafting.CraftingRegistry;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.world.BlockType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

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

    // Armor slots: helmet, chestplate, leggings, boots.
    public static final int TEX_ARMOR_X = 8;
    public static final int TEX_ARMOR_Y = 8;

    public static final int MAIN_SLOT_START = 0;
    public static final int HOTBAR_SLOT_START = 27;
    public static final int CRAFTING_SLOT_START = 36;
    public static final int CRAFTING_OUTPUT_SLOT = 40;
    public static final int ARMOR_SLOT_START = 41;
    public static final int ARMOR_SLOT_COUNT = 4;

    // Grid dimensions
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;
    public static final int HOTBAR_ROWS = 1;
    public static final int CRAFTING_COLS = 2;
    public static final int CRAFTING_ROWS = 2;

    // Legacy constants for compatibility (can be removed later)
    public static final int SLOT_SPACING = 0; // Slots are adjacent in texture
    public static final int PADDING = 0; // No padding, texture handles it
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    private boolean isOpen = false;
    private Inventory inventory;

    // Mouse state
    private int hoveredSlot = -1; // -1 = no slot hovered

    // Dragging state
    private boolean isMouseDragging = false;
    private boolean mouseDragRightClick = false;
    private int dragStartSlot = -1;
    private final Set<Integer> draggedSlots = new LinkedHashSet<>();
    private int lastClickSlot = -1;
    private long lastClickNanos = 0L;
    private boolean lastClickRightClick = false;

    // Window position (centered, calculated on open)
    private int windowX;
    private int windowY;
    private int screenWidth;
    private int screenHeight;
    private final BooleanSupplier dropRequested;

    public InventoryScreen(Inventory inventory) {
        this(inventory, null);
    }

    public InventoryScreen(Inventory inventory, BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    /**
     * Toggle the inventory screen open/closed.
     */
    public void toggle(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        if (isOpen) {
            close();
            return;
        }

        isOpen = true;
        // Center the window
        windowX = (screenWidth - WINDOW_WIDTH) / 2;
        windowY = (screenHeight - WINDOW_HEIGHT) / 2;
        Input.setCursorLocked(false);
        hoveredSlot = -1;
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

            ItemStack[] craftingGrid = inventory.getCraftingGrid();
            for (int i = 0; i < craftingGrid.length; i++) {
                if (craftingGrid[i] != null && !craftingGrid[i].isEmpty()) {
                    itemsToThrow.add(craftingGrid[i]);
                    craftingGrid[i] = null;
                }
            }
            if (inventory.getCursorItem() != null) {
                itemsToThrow.add(inventory.getCursorItem());
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

        if (ContainerKeyboardDrop.dropOne(dropRequested, inventory, dragSlotAccess(), hoveredSlot,
                itemsToThrow).dropped()) {
            return;
        }

        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot, HOTBAR_SLOT_START)) {
            return;
        }

        handleMouseButton(GLFW_MOUSE_BUTTON_LEFT, false);
        handleMouseButton(GLFW_MOUSE_BUTTON_RIGHT, true);
    }

    // Items to be thrown after click-out or close behavior.
    private final List<ItemStack> itemsToThrow = new ArrayList<>();
    private final List<CraftAction> craftActions = new ArrayList<>();

    /**
     * Get and clear the item to throw (for Main to handle).
     */
    public ItemStack getAndClearItemToThrow() {
        if (itemsToThrow.isEmpty()) {
            return null;
        }
        return itemsToThrow.remove(0);
    }

    public List<ItemStack> getAndClearItemsToThrow() {
        List<ItemStack> items = new ArrayList<>(itemsToThrow);
        itemsToThrow.clear();
        return items;
    }

    private void handleMouseButton(int button, boolean rightClick) {
        if (Input.isButtonPressed(button)) {
            startMouseDrag(rightClick);
        }
        if (isMouseDragging && mouseDragRightClick == rightClick && Input.isButtonDown(button)) {
            continueMouseDrag();
        }
        if (isMouseDragging && mouseDragRightClick == rightClick && Input.isButtonReleased(button)) {
            finishMouseDrag();
        }
    }

    private void startMouseDrag(boolean rightClick) {
        if (hoveredSlot == -1) {
            resetDoubleClickTracking();
            ContainerCursorDrop.dropOutside(inventory, itemsToThrow, rightClick);
            return;
        }
        if (isShiftDown()) {
            recordClick(hoveredSlot, rightClick);
            handleClick(hoveredSlot, rightClick);
            return;
        }
        if (isDoubleLeftClick(hoveredSlot, rightClick) && canHandleDoubleClick(hoveredSlot)) {
            handleDoubleClick(hoveredSlot);
            return;
        }
        ItemStack cursorItem = inventory.getCursorItem();
        if (ItemStackOps.isEmpty(cursorItem) || !ContainerDragDistributor.canDragInto(dragSlotAccess(), hoveredSlot,
                cursorItem)) {
            handleClick(hoveredSlot, rightClick);
            return;
        }
        isMouseDragging = true;
        mouseDragRightClick = rightClick;
        dragStartSlot = hoveredSlot;
        draggedSlots.clear();
        draggedSlots.add(hoveredSlot);
    }

    private boolean isDoubleLeftClick(int slotIndex, boolean rightClick) {
        long now = System.nanoTime();
        boolean doubleClick = !rightClick && !lastClickRightClick && slotIndex == lastClickSlot
                && now - lastClickNanos <= DOUBLE_CLICK_NANOS;
        recordClick(slotIndex, rightClick, now);
        return doubleClick;
    }

    private void recordClick(int slotIndex, boolean rightClick) {
        recordClick(slotIndex, rightClick, System.nanoTime());
    }

    private void recordClick(int slotIndex, boolean rightClick, long now) {
        lastClickSlot = slotIndex;
        lastClickRightClick = rightClick;
        lastClickNanos = now;
    }

    private void resetDoubleClickTracking() {
        lastClickSlot = -1;
        lastClickRightClick = false;
        lastClickNanos = 0L;
    }

    private boolean handleDoubleClick(int slotIndex) {
        if (!canHandleDoubleClick(slotIndex)) {
            return false;
        }
        return ContainerDoubleClickCollector.collectMatching(dragSlotAccess(), doubleClickCollectSlots(slotIndex),
                inventory.getCursorItem());
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return slotIndex >= MAIN_SLOT_START && slotIndex < CRAFTING_OUTPUT_SLOT
                && !ItemStackOps.isEmpty(inventory.getCursorItem());
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int[] playerSlots = ContainerSlotOrder.range(MAIN_SLOT_START, CRAFTING_SLOT_START);
        int[] craftingSlots = ContainerSlotOrder.range(CRAFTING_SLOT_START, CRAFTING_OUTPUT_SLOT);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot,
                CRAFTING_SLOT_START, CRAFTING_OUTPUT_SLOT, craftingSlots, playerSlots);
    }

    private void continueMouseDrag() {
        ItemStack cursorItem = inventory.getCursorItem();
        if (hoveredSlot != -1 && !draggedSlots.contains(hoveredSlot)
                && ContainerDragDistributor.canDragInto(dragSlotAccess(), hoveredSlot, cursorItem)) {
            draggedSlots.add(hoveredSlot);
        }
    }

    private void finishMouseDrag() {
        if (draggedSlots.size() <= 1) {
            handleClick(dragStartSlot, mouseDragRightClick);
        } else {
            ItemStack cursorItem = inventory.getCursorItem();
            int moved = ContainerDragDistributor.distribute(dragSlotAccess(), draggedSlots, cursorItem,
                    mouseDragRightClick);
            if (moved == 0) {
                handleClick(dragStartSlot, mouseDragRightClick);
            } else if (ItemStackOps.isEmpty(cursorItem)) {
                inventory.setCursorItem(null);
            }
        }
        clearMouseDrag();
    }

    private void clearMouseDrag() {
        isMouseDragging = false;
        mouseDragRightClick = false;
        dragStartSlot = -1;
        draggedSlots.clear();
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
     * - Slots 41-44: Armor slots at TEX_ARMOR
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

        // Check armor slots (41-44)
        if (texX >= TEX_ARMOR_X && texX < TEX_ARMOR_X + TEX_SLOT_SIZE &&
                texY >= TEX_ARMOR_Y && texY < TEX_ARMOR_Y + ARMOR_SLOT_COUNT * TEX_SLOT_SIZE) {
            int row = (int) ((texY - TEX_ARMOR_Y) / TEX_SLOT_SIZE);
            if (row < ARMOR_SLOT_COUNT) {
                return ARMOR_SLOT_START + row;
            }
        }

        // Check crafting grid (2x2) - slots 36-39
        if (texX >= TEX_CRAFT_GRID_X && texX < TEX_CRAFT_GRID_X + CRAFTING_COLS * TEX_SLOT_SIZE &&
                texY >= TEX_CRAFT_GRID_Y && texY < TEX_CRAFT_GRID_Y + CRAFTING_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_CRAFT_GRID_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_CRAFT_GRID_Y) / TEX_SLOT_SIZE);
            if (col < CRAFTING_COLS && row < CRAFTING_ROWS) {
                return CRAFTING_SLOT_START + row * CRAFTING_COLS + col;
            }
        }

        // Check crafting output - slot 40
        if (texX >= TEX_CRAFT_OUTPUT_X && texX < TEX_CRAFT_OUTPUT_X + TEX_SLOT_SIZE &&
                texY >= TEX_CRAFT_OUTPUT_Y && texY < TEX_CRAFT_OUTPUT_Y + TEX_SLOT_SIZE) {
            return CRAFTING_OUTPUT_SLOT;
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
                return HOTBAR_SLOT_START + col;
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
        if (slotIndex == CRAFTING_OUTPUT_SLOT) {
            ItemStack[] gridBefore = snapshotCraftingGrid();
            ItemStack cursorBefore = copyStack(inventory.getCursorItem());
            // SHIFT+CLICK Output
            if (isShiftDown()) {
                int crafted = CraftingGridOps.quickMoveOutputToInventory(inventory, inventory.getCraftingGrid(),
                        () -> CraftingRegistry.findRecipe(inventory.getCraftingGrid()), itemsToThrow);
                recordCraftAction(gridBefore, cursorBefore, true, crafted);
                return;
            }

            if (slotItem == null)
                return;

            // Regular click on output
            CraftingRecipe recipe = CraftingRegistry.findRecipe(inventory.getCraftingGrid());
            if (CraftingGridOps.takeOutputToCursor(inventory, inventory.getCraftingGrid(), recipe, itemsToThrow)) {
                recordCraftAction(gridBefore, cursorBefore, false, 1);
            }
            return;
        }

        if (isShiftDown()) {
            shiftClick(slotIndex, slotItem);
            return;
        }

        if (isArmorSlot(slotIndex)) {
            handleArmorSlotClick(slotIndex, slotItem, cursorItem, isRightClick);
            return;
        }

        if (isRightClick) {
            // Right-click logic
            if (cursorItem == null && slotItem != null) {
                // Pick up half the stack
                ItemStack picked = ItemStackOps.splitHalf(slotItem);
                if (slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                }
                inventory.setCursorItem(picked);
            } else if (cursorItem != null && slotItem == null) {
                // Place one item
                setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
                // Add one to stack
                if (ItemStackOps.mergeAmountInto(slotItem, cursorItem, 1) > 0) {
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
                if (ItemStackOps.canMerge(slotItem, cursorItem)) {
                    // Merge stacks
                    ItemStackOps.mergeInto(slotItem, cursorItem);
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

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }

        if (isArmorSlot(slotIndex)) {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations());
            return;
        }

        ArmorSlot armorSlot = ArmorMaterial.slotOf(slotItem.getType());
        if (armorSlot != null && tryEquipArmorStack(slotItem, armorSlot)) {
            if (slotItem.isEmpty()) {
                setItemInSlot(slotIndex, null);
            }
            return;
        }

        if (slotIndex >= CRAFTING_SLOT_START && slotIndex < CRAFTING_SLOT_START + Inventory.CRAFTING_SIZE) {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations());
        } else if (slotIndex >= HOTBAR_SLOT_START) {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(MAIN_SLOT_START, HOTBAR_SLOT_START));
        } else {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(HOTBAR_SLOT_START, HOTBAR_SLOT_START + Inventory.HOTBAR_SIZE));
        }
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(MAIN_SLOT_START, Inventory.MAIN_SIZE, Inventory.HOTBAR_SIZE);
    }

    private void handleArmorSlotClick(int slotIndex, ItemStack slotItem, ItemStack cursorItem, boolean isRightClick) {
        if (cursorItem == null || cursorItem.isEmpty()) {
            if (slotItem == null || slotItem.isEmpty()) {
                return;
            }
            ItemStack picked = isRightClick ? ItemStackOps.splitHalf(slotItem) : slotItem;
            if (isRightClick && slotItem.isEmpty()) {
                setItemInSlot(slotIndex, null);
            } else if (!isRightClick) {
                setItemInSlot(slotIndex, null);
            }
            inventory.setCursorItem(picked);
            return;
        }

        if (!isValidArmorForSlot(slotIndex, cursorItem)) {
            return;
        }

        if (slotItem == null || slotItem.isEmpty()) {
            setItemInSlot(slotIndex, isRightClick ? ItemStackOps.splitOne(cursorItem) : cursorItem);
            if (!isRightClick || cursorItem.isEmpty()) {
                inventory.setCursorItem(null);
            }
            return;
        }

        setItemInSlot(slotIndex, cursorItem);
        inventory.setCursorItem(slotItem);
    }

    private boolean tryEquipArmorStack(ItemStack stack, ArmorSlot armorSlot) {
        if (stack == null || stack.isEmpty() || armorSlot == null) {
            return false;
        }
        ItemStack[] armor = inventory.getArmor();
        int index = armorSlot.getIndex();
        if (index < 0 || index >= armor.length || armor[index] != null) {
            return false;
        }
        armor[index] = ItemStackOps.splitOne(stack);
        return true;
    }

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    /**
     * Get item from combined slot index.
     */
    private ItemStack getItemInSlot(int slotIndex) {
        if (isArmorSlot(slotIndex)) {
            return inventory.getArmor()[slotIndex - ARMOR_SLOT_START];
        } else if (slotIndex >= CRAFTING_SLOT_START && slotIndex < CRAFTING_SLOT_START + Inventory.CRAFTING_SIZE) {
            // Crafting grid
            return inventory.getCraftingGrid()[slotIndex - CRAFTING_SLOT_START];
        } else if (slotIndex == CRAFTING_OUTPUT_SLOT) {
            // Crafting output - return recipe result
            CraftingRecipe recipe = CraftingRegistry.findRecipe(inventory.getCraftingGrid());
            return recipe != null ? recipe.getOutput() : null;
        } else if (slotIndex >= HOTBAR_SLOT_START) {
            return inventory.getHotbar()[slotIndex - HOTBAR_SLOT_START];
        } else {
            return inventory.getMainInventory()[slotIndex];
        }
    }

    /**
     * Set item in combined slot index.
     */
    private void setItemInSlot(int slotIndex, ItemStack item) {
        if (isArmorSlot(slotIndex)) {
            if (item == null || item.isEmpty() || isValidArmorForSlot(slotIndex, item)) {
                inventory.getArmor()[slotIndex - ARMOR_SLOT_START] = item;
            }
        } else if (slotIndex >= CRAFTING_SLOT_START && slotIndex < CRAFTING_SLOT_START + Inventory.CRAFTING_SIZE) {
            // Crafting grid
            inventory.getCraftingGrid()[slotIndex - CRAFTING_SLOT_START] = item;
        } else if (slotIndex == CRAFTING_OUTPUT_SLOT) {
            // Output slot - cannot directly set
            return;
        } else if (slotIndex >= HOTBAR_SLOT_START) {
            inventory.getHotbar()[slotIndex - HOTBAR_SLOT_START] = item;
        } else {
            inventory.getMainInventory()[slotIndex] = item;
        }
    }

    private static boolean isArmorSlot(int slotIndex) {
        return slotIndex >= ARMOR_SLOT_START && slotIndex < ARMOR_SLOT_START + ARMOR_SLOT_COUNT;
    }

    private static boolean isValidArmorForSlot(int slotIndex, ItemStack stack) {
        if (!isArmorSlot(slotIndex) || stack == null || stack.isEmpty()) {
            return false;
        }
        ArmorSlot expected = ArmorSlot.values()[slotIndex - ARMOR_SLOT_START];
        return ArmorMaterial.slotOf(stack.getType()) == expected;
    }

    private ContainerDragDistributor.Slots dragSlotAccess() {
        return new ContainerDragDistributor.Slots() {
            @Override
            public ItemStack get(int slotIndex) {
                return getItemInSlot(slotIndex);
            }

            @Override
            public void set(int slotIndex, ItemStack stack) {
                setItemInSlot(slotIndex, stack);
            }

            @Override
            public boolean canPlace(int slotIndex, ItemStack stack) {
                if (slotIndex == CRAFTING_OUTPUT_SLOT) {
                    return false;
                }
                if (isArmorSlot(slotIndex)) {
                    return isValidArmorForSlot(slotIndex, stack);
                }
                return slotIndex >= MAIN_SLOT_START && slotIndex < CRAFTING_OUTPUT_SLOT;
            }

            @Override
            public int maxStackSize(int slotIndex, ItemStack stack) {
                return isArmorSlot(slotIndex) ? 1 : stack.getMaxStackSize();
            }
        };
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

    public List<CraftAction> drainCraftActions() {
        List<CraftAction> actions = new ArrayList<>(craftActions);
        craftActions.clear();
        return actions;
    }

    private ItemStack[] snapshotCraftingGrid() {
        ItemStack[] grid = inventory.getCraftingGrid();
        ItemStack[] snapshot = new ItemStack[Inventory.CRAFTING_SIZE];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = grid[i] == null ? null : grid[i].copy();
        }
        return snapshot;
    }

    private ItemStack copyStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private void recordCraftAction(ItemStack[] gridBefore, ItemStack cursorBefore, boolean quickMove, int crafted) {
        if (crafted > 0) {
            craftActions.add(new CraftAction(2, quickMove, crafted, gridBefore, cursorBefore, null));
        }
    }
}
