package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.FuelRegistry;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.SmeltingRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.*;

public class FurnaceScreen {
    public static final float GUI_SCALE = 2.0f;
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 166;
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE);
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE);
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE);
    public static final int WINDOW_HEIGHT = (int) (TEX_HEIGHT * GUI_SCALE);
    public static final int TEX_INPUT_X = 56;
    public static final int TEX_INPUT_Y = 17;
    public static final int TEX_FUEL_X = 56;
    public static final int TEX_FUEL_Y = 53;
    public static final int TEX_OUTPUT_X = 116;
    public static final int TEX_OUTPUT_Y = 35;
    public static final int TEX_FLAME_X = 56;
    public static final int TEX_FLAME_Y = 36;
    public static final int TEX_ARROW_X = 79;
    public static final int TEX_ARROW_Y = 34;
    public static final int TEX_OVERLAY_U = 176;
    public static final int TEX_FLAME_WIDTH = 14;
    public static final int TEX_FLAME_PROGRESS_HEIGHT = 12;
    public static final int TEX_FLAME_EXTRA_HEIGHT = 2;
    public static final int TEX_ARROW_PROGRESS_WIDTH = 24;
    public static final int TEX_ARROW_HEIGHT = 16;
    public static final int TEX_ARROW_OVERLAY_V = 14;
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;

    private final Inventory inventory;
    private FurnaceTileEntity furnace;
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

    public FurnaceScreen(Inventory inventory) {
        this(inventory, null, null);
    }

    public FurnaceScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested) {
        this(inventory, inventoryCloseRequested, null);
    }

    public FurnaceScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested,
            BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.inventoryCloseRequested = ContainerScreenControls.closeRequester(inventoryCloseRequested);
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    public static ProgressOverlay getBurnFlameOverlay(FurnaceTileEntity furnace) {
        if (furnace == null || !furnace.isBurning()) {
            return null;
        }
        int remaining = getBurnTimeRemainingScaled(furnace, TEX_FLAME_PROGRESS_HEIGHT);
        int topOffset = TEX_FLAME_PROGRESS_HEIGHT - remaining;
        return new ProgressOverlay(
                TEX_FLAME_X,
                TEX_FLAME_Y + topOffset,
                TEX_OVERLAY_U,
                topOffset,
                TEX_FLAME_WIDTH,
                remaining + TEX_FLAME_EXTRA_HEIGHT);
    }

    public static ProgressOverlay getCookArrowOverlay(FurnaceTileEntity furnace) {
        if (furnace == null) {
            return null;
        }
        int progress = getCookProgressScaled(furnace, TEX_ARROW_PROGRESS_WIDTH);
        return new ProgressOverlay(
                TEX_ARROW_X,
                TEX_ARROW_Y,
                TEX_OVERLAY_U,
                TEX_ARROW_OVERLAY_V,
                progress + 1,
                TEX_ARROW_HEIGHT);
    }

    private static int getBurnTimeRemainingScaled(FurnaceTileEntity furnace, int scale) {
        int currentFuelBurnTime = furnace.getCurrentFuelBurnTime();
        if (currentFuelBurnTime <= 0) {
            currentFuelBurnTime = FurnaceTileEntity.COOK_TIME_TOTAL;
        }
        return Math.max(0, Math.min(scale, furnace.getBurnTime() * scale / currentFuelBurnTime));
    }

    private static int getCookProgressScaled(FurnaceTileEntity furnace, int scale) {
        return Math.max(0, Math.min(scale, furnace.getCookTime() * scale / FurnaceTileEntity.COOK_TIME_TOTAL));
    }

    public void open(FurnaceTileEntity furnace, int screenWidth, int screenHeight) {
        if (open) {
            close();
        }
        this.furnace = furnace;
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
        furnace = null;
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
            markFurnaceDirtyIfNeeded(keyboardDrop.sourceSlot());
            return;
        }

        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot,
                FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE)) {
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
            furnace.markDirty();
        }
        return collected;
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return furnace != null && !ItemStackOps.isEmpty(inventory.getCursorItem())
                && slotIndex >= 0 && slotIndex < FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE
                && slotIndex != FurnaceTileEntity.SLOT_OUTPUT;
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int[] furnaceSlots = ContainerSlotOrder.filteredRange(0, FurnaceTileEntity.SIZE,
                FurnaceTileEntity.SLOT_OUTPUT);
        int[] playerSlots = ContainerSlotOrder.range(FurnaceTileEntity.SIZE,
                FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot, 0, FurnaceTileEntity.SIZE,
                furnaceSlots, playerSlots);
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
                markDraggedFurnaceSlotsDirty();
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
        return open && BlockContainerValidity.sameTileWithinUseDistance(world, furnace, player,
                BlockType.FURNACE, BlockType.LIT_FURNACE);
    }

    private int getSlotAtPosition(int mx, int my) {
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH || my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;

        if (inSlot(texX, texY, TEX_INPUT_X, TEX_INPUT_Y)) {
            return FurnaceTileEntity.SLOT_INPUT;
        }
        if (inSlot(texX, texY, TEX_FUEL_X, TEX_FUEL_Y)) {
            return FurnaceTileEntity.SLOT_FUEL;
        }
        if (inSlot(texX, texY, TEX_OUTPUT_X, TEX_OUTPUT_Y)) {
            return FurnaceTileEntity.SLOT_OUTPUT;
        }

        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_MAIN_INV_Y && texY < TEX_MAIN_INV_Y + MAIN_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_MAIN_INV_Y) / TEX_SLOT_SIZE);
            return FurnaceTileEntity.SIZE + row * COLS + col;
        }

        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_HOTBAR_Y && texY < TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            return FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE + col;
        }
        return -1;
    }

    private boolean inSlot(float texX, float texY, int slotX, int slotY) {
        return texX >= slotX && texX < slotX + TEX_SLOT_SIZE
                && texY >= slotY && texY < slotY + TEX_SLOT_SIZE;
    }

    private void handleClick(int slotIndex, boolean rightClick) {
        if (slotIndex == -1 || furnace == null) {
            return;
        }

        ItemStack cursorItem = inventory.getCursorItem();
        ItemStack slotItem = getItemInSlot(slotIndex);

        if (isShiftDown()) {
            shiftClick(slotIndex, slotItem);
            return;
        }

        if (slotIndex == FurnaceTileEntity.SLOT_OUTPUT) {
            handleOutputClick(slotItem, rightClick);
            return;
        }

        if (rightClick) {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(ItemStackOps.splitHalf(slotItem));
                if (slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                }
            } else if (cursorItem != null && slotItem == null && canPlace(slotIndex, cursorItem)) {
                setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (canPlace(slotIndex, cursorItem) && ItemStackOps.mergeAmountInto(slotItem, cursorItem, 1) > 0) {
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
                markFurnaceDirtyIfNeeded(slotIndex);
            }
        } else {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(slotItem);
                setItemInSlot(slotIndex, null);
            } else if (cursorItem != null && slotItem == null && canPlace(slotIndex, cursorItem)) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(null);
            } else if (canPlace(slotIndex, cursorItem) && ItemStackOps.canMerge(slotItem, cursorItem)) {
                ItemStackOps.mergeInto(slotItem, cursorItem);
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
                markFurnaceDirtyIfNeeded(slotIndex);
            } else if (cursorItem != null && canPlace(slotIndex, cursorItem)) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(slotItem);
            }
        }
    }

    private void handleOutputClick(ItemStack slotItem, boolean rightClick) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }

        ItemStack cursorItem = inventory.getCursorItem();
        if (cursorItem == null) {
            if (rightClick) {
                inventory.setCursorItem(ItemStackOps.splitHalf(slotItem));
                if (slotItem.isEmpty()) {
                    setItemInSlot(FurnaceTileEntity.SLOT_OUTPUT, null);
                } else {
                    furnace.markDirty();
                }
            } else {
                inventory.setCursorItem(slotItem);
                setItemInSlot(FurnaceTileEntity.SLOT_OUTPUT, null);
            }
            return;
        }

        if (ItemStackOps.canMerge(cursorItem, slotItem)) {
            int moved = rightClick
                    ? ItemStackOps.mergeAmountInto(cursorItem, slotItem, 1)
                    : ItemStackOps.mergeInto(cursorItem, slotItem);
            if (moved == 0) {
                return;
            }
            if (slotItem.isEmpty()) {
                setItemInSlot(FurnaceTileEntity.SLOT_OUTPUT, null);
            } else {
                furnace.markDirty();
            }
        }
    }

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }
        if (slotIndex < FurnaceTileEntity.SIZE) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations())) {
                markFurnaceDirtyIfNeeded(slotIndex);
            }
            return;
        }

        if (SmeltingRegistry.getResult(slotItem) != null) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    new int[] { FurnaceTileEntity.SLOT_INPUT })) {
                furnace.markDirty();
            }
        } else if (FuelRegistry.isFuel(slotItem)) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    new int[] { FurnaceTileEntity.SLOT_FUEL })) {
                furnace.markDirty();
            }
        } else if (moveWithinPlayerInventory(slotIndex)) {
        }
    }

    private boolean moveWithinPlayerInventory(int slotIndex) {
        int playerIndex = slotIndex - FurnaceTileEntity.SIZE;
        if (playerIndex < 0) {
            return false;
        }
        if (playerIndex < Inventory.MAIN_SIZE) {
            return ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE,
                            FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE));
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
            return ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(FurnaceTileEntity.SIZE, FurnaceTileEntity.SIZE + Inventory.MAIN_SIZE));
        }
        return false;
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(FurnaceTileEntity.SIZE, Inventory.MAIN_SIZE,
                Inventory.HOTBAR_SIZE);
    }

    private boolean canPlace(int slotIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (slotIndex == FurnaceTileEntity.SLOT_INPUT) {
            return true;
        }
        if (slotIndex == FurnaceTileEntity.SLOT_FUEL) {
            return FuelRegistry.isFuel(stack);
        }
        return slotIndex >= FurnaceTileEntity.SIZE;
    }

    public ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex < FurnaceTileEntity.SIZE) {
            return furnace.getInventory()[slotIndex];
        }
        int playerIndex = slotIndex - FurnaceTileEntity.SIZE;
        if (playerIndex < Inventory.MAIN_SIZE) {
            return inventory.getMainInventory()[playerIndex];
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        return hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE ? inventory.getHotbar()[hotbarIndex] : null;
    }

    private void setItemInSlot(int slotIndex, ItemStack stack) {
        if (slotIndex < FurnaceTileEntity.SIZE) {
            furnace.getInventory()[slotIndex] = stack;
            furnace.markDirty();
            return;
        }
        int playerIndex = slotIndex - FurnaceTileEntity.SIZE;
        if (playerIndex < Inventory.MAIN_SIZE) {
            inventory.getMainInventory()[playerIndex] = stack;
        } else {
            int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
            if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
                inventory.getHotbar()[hotbarIndex] = stack;
            }
        }
    }

    private void markFurnaceDirtyIfNeeded(int slotIndex) {
        if (slotIndex < FurnaceTileEntity.SIZE) {
            furnace.markDirty();
        }
    }

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    private void markDraggedFurnaceSlotsDirty() {
        for (int slot : draggedSlots) {
            if (slot < FurnaceTileEntity.SIZE) {
                furnace.markDirty();
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
                return FurnaceScreen.this.canPlace(slotIndex, stack);
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

    public FurnaceTileEntity getFurnace() {
        return furnace;
    }

    public record ProgressOverlay(int x, int y, int sourceX, int sourceY, int width, int height) {
    }
}
