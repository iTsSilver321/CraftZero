package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.DispenserTileEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.*;

public class DispenserScreen {
    public static final float GUI_SCALE = 2.0f;
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 166;
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE);
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE);
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE);
    public static final int WINDOW_HEIGHT = (int) (TEX_HEIGHT * GUI_SCALE);
    public static final int TEX_CONTAINER_X = 62;
    public static final int TEX_CONTAINER_Y = 17;
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;
    public static final int DISPENSER_COLS = 3;
    public static final int DISPENSER_ROWS = 3;
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;

    private final Inventory inventory;
    private DispenserTileEntity dispenser;
    private boolean open;
    private int windowX;
    private int windowY;
    private int hoveredSlot = -1;
    private boolean isMouseDragging;
    private boolean mouseDragRightClick;
    private int dragStartSlot = -1;
    private final Set<Integer> draggedSlots = new LinkedHashSet<>();
    private final List<ItemStack> itemsToThrow = new ArrayList<>();
    private final ContainerDoubleClickTracker doubleClickTracker = new ContainerDoubleClickTracker();
    private final BooleanSupplier inventoryCloseRequested;
    private final BooleanSupplier dropRequested;

    public DispenserScreen(Inventory inventory) {
        this(inventory, null, null);
    }

    public DispenserScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested) {
        this(inventory, inventoryCloseRequested, null);
    }

    public DispenserScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested,
            BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.inventoryCloseRequested = ContainerScreenControls.closeRequester(inventoryCloseRequested);
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    public void open(DispenserTileEntity dispenser, int screenWidth, int screenHeight) {
        if (open) {
            close();
        }
        this.dispenser = dispenser;
        this.open = true;
        this.windowX = (screenWidth - WINDOW_WIDTH) / 2;
        this.windowY = (screenHeight - WINDOW_HEIGHT) / 2;
        this.hoveredSlot = -1;
        Input.setCursorLocked(false);
    }

    public void close() {
        if (!open) {
            return;
        }
        if (inventory.getCursorItem() != null) {
            itemsToThrow.add(inventory.getCursorItem());
            inventory.setCursorItem(null);
        }
        open = false;
        hoveredSlot = -1;
        dispenser = null;
        Input.setCursorLocked(true);
    }

    public void update() {
        if (!open) {
            return;
        }
        if (ContainerScreenControls.shouldClose(inventoryCloseRequested)) {
            close();
            return;
        }

        hoveredSlot = getSlotAtPosition((int) Input.getMouseX(), (int) Input.getMouseY());

        ContainerKeyboardDrop.DropResult keyboardDrop =
                ContainerKeyboardDrop.dropOne(dropRequested, inventory, dragSlotAccess(), hoveredSlot, itemsToThrow);
        if (keyboardDrop.dropped()) {
            markDispenserDirtyIfNeeded(keyboardDrop.sourceSlot());
            return;
        }

        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot,
                DispenserTileEntity.SIZE + Inventory.MAIN_SIZE)) {
            return;
        }

        handleMouseButton(GLFW_MOUSE_BUTTON_LEFT, false);
        handleMouseButton(GLFW_MOUSE_BUTTON_RIGHT, true);
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
        boolean collected = ContainerDoubleClickCollector.collectMatching(dragSlotAccess(), doubleClickCollectSlots(slotIndex),
                inventory.getCursorItem());
        if (collected) {
            dispenser.markDirty();
        }
        return collected;
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return dispenser != null && !ItemStackOps.isEmpty(inventory.getCursorItem())
                && slotIndex >= 0
                && slotIndex < DispenserTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE;
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int[] dispenserSlots = ContainerSlotOrder.range(0, DispenserTileEntity.SIZE);
        int[] playerSlots = ContainerSlotOrder.range(DispenserTileEntity.SIZE,
                DispenserTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot, 0, DispenserTileEntity.SIZE,
                dispenserSlots, playerSlots);
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
            } else {
                markDraggedDispenserSlotsDirty();
                if (ItemStackOps.isEmpty(cursorItem)) {
                    inventory.setCursorItem(null);
                }
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

    public boolean isStillUsable(World world, Player player) {
        return open && BlockContainerValidity.sameTileWithinUseDistance(world, dispenser, player, BlockType.DISPENSER);
    }

    private int getSlotAtPosition(int mx, int my) {
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH || my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;

        if (texX >= TEX_CONTAINER_X && texX < TEX_CONTAINER_X + DISPENSER_COLS * TEX_SLOT_SIZE
                && texY >= TEX_CONTAINER_Y && texY < TEX_CONTAINER_Y + DISPENSER_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_CONTAINER_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_CONTAINER_Y) / TEX_SLOT_SIZE);
            return row * DISPENSER_COLS + col;
        }

        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_MAIN_INV_Y && texY < TEX_MAIN_INV_Y + MAIN_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_MAIN_INV_Y) / TEX_SLOT_SIZE);
            return DispenserTileEntity.SIZE + row * COLS + col;
        }

        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_HOTBAR_Y && texY < TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            return DispenserTileEntity.SIZE + Inventory.MAIN_SIZE + col;
        }
        return -1;
    }

    private void handleClick(int slotIndex, boolean rightClick) {
        if (slotIndex == -1 || dispenser == null) {
            return;
        }
        ItemStack cursorItem = inventory.getCursorItem();
        ItemStack slotItem = getItemInSlot(slotIndex);

        if (isShiftDown()) {
            shiftClick(slotIndex, slotItem);
            return;
        }

        if (rightClick) {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(ItemStackOps.splitHalf(slotItem));
                if (slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                } else {
                    markDispenserDirtyIfNeeded(slotIndex);
                }
            } else if (cursorItem != null && slotItem == null) {
                setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (ItemStackOps.mergeAmountInto(slotItem, cursorItem, 1) > 0) {
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
                markDispenserDirtyIfNeeded(slotIndex);
            }
        } else {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(slotItem);
                setItemInSlot(slotIndex, null);
            } else if (cursorItem != null && slotItem == null) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(null);
            } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
                if (ItemStackOps.mergeInto(slotItem, cursorItem) > 0) {
                    markDispenserDirtyIfNeeded(slotIndex);
                }
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (cursorItem != null) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(slotItem);
            }
        }
    }

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }
        if (slotIndex < DispenserTileEntity.SIZE) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations())) {
                dispenser.markDirty();
            }
            return;
        }

        if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                ContainerSlotOrder.range(0, DispenserTileEntity.SIZE))) {
            dispenser.markDirty();
        }
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(DispenserTileEntity.SIZE, Inventory.MAIN_SIZE,
                Inventory.HOTBAR_SIZE);
    }

    public ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex < DispenserTileEntity.SIZE) {
            return dispenser.getInventory()[slotIndex];
        }
        int playerIndex = slotIndex - DispenserTileEntity.SIZE;
        if (playerIndex < Inventory.MAIN_SIZE) {
            return inventory.getMainInventory()[playerIndex];
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        return hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE ? inventory.getHotbar()[hotbarIndex] : null;
    }

    private void setItemInSlot(int slotIndex, ItemStack stack) {
        if (slotIndex < DispenserTileEntity.SIZE) {
            dispenser.getInventory()[slotIndex] = stack;
            dispenser.markDirty();
            return;
        }
        int playerIndex = slotIndex - DispenserTileEntity.SIZE;
        if (playerIndex < Inventory.MAIN_SIZE) {
            inventory.getMainInventory()[playerIndex] = stack;
        } else {
            int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
            if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
                inventory.getHotbar()[hotbarIndex] = stack;
            }
        }
    }

    private void markDispenserDirtyIfNeeded(int slotIndex) {
        if (slotIndex < DispenserTileEntity.SIZE) {
            dispenser.markDirty();
        }
    }

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    private void markDraggedDispenserSlotsDirty() {
        for (int slot : draggedSlots) {
            if (slot < DispenserTileEntity.SIZE) {
                dispenser.markDirty();
                return;
            }
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
                return slotIndex >= 0 && slotIndex < DispenserTileEntity.SIZE + Inventory.MAIN_SIZE
                        + Inventory.HOTBAR_SIZE;
            }

            @Override
            public int maxStackSize(int slotIndex, ItemStack stack) {
                return stack == null || stack.isEmpty() ? 0 : stack.getMaxStackSize();
            }
        };
    }

    public List<ItemStack> getAndClearItemsToThrow() {
        List<ItemStack> items = new ArrayList<>(itemsToThrow);
        itemsToThrow.clear();
        return items;
    }

    public boolean isOpen() {
        return open;
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

    public DispenserTileEntity getDispenser() {
        return dispenser;
    }
}
