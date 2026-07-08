package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.CraftingGridOps;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.crafting.CraftingRecipe;
import com.craftzero.crafting.CraftingRegistry;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

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
    private World boundWorld;
    private Vector3i tablePos;

    // 3x3 crafting grid
    private ItemStack[] craftingGrid = new ItemStack[9];

    // Mouse state
    private int hoveredSlot = -1;

    // Window position
    private int windowX;
    private int windowY;

    // Items to throw when closing
    private List<ItemStack> itemsToThrow = new ArrayList<>();
    private final List<CraftAction> craftActions = new ArrayList<>();

    // Flag to open inventory after closing
    // Flag to open inventory after closing
    private boolean openInventoryAfterClose = false;

    // Dragging state
    private boolean isMouseDragging = false;
    private boolean mouseDragRightClick = false;
    private int dragStartSlot = -1;
    private final Set<Integer> draggedSlots = new LinkedHashSet<>();
    private final ContainerDoubleClickTracker doubleClickTracker = new ContainerDoubleClickTracker();
    private final BooleanSupplier inventoryCloseRequested;
    private final BooleanSupplier dropRequested;

    public CraftingTableScreen(Inventory inventory) {
        this(inventory, null, null);
    }

    public CraftingTableScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested) {
        this(inventory, inventoryCloseRequested, null);
    }

    public CraftingTableScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested,
            BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.inventoryCloseRequested = ContainerScreenControls.closeRequester(inventoryCloseRequested);
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    public void open(int screenWidth, int screenHeight) {
        open(null, null, screenWidth, screenHeight);
    }

    public void open(World world, Vector3i tablePos, int screenWidth, int screenHeight) {
        if (!isOpen) {
            isOpen = true;
            windowX = (screenWidth - WINDOW_WIDTH) / 2;
            windowY = (screenHeight - WINDOW_HEIGHT) / 2;
            Input.setCursorLocked(false);
            hoveredSlot = -1;
        }
        this.boundWorld = world;
        this.tablePos = tablePos == null ? null : new Vector3i(tablePos);
    }

    public void close() {
        if (isOpen) {
            isOpen = false;
            Input.setCursorLocked(true);
            hoveredSlot = -1;
            boundWorld = null;
            tablePos = null;

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

        if (ContainerScreenControls.shouldClose(inventoryCloseRequested)) {
            close();
            return;
        }

        // Get mouse position
        double mx = Input.getMouseX();
        double my = Input.getMouseY();

        hoveredSlot = getSlotAtPosition((int) mx, (int) my);

        if (ContainerKeyboardDrop.dropOne(dropRequested, inventory, dragSlotAccess(), hoveredSlot,
                itemsToThrow).dropped()) {
            return;
        }

        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot,
                10 + Inventory.MAIN_SIZE)) {
            return;
        }

        handleMouseButton(GLFW_MOUSE_BUTTON_LEFT, false);
        handleMouseButton(GLFW_MOUSE_BUTTON_RIGHT, true);
    }

    public boolean isStillUsable(Player player) {
        if (!isOpen) {
            return false;
        }
        if (boundWorld == null || tablePos == null) {
            return true;
        }
        return BlockContainerValidity.sameBlockWithinUseDistance(boundWorld, tablePos, player,
                BlockType.CRAFTING_TABLE);
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
            doubleClickTracker.reset();
            ContainerCursorDrop.dropOutside(inventory, itemsToThrow, rightClick);
            return;
        }
        if (isShiftDown()) {
            doubleClickTracker.recordClick(hoveredSlot, rightClick);
            handleClick(hoveredSlot, rightClick);
            return;
        }
        if (doubleClickTracker.isDoubleLeftClick(hoveredSlot, rightClick) && canHandleDoubleClick(hoveredSlot)) {
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

    private boolean handleDoubleClick(int slotIndex) {
        if (!canHandleDoubleClick(slotIndex)) {
            return false;
        }
        return ContainerDoubleClickCollector.collectMatching(dragSlotAccess(), doubleClickCollectSlots(slotIndex),
                inventory.getCursorItem());
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return !ItemStackOps.isEmpty(inventory.getCursorItem())
                && (slotIndex >= 0 && slotIndex < 9 || slotIndex >= 10 && slotIndex <= 45);
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int[] craftingSlots = ContainerSlotOrder.range(0, 9);
        int[] playerSlots = ContainerSlotOrder.range(10, 46);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot, 0, 9, craftingSlots, playerSlots);
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
        if (isShiftDown()) {
            if (slotIndex == 9) {
                ItemStack[] gridBefore = snapshotCraftingGrid();
                ItemStack cursorBefore = copyStack(inventory.getCursorItem());
                int crafted = shiftClickOutput();
                recordCraftAction(gridBefore, cursorBefore, true, crafted);
            } else {
                shiftClick(slotIndex, slotItem);
            }
            return;
        }

        // Handle crafting output slot (slot 9)
        if (slotIndex == 9) {
            ItemStack[] gridBefore = snapshotCraftingGrid();
            ItemStack cursorBefore = copyStack(inventory.getCursorItem());
            CraftingRecipe recipe = CraftingRegistry.findRecipe3x3(craftingGrid);
            if (slotItem == null || recipe == null)
                return;

            if (CraftingGridOps.takeOutputToCursor(inventory, craftingGrid, recipe, itemsToThrow)) {
                recordCraftAction(gridBefore, cursorBefore, false, 1);
            }
            return;
        }

        // Regular slot interaction (Grid/Inventory)
        if (isRightClick) {
            if (cursorItem == null) {
                // Pick up half
                if (slotItem != null && !slotItem.isEmpty()) {
                    ItemStack taken = ItemStackOps.splitHalf(slotItem);
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
                    setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                    if (cursorItem.isEmpty()) {
                        inventory.setCursorItem(null);
                    }
                } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
                    if (ItemStackOps.mergeAmountInto(slotItem, cursorItem, 1) > 0) {
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
                } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
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

    private ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 9) {
            return craftingGrid[slotIndex];
        } else if (slotIndex == 9) {
            CraftingRecipe recipe = CraftingRegistry.findRecipe3x3(craftingGrid);
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

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    private int shiftClickOutput() {
        return CraftingGridOps.quickMoveOutputToInventory(inventory, craftingGrid,
                () -> CraftingRegistry.findRecipe3x3(craftingGrid), itemsToThrow);
    }

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }
        if (slotIndex >= 0 && slotIndex < 9) {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations());
            return;
        }
        if (slotIndex >= 10 && slotIndex <= 36) {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, ContainerSlotOrder.range(37, 46));
        } else if (slotIndex >= 37 && slotIndex <= 45) {
            ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, ContainerSlotOrder.range(10, 37));
        }
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(10, Inventory.MAIN_SIZE, Inventory.HOTBAR_SIZE);
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
                return slotIndex >= 0 && slotIndex < 9
                        || slotIndex >= 10 && slotIndex <= 45;
            }

            @Override
            public int maxStackSize(int slotIndex, ItemStack stack) {
                return stack.getMaxStackSize();
            }
        };
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

    public List<CraftAction> drainCraftActions() {
        List<CraftAction> actions = new ArrayList<>(craftActions);
        craftActions.clear();
        return actions;
    }

    public void applyRemoteCraftingGrid(int x, int y, int z, ItemStack[] grid) {
        if (!isOpen || tablePos == null || tablePos.x != x || tablePos.y != y || tablePos.z != z
                || grid == null) {
            return;
        }
        for (int i = 0; i < craftingGrid.length; i++) {
            craftingGrid[i] = i < grid.length && grid[i] != null ? grid[i].copy() : null;
        }
    }

    private ItemStack[] snapshotCraftingGrid() {
        ItemStack[] snapshot = new ItemStack[craftingGrid.length];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = craftingGrid[i] == null ? null : craftingGrid[i].copy();
        }
        return snapshot;
    }

    private ItemStack copyStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private void recordCraftAction(ItemStack[] gridBefore, ItemStack cursorBefore, boolean quickMove, int crafted) {
        if (crafted > 0) {
            craftActions.add(new CraftAction(3, quickMove, crafted, gridBefore, cursorBefore, tablePos));
        }
    }
}
