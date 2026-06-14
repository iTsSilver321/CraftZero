package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.world.tile.FuelRegistry;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.SmeltingRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private boolean isRightClickDragging;
    private final Set<Integer> draggedSlots = new HashSet<>();
    private final List<ItemStack> itemsToThrow = new ArrayList<>();

    public FurnaceScreen(Inventory inventory) {
        this.inventory = inventory;
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

        if (Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
            shiftClick(slotIndex, slotItem);
            return;
        }

        if (slotIndex == FurnaceTileEntity.SLOT_OUTPUT && cursorItem != null
                && !ItemStackOps.canMerge(cursorItem, slotItem)) {
            return;
        }

        if (rightClick) {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(ItemStackOps.splitHalf(slotItem));
                if (slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                }
            } else if (cursorItem != null && slotItem == null && slotIndex != FurnaceTileEntity.SLOT_OUTPUT) {
                setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (ItemStackOps.mergeAmountInto(slotItem, cursorItem, 1) > 0) {
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
                markFurnaceDirtyIfNeeded(slotIndex);
            }
        } else {
            if (cursorItem == null && slotItem != null) {
                inventory.setCursorItem(slotItem);
                setItemInSlot(slotIndex, null);
            } else if (cursorItem != null && slotItem == null && slotIndex != FurnaceTileEntity.SLOT_OUTPUT) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(null);
            } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
                ItemStackOps.mergeInto(slotItem, cursorItem);
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
                markFurnaceDirtyIfNeeded(slotIndex);
            } else if (cursorItem != null && slotIndex != FurnaceTileEntity.SLOT_OUTPUT) {
                setItemInSlot(slotIndex, cursorItem);
                inventory.setCursorItem(slotItem);
            }
        }
    }

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }
        if (slotIndex < FurnaceTileEntity.SIZE) {
            if (inventory.addItem(slotItem)) {
                setItemInSlot(slotIndex, null);
            }
            return;
        }

        if (FuelRegistry.isFuel(slotItem)) {
            if (moveIntoFurnaceSlot(slotItem, FurnaceTileEntity.SLOT_FUEL)) {
                setItemInSlot(slotIndex, null);
            }
        } else if (SmeltingRegistry.getResult(slotItem) != null) {
            if (moveIntoFurnaceSlot(slotItem, FurnaceTileEntity.SLOT_INPUT)) {
                setItemInSlot(slotIndex, null);
            }
        }
    }

    private boolean moveIntoFurnaceSlot(ItemStack stack, int targetSlot) {
        ItemStack target = furnace.getInventory()[targetSlot];
        if (target == null) {
            int toMove = Math.min(stack.getMaxStackSize(), stack.getCount());
            furnace.getInventory()[targetSlot] = ItemStackOps.split(stack, toMove);
            furnace.markDirty();
            return stack.isEmpty();
        }
        if (ItemStackOps.mergeInto(target, stack) > 0) {
            furnace.markDirty();
        }
        return stack.isEmpty();
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
}
