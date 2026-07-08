package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.ChestTileEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.*;

public class ChestScreen {
    public static final float GUI_SCALE = 2.0f;
    public static final int TEX_WIDTH = 176;
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE);
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE);
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE);
    public static final int TEX_CONTAINER_X = 8;
    public static final int TEX_CONTAINER_Y = 18;
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_HOTBAR_X = 8;
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    private final Inventory inventory;
    private boolean open;
    private World boundWorld;
    private ChestTileEntity firstChest;
    private ChestTileEntity secondChest;
    private ChestMinecartEntity minecart;
    private ItemStack[] minecartInventory;
    private boolean minecartDirty;
    private int hoveredSlot = -1;
    private int windowX;
    private int windowY;
    private int windowHeight;
    private boolean isMouseDragging;
    private boolean mouseDragRightClick;
    private int dragStartSlot = -1;
    private final Set<Integer> draggedSlots = new LinkedHashSet<>();
    private final List<ItemStack> itemsToThrow = new ArrayList<>();
    private final BooleanSupplier inventoryCloseRequested;
    private int lastClickSlot = -1;
    private long lastClickNanos = 0L;
    private boolean lastClickRightClick = false;
    private final BooleanSupplier dropRequested;

    public ChestScreen(Inventory inventory) {
        this(inventory, null, null);
    }

    public ChestScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested) {
        this(inventory, inventoryCloseRequested, null);
    }

    public ChestScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested,
            BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.inventoryCloseRequested = ContainerScreenControls.closeRequester(inventoryCloseRequested);
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    public void open(World world, ChestTileEntity chest, int screenWidth, int screenHeight) {
        if (open) {
            close();
        }
        bindChests(world, chest);
        firstChest.open();
        if (secondChest != null) {
            secondChest.open();
        }
        boundWorld = world;
        open = true;
        windowHeight = (int) ((114 + getContainerRows() * 18) * GUI_SCALE);
        windowX = (screenWidth - WINDOW_WIDTH) / 2;
        windowY = (screenHeight - windowHeight) / 2;
        hoveredSlot = -1;
        Input.setCursorLocked(false);
    }

    void bindChests(World world, ChestTileEntity chest) {
        minecart = null;
        minecartInventory = null;
        ChestTileEntity adjacent = world.getAdjacentChest(chest);
        if (adjacent != null && comesBefore(adjacent, chest)) {
            firstChest = adjacent;
            secondChest = chest;
        } else {
            firstChest = chest;
            secondChest = adjacent;
        }
    }

    public void openMinecart(ChestMinecartEntity minecart, int screenWidth, int screenHeight) {
        if (open) {
            close();
        }
        firstChest = null;
        secondChest = null;
        this.minecart = minecart;
        minecartInventory = minecart.getInventory();
        minecartDirty = false;
        boundWorld = null;
        open = true;
        windowHeight = (int) ((114 + getContainerRows() * 18) * GUI_SCALE);
        windowX = (screenWidth - WINDOW_WIDTH) / 2;
        windowY = (screenHeight - windowHeight) / 2;
        hoveredSlot = -1;
        Input.setCursorLocked(false);
    }

    private boolean comesBefore(ChestTileEntity a, ChestTileEntity b) {
        if (a.getPos().z() != b.getPos().z()) {
            return a.getPos().z() < b.getPos().z();
        }
        return a.getPos().x() < b.getPos().x();
    }

    public void close() {
        if (!open) {
            return;
        }
        if (firstChest != null) {
            firstChest.close();
        }
        if (secondChest != null) {
            secondChest.close();
        }
        if (inventory.getCursorItem() != null) {
            itemsToThrow.add(inventory.getCursorItem());
            inventory.setCursorItem(null);
        }
        minecart = null;
        minecartInventory = null;
        minecartDirty = false;
        boundWorld = null;
        open = false;
        hoveredSlot = -1;
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
            if (keyboardDrop.sourceSlot() >= 0 && keyboardDrop.sourceSlot() < getContainerSize()) {
                markChestsDirty();
            }
            return;
        }

        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot,
                getContainerSize() + Inventory.MAIN_SIZE)) {
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
        boolean collected = ContainerDoubleClickCollector.collectMatching(dragSlotAccess(), doubleClickCollectSlots(slotIndex),
                inventory.getCursorItem());
        if (collected) {
            markChestsDirty();
        }
        return collected;
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return slotIndex >= 0 && slotIndex < getContainerSize() + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE
                && !ItemStackOps.isEmpty(inventory.getCursorItem());
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int containerSize = getContainerSize();
        int[] containerSlots = ContainerSlotOrder.range(0, containerSize);
        int[] playerSlots = ContainerSlotOrder.range(containerSize,
                containerSize + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot, 0, containerSize, containerSlots, playerSlots);
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
                markDraggedContainerSlotsDirty();
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

    public boolean isStillUsable(Player player) {
        if (!open) {
            return false;
        }
        if (minecartInventory != null) {
            return minecart != null && !minecart.isRemoved() && player != null
                    && distanceSquared(player, minecart) <= 64.0f;
        }
        if (boundWorld == null || firstChest == null || player == null) {
            return false;
        }
        return BlockContainerValidity.sameTileWithinUseDistance(boundWorld, firstChest, player, BlockType.CHEST)
                && (secondChest == null
                        || BlockContainerValidity.sameTileWithinUseDistance(boundWorld, secondChest, player,
                                BlockType.CHEST));
    }

    private static float distanceSquared(Player player, ChestMinecartEntity minecart) {
        float dx = player.getPosition().x - minecart.getX();
        float dy = player.getPosition().y - minecart.getY();
        float dz = player.getPosition().z - minecart.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private int getSlotAtPosition(int mx, int my) {
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH || my < windowY || my >= windowY + windowHeight) {
            return -1;
        }
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;
        int rows = getContainerRows();

        if (texX >= TEX_CONTAINER_X && texX < TEX_CONTAINER_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_CONTAINER_Y && texY < TEX_CONTAINER_Y + rows * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_CONTAINER_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_CONTAINER_Y) / TEX_SLOT_SIZE);
            return row * COLS + col;
        }

        int mainY = getTexMainInvY();
        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + COLS * TEX_SLOT_SIZE
                && texY >= mainY && texY < mainY + MAIN_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - mainY) / TEX_SLOT_SIZE);
            return getContainerSize() + row * COLS + col;
        }

        int hotbarY = getTexHotbarY();
        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + COLS * TEX_SLOT_SIZE
                && texY >= hotbarY && texY < hotbarY + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            return getContainerSize() + Inventory.MAIN_SIZE + col;
        }

        return -1;
    }

    private void handleClick(int slotIndex, boolean rightClick) {
        if (slotIndex == -1) {
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
            }
        } else {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(slotItem);
                setItemInSlot(slotIndex, null);
            } else if (cursorItem != null && slotItem == null) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(null);
            } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
                ItemStackOps.mergeInto(slotItem, cursorItem);
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
        if (slotIndex < getContainerSize()) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations())) {
                markChestsDirty();
            }
            return;
        }

        if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, ContainerSlotOrder.range(0, getContainerSize()))) {
            markChestsDirty();
        }
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(getContainerSize(), Inventory.MAIN_SIZE,
                Inventory.HOTBAR_SIZE);
    }

    public ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex < getContainerSize()) {
            return getContainerStack(slotIndex);
        }
        int playerIndex = slotIndex - getContainerSize();
        if (playerIndex < Inventory.MAIN_SIZE) {
            return inventory.getMainInventory()[playerIndex];
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        return hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE ? inventory.getHotbar()[hotbarIndex] : null;
    }

    private void setItemInSlot(int slotIndex, ItemStack stack) {
        if (slotIndex < getContainerSize()) {
            setContainerStack(slotIndex, stack);
            markChestsDirty();
            return;
        }
        int playerIndex = slotIndex - getContainerSize();
        if (playerIndex < Inventory.MAIN_SIZE) {
            inventory.getMainInventory()[playerIndex] = stack;
        } else {
            int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
            if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
                inventory.getHotbar()[hotbarIndex] = stack;
            }
        }
    }

    private ItemStack getContainerStack(int slotIndex) {
        if (minecartInventory != null) {
            return slotIndex >= 0 && slotIndex < minecartInventory.length ? minecartInventory[slotIndex] : null;
        }
        if (slotIndex < ChestTileEntity.SIZE) {
            return firstChest.getInventory()[slotIndex];
        }
        return secondChest != null ? secondChest.getInventory()[slotIndex - ChestTileEntity.SIZE] : null;
    }

    private void setContainerStack(int slotIndex, ItemStack stack) {
        if (minecartInventory != null) {
            if (slotIndex >= 0 && slotIndex < minecartInventory.length) {
                minecartInventory[slotIndex] = stack;
            }
            return;
        }
        if (slotIndex < ChestTileEntity.SIZE) {
            firstChest.getInventory()[slotIndex] = stack;
        } else if (secondChest != null) {
            secondChest.getInventory()[slotIndex - ChestTileEntity.SIZE] = stack;
        }
    }

    private void markChestsDirty() {
        if (minecartInventory != null) {
            minecartDirty = true;
            return;
        }
        if (firstChest != null) {
            firstChest.markDirty();
        }
        if (secondChest != null) {
            secondChest.markDirty();
        }
    }

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    private void markDraggedContainerSlotsDirty() {
        for (int slot : draggedSlots) {
            if (slot < getContainerSize()) {
                markChestsDirty();
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
                return slotIndex >= 0 && slotIndex < getContainerSize() + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE;
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

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getContainerRows() {
        return secondChest == null ? 3 : 6;
    }

    public int getContainerSize() {
        return getContainerRows() * COLS;
    }

    public int getTexMainInvY() {
        return 32 + getContainerRows() * 18;
    }

    public int getTexHotbarY() {
        return getTexMainInvY() + 58;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public ChestTileEntity getFirstChest() {
        return firstChest;
    }

    public ChestTileEntity getSecondChest() {
        return secondChest;
    }

    public ChestMinecartEntity getMinecart() {
        return minecart;
    }

    public boolean isMinecartDirty() {
        return minecartDirty;
    }

    public void clearMinecartDirty() {
        minecartDirty = false;
    }
}
