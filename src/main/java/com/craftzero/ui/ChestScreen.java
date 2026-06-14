package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.inventory.SlotAccess;
import com.craftzero.world.World;
import com.craftzero.world.tile.ChestTileEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final Inventory inventory;
    private boolean open;
    private ChestTileEntity firstChest;
    private ChestTileEntity secondChest;
    private int hoveredSlot = -1;
    private int windowX;
    private int windowY;
    private int windowHeight;
    private boolean isRightClickDragging;
    private final Set<Integer> draggedSlots = new HashSet<>();
    private final List<ItemStack> itemsToThrow = new ArrayList<>();

    public ChestScreen(Inventory inventory) {
        this.inventory = inventory;
    }

    public void open(World world, ChestTileEntity chest, int screenWidth, int screenHeight) {
        if (open) {
            close();
        }
        ChestTileEntity adjacent = world.getAdjacentChest(chest);
        if (adjacent != null && comesBefore(adjacent, chest)) {
            firstChest = adjacent;
            secondChest = chest;
        } else {
            firstChest = chest;
            secondChest = adjacent;
        }
        firstChest.open();
        if (secondChest != null) {
            secondChest.open();
        }
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
        open = false;
        hoveredSlot = -1;
        Input.setCursorLocked(true);
    }

    public void update() {
        if (!open) {
            return;
        }
        if (Input.isKeyPressed(GLFW_KEY_ESCAPE) || Input.isKeyPressed(GLFW_KEY_E)) {
            close();
            return;
        }

        hoveredSlot = getSlotAtPosition((int) Input.getMouseX(), (int) Input.getMouseY());

        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            if (hoveredSlot == -1 && inventory.getCursorItem() != null) {
                itemsToThrow.add(inventory.getCursorItem());
                inventory.setCursorItem(null);
            } else {
                handleClick(hoveredSlot, false);
            }
        }

        if (Input.isButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
            if (!isRightClickDragging) {
                isRightClickDragging = true;
                draggedSlots.clear();
                handleClick(hoveredSlot, true);
                if (hoveredSlot != -1) {
                    draggedSlots.add(hoveredSlot);
                }
            } else if (hoveredSlot != -1 && !draggedSlots.contains(hoveredSlot)) {
                handleClick(hoveredSlot, true);
                draggedSlots.add(hoveredSlot);
            }
        } else {
            isRightClickDragging = false;
            draggedSlots.clear();
        }
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

        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
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
            if (inventory.addItem(slotItem)) {
                setItemInSlot(slotIndex, null);
            }
            return;
        }

        if (addToContainer(slotItem)) {
            setItemInSlot(slotIndex, null);
        }
    }

    private boolean addToContainer(ItemStack stack) {
        int moved = ItemStackOps.mergeIntoSlots(SlotAccess.of(firstChest.getInventory()), stack);
        if (secondChest != null && !stack.isEmpty()) {
            moved += ItemStackOps.mergeIntoSlots(SlotAccess.of(secondChest.getInventory()), stack);
        }
        if (!stack.isEmpty()) {
            moved += ItemStackOps.placeIntoEmptySlots(SlotAccess.of(firstChest.getInventory()), stack);
        }
        if (secondChest != null && !stack.isEmpty()) {
            moved += ItemStackOps.placeIntoEmptySlots(SlotAccess.of(secondChest.getInventory()), stack);
        }
        if (moved > 0) {
            markChestsDirty();
        }
        return stack.isEmpty();
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
        if (slotIndex < ChestTileEntity.SIZE) {
            return firstChest.getInventory()[slotIndex];
        }
        return secondChest != null ? secondChest.getInventory()[slotIndex - ChestTileEntity.SIZE] : null;
    }

    private void setContainerStack(int slotIndex, ItemStack stack) {
        if (slotIndex < ChestTileEntity.SIZE) {
            firstChest.getInventory()[slotIndex] = stack;
        } else if (secondChest != null) {
            secondChest.getInventory()[slotIndex - ChestTileEntity.SIZE] = stack;
        }
    }

    private void markChestsDirty() {
        if (firstChest != null) {
            firstChest.markDirty();
        }
        if (secondChest != null) {
            secondChest.markDirty();
        }
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

}
