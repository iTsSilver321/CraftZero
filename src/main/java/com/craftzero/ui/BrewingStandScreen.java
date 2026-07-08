package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.BrewingRecipeRegistry;
import com.craftzero.world.tile.BrewingStandTileEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.*;

public class BrewingStandScreen {
    public static final float GUI_SCALE = 2.0f;
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 166;
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE);
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE);
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE);
    public static final int WINDOW_HEIGHT = (int) (TEX_HEIGHT * GUI_SCALE);
    public static final int TEX_INGREDIENT_X = 79;
    public static final int TEX_INGREDIENT_Y = 17;
    public static final int TEX_BOTTLE_0_X = 56;
    public static final int TEX_BOTTLE_0_Y = 51;
    public static final int TEX_BOTTLE_1_X = 79;
    public static final int TEX_BOTTLE_1_Y = 58;
    public static final int TEX_BOTTLE_2_X = 102;
    public static final int TEX_BOTTLE_2_Y = 51;
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;

    private final Inventory inventory;
    private BrewingStandTileEntity brewingStand;
    private AchievementTracker achievementTracker;
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

    public BrewingStandScreen(Inventory inventory) {
        this(inventory, null, null);
    }

    public BrewingStandScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested) {
        this(inventory, inventoryCloseRequested, null);
    }

    public BrewingStandScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested,
            BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.inventoryCloseRequested = ContainerScreenControls.closeRequester(inventoryCloseRequested);
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    public void open(BrewingStandTileEntity brewingStand, int screenWidth, int screenHeight) {
        open(brewingStand, screenWidth, screenHeight, null);
    }

    public void open(BrewingStandTileEntity brewingStand, int screenWidth, int screenHeight,
            AchievementTracker achievementTracker) {
        if (open) {
            close();
        }
        this.brewingStand = brewingStand;
        this.achievementTracker = achievementTracker;
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
        brewingStand = null;
        achievementTracker = null;
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
            markDirtyIfNeeded(keyboardDrop.sourceSlot());
            return;
        }
        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot,
                BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE)) {
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
            brewingStand.markDirty();
        }
        return collected;
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return brewingStand != null && !ItemStackOps.isEmpty(inventory.getCursorItem())
                && slotIndex >= 0
                && slotIndex < BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE;
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int[] brewingSlots = ContainerSlotOrder.range(0, BrewingStandTileEntity.SIZE);
        int[] playerSlots = ContainerSlotOrder.range(BrewingStandTileEntity.SIZE,
                BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot, 0, BrewingStandTileEntity.SIZE,
                brewingSlots, playerSlots);
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
                markDraggedBrewingSlotsDirty();
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
        return open && BlockContainerValidity.sameTileWithinUseDistance(world, brewingStand, player,
                BlockType.BREWING_STAND);
    }

    private int getSlotAtPosition(int mx, int my) {
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH || my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;
        if (inSlot(texX, texY, TEX_BOTTLE_0_X, TEX_BOTTLE_0_Y)) {
            return BrewingStandTileEntity.SLOT_BOTTLE_0;
        }
        if (inSlot(texX, texY, TEX_BOTTLE_1_X, TEX_BOTTLE_1_Y)) {
            return BrewingStandTileEntity.SLOT_BOTTLE_1;
        }
        if (inSlot(texX, texY, TEX_BOTTLE_2_X, TEX_BOTTLE_2_Y)) {
            return BrewingStandTileEntity.SLOT_BOTTLE_2;
        }
        if (inSlot(texX, texY, TEX_INGREDIENT_X, TEX_INGREDIENT_Y)) {
            return BrewingStandTileEntity.SLOT_INGREDIENT;
        }
        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_MAIN_INV_Y && texY < TEX_MAIN_INV_Y + MAIN_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_MAIN_INV_Y) / TEX_SLOT_SIZE);
            return BrewingStandTileEntity.SIZE + row * COLS + col;
        }
        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_HOTBAR_Y && texY < TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            return BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE + col;
        }
        return -1;
    }

    private boolean inSlot(float texX, float texY, int slotX, int slotY) {
        return texX >= slotX && texX < slotX + TEX_SLOT_SIZE
                && texY >= slotY && texY < slotY + TEX_SLOT_SIZE;
    }

    private void handleClick(int slotIndex, boolean rightClick) {
        if (slotIndex == -1 || brewingStand == null) {
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
                recordBrewedPotionTaken(slotIndex, inventory.getCursorItem());
                if (slotItem.isEmpty()) {
                    setItemInSlot(slotIndex, null);
                }
            } else if (cursorItem != null && slotItem == null && canPlace(slotIndex, cursorItem)) {
                setItemInSlot(slotIndex,
                        ItemStackOps.split(cursorItem, Math.min(1, maxStackSizeForSlot(slotIndex, cursorItem))));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (canPlace(slotIndex, cursorItem) && mergeIntoSlot(slotIndex, slotItem, cursorItem, 1) > 0) {
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
                markDirtyIfNeeded(slotIndex);
            }
            return;
        }
        if (cursorItem == null && slotItem != null) {
            inventory.setCursorItem(slotItem);
            setItemInSlot(slotIndex, null);
        } else if (cursorItem != null && slotItem == null && canPlace(slotIndex, cursorItem)) {
            placeCursorIntoEmptySlot(slotIndex, cursorItem);
        } else if (canPlace(slotIndex, cursorItem) && mergeIntoSlot(slotIndex, slotItem, cursorItem, Integer.MAX_VALUE) > 0) {
            if (cursorItem.isEmpty()) {
                inventory.setCursorItem(null);
            }
            markDirtyIfNeeded(slotIndex);
        } else if (cursorItem != null && canPlace(slotIndex, cursorItem)
                && cursorItem.getCount() <= maxStackSizeForSlot(slotIndex, cursorItem)) {
            setItemInSlot(slotIndex, cursorItem);
            inventory.setCursorItem(slotItem);
        }
    }

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }
        if (slotIndex < BrewingStandTileEntity.SIZE) {
            int before = slotItem.getCount();
            ItemStack moved = slotItem.copy();
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations())) {
                moved.setCount(before - (slotItem.isEmpty() ? 0 : slotItem.getCount()));
                recordBrewedPotionTaken(slotIndex, moved);
                markDirtyIfNeeded(slotIndex);
            }
            return;
        }
        if (BrewingRecipeRegistry.isIngredient(slotItem)) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    new int[] { BrewingStandTileEntity.SLOT_INGREDIENT })) {
                brewingStand.markDirty();
            }
        } else if (isBottleSlotItem(slotItem)) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    new int[] { BrewingStandTileEntity.SLOT_BOTTLE_0, BrewingStandTileEntity.SLOT_BOTTLE_1,
                            BrewingStandTileEntity.SLOT_BOTTLE_2 })) {
                brewingStand.markDirty();
            }
        } else if (moveWithinPlayerInventory(slotIndex)) {
        }
    }

    private boolean moveWithinPlayerInventory(int slotIndex) {
        int playerIndex = slotIndex - BrewingStandTileEntity.SIZE;
        if (playerIndex < 0) {
            return false;
        }
        if (playerIndex < Inventory.MAIN_SIZE) {
            return ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE,
                            BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE));
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
            return ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(BrewingStandTileEntity.SIZE,
                            BrewingStandTileEntity.SIZE + Inventory.MAIN_SIZE));
        }
        return false;
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(BrewingStandTileEntity.SIZE, Inventory.MAIN_SIZE,
                Inventory.HOTBAR_SIZE);
    }

    private void placeCursorIntoEmptySlot(int slotIndex, ItemStack cursorItem) {
        int amount = Math.min(maxStackSizeForSlot(slotIndex, cursorItem), cursorItem.getCount());
        if (amount == cursorItem.getCount()) {
            setItemInSlot(slotIndex, cursorItem);
            inventory.setCursorItem(null);
            return;
        }
        setItemInSlot(slotIndex, ItemStackOps.split(cursorItem, amount));
        if (cursorItem.isEmpty()) {
            inventory.setCursorItem(null);
        }
    }

    private int mergeIntoSlot(int slotIndex, ItemStack target, ItemStack source, int amount) {
        if (amount <= 0 || !ItemStackOps.canMerge(target, source)) {
            return 0;
        }
        int max = Math.min(target.getMaxStackSize(), maxStackSizeForSlot(slotIndex, source));
        int space = max - target.getCount();
        if (space <= 0) {
            return 0;
        }
        int moved = Math.min(Math.min(space, source.getCount()), amount);
        target.add(moved);
        source.remove(moved);
        return moved;
    }

    private boolean canPlace(int slotIndex, ItemStack stack) {
        if (slotIndex >= BrewingStandTileEntity.SIZE) {
            return true;
        }
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (slotIndex == BrewingStandTileEntity.SLOT_INGREDIENT) {
            return BrewingRecipeRegistry.isIngredient(stack);
        }
        return isBottleSlotItem(stack);
    }

    private boolean isBottleSlotItem(ItemStack stack) {
        return BrewingRecipeRegistry.isBottleSlotItem(stack);
    }

    private int maxStackSizeForSlot(int slotIndex, ItemStack stack) {
        if (slotIndex >= 0 && slotIndex < BrewingStandTileEntity.SLOT_INGREDIENT) {
            return 1;
        }
        return stack == null || stack.isEmpty() ? 0 : stack.getMaxStackSize();
    }

    public ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex < BrewingStandTileEntity.SIZE) {
            return brewingStand.getInventory()[slotIndex];
        }
        int playerIndex = slotIndex - BrewingStandTileEntity.SIZE;
        if (playerIndex < Inventory.MAIN_SIZE) {
            return inventory.getMainInventory()[playerIndex];
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        return hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE ? inventory.getHotbar()[hotbarIndex] : null;
    }

    private void setItemInSlot(int slotIndex, ItemStack stack) {
        if (slotIndex < BrewingStandTileEntity.SIZE) {
            ItemStack previous = brewingStand.getInventory()[slotIndex];
            brewingStand.getInventory()[slotIndex] = stack;
            if (previous != stack) {
                recordBrewedPotionTaken(slotIndex, previous);
            }
            brewingStand.markDirty();
            return;
        }
        int playerIndex = slotIndex - BrewingStandTileEntity.SIZE;
        if (playerIndex < Inventory.MAIN_SIZE) {
            inventory.getMainInventory()[playerIndex] = stack;
        } else {
            int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
            if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
                inventory.getHotbar()[hotbarIndex] = stack;
            }
        }
    }

    private void markDirtyIfNeeded(int slotIndex) {
        if (slotIndex < BrewingStandTileEntity.SIZE) {
            brewingStand.markDirty();
        }
    }

    private void recordBrewedPotionTaken(int slotIndex, ItemStack stack) {
        if (achievementTracker == null || !isBottleSlot(slotIndex) || !isBrewedPotion(stack)) {
            return;
        }
        achievementTracker.recordBrewedPotionTaken();
    }

    private boolean isBottleSlot(int slotIndex) {
        return slotIndex >= BrewingStandTileEntity.SLOT_BOTTLE_0
                && slotIndex <= BrewingStandTileEntity.SLOT_BOTTLE_2;
    }

    private boolean isBrewedPotion(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getType() != ItemType.POTION) {
            return false;
        }
        PotionData potion = stack.getPotionData();
        return potion != null && potion.type() != PotionType.WATER;
    }

    public void setAchievementTracker(AchievementTracker achievementTracker) {
        this.achievementTracker = achievementTracker;
    }

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    private void markDraggedBrewingSlotsDirty() {
        for (int slot : draggedSlots) {
            if (slot < BrewingStandTileEntity.SIZE) {
                brewingStand.markDirty();
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
                return BrewingStandScreen.this.canPlace(slotIndex, stack);
            }

            @Override
            public int maxStackSize(int slotIndex, ItemStack stack) {
                return maxStackSizeForSlot(slotIndex, stack);
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

    public BrewingStandTileEntity getBrewingStand() {
        return brewingStand;
    }
}
