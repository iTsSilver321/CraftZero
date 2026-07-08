package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.progression.BookshelfPower;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.PlayerProgression;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.lwjgl.glfw.GLFW.*;

public class EnchantingTableScreen {
    public static final float GUI_SCALE = 2.0f;
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 166;
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;
    public static final int SLOT_SIZE = (int) (TEX_SLOT_SIZE * GUI_SCALE);
    public static final int ITEM_SIZE = (int) (TEX_ITEM_SIZE * GUI_SCALE);
    public static final int WINDOW_WIDTH = (int) (TEX_WIDTH * GUI_SCALE);
    public static final int WINDOW_HEIGHT = (int) (TEX_HEIGHT * GUI_SCALE);
    public static final int TEX_TABLE_SLOT_X = 25;
    public static final int TEX_TABLE_SLOT_Y = 47;
    public static final int TEX_OFFER_X = 60;
    public static final int TEX_OFFER_Y = 14;
    public static final int TEX_OFFER_W = 108;
    public static final int TEX_OFFER_H = 19;
    public static final int TEX_OFFER_U = 0;
    public static final int TEX_OFFER_NORMAL_V = 166;
    public static final int TEX_OFFER_DISABLED_V = 185;
    public static final int TEX_OFFER_HOVER_V = 204;
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;
    private static final String[] ENCHANTMENT_WORDS = ("the elder scrolls klaatu berata niktu xyzzy bless curse "
            + "light darkness fire air earth water hot dry cold wet ignite snuff embiggen twist shorten stretch "
            + "fiddle destroy imbue galvanize enchant free limited range of towards inside sphere cube self other "
            + "ball mental physical grow shrink demon elemental spirit animal creature beast humanoid undead "
            + "fresh stale").split(" ");

    private final Inventory inventory;
    private final List<ItemStack> itemsToThrow = new ArrayList<>();
    private final BooleanSupplier inventoryCloseRequested;
    private World world;
    private PlayerProgression progression;
    private Vector3i tablePos;
    private ItemStack tableItem;
    private boolean open;
    private int windowX;
    private int windowY;
    private int hoveredSlot = -1;
    private int hoveredOffer = -1;
    private boolean isMouseDragging;
    private boolean mouseDragRightClick;
    private int dragStartSlot = -1;
    private final Set<Integer> draggedSlots = new LinkedHashSet<>();
    private final ContainerDoubleClickTracker doubleClickTracker = new ContainerDoubleClickTracker();
    private int bookshelfPower;
    private int[] offers = new int[] { 0, 0, 0 };
    private String[] offerPhrases = new String[] { "", "", "" };
    private int lastOfferHash;
    private long offerSeed;
    private EnchantAction lastEnchantAction;
    private final BooleanSupplier dropRequested;

    public EnchantingTableScreen(Inventory inventory) {
        this(inventory, null, null);
    }

    public EnchantingTableScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested) {
        this(inventory, inventoryCloseRequested, null);
    }

    public EnchantingTableScreen(Inventory inventory, BooleanSupplier inventoryCloseRequested,
            BooleanSupplier dropRequested) {
        this.inventory = inventory;
        this.inventoryCloseRequested = ContainerScreenControls.closeRequester(inventoryCloseRequested);
        this.dropRequested = ContainerScreenControls.dropRequester(dropRequested);
    }

    public void open(World world, Vector3i tablePos, PlayerProgression progression, int screenWidth, int screenHeight) {
        if (open) {
            close();
        }
        this.world = world;
        this.tablePos = new Vector3i(tablePos);
        this.progression = progression;
        this.open = true;
        this.windowX = (screenWidth - WINDOW_WIDTH) / 2;
        this.windowY = (screenHeight - WINDOW_HEIGHT) / 2;
        this.hoveredSlot = -1;
        this.hoveredOffer = -1;
        this.lastEnchantAction = null;
        rerollOfferSeed();
        updateOffers(true);
        Input.setCursorLocked(false);
    }

    public void close() {
        if (!open) {
            return;
        }
        if (tableItem != null && !tableItem.isEmpty()) {
            itemsToThrow.add(tableItem);
        }
        tableItem = null;
        if (inventory.getCursorItem() != null) {
            itemsToThrow.add(inventory.getCursorItem());
            inventory.setCursorItem(null);
        }
        open = false;
        world = null;
        progression = null;
        tablePos = null;
        hoveredSlot = -1;
        hoveredOffer = -1;
        lastEnchantAction = null;
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
        hoveredOffer = getOfferAtPosition((int) Input.getMouseX(), (int) Input.getMouseY());
        updateOffers(false);
        if (ContainerKeyboardDrop.dropOne(dropRequested, inventory, dragSlotAccess(), hoveredSlot,
                itemsToThrow).dropped()) {
            updateOffers(true);
            return;
        }
        if (ContainerHotbarSwap.trySwapWithHotbar(inventory, dragSlotAccess(), hoveredSlot,
                1 + Inventory.MAIN_SIZE)) {
            return;
        }
        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT) && hoveredOffer >= 0) {
            enchant(hoveredOffer);
        } else {
            handleMouseButton(GLFW_MOUSE_BUTTON_LEFT, false);
        }
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
        return ContainerDoubleClickCollector.collectMatching(dragSlotAccess(), doubleClickCollectSlots(slotIndex),
                inventory.getCursorItem());
    }

    private boolean canHandleDoubleClick(int slotIndex) {
        return !ItemStackOps.isEmpty(inventory.getCursorItem())
                && slotIndex >= 0 && slotIndex <= Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE;
    }

    private int[] doubleClickCollectSlots(int clickedSlot) {
        int[] tableSlot = ContainerSlotOrder.range(0, 1);
        int[] playerSlots = ContainerSlotOrder.range(1, 1 + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE);
        return ContainerSlotOrder.clickedGroupFirst(clickedSlot, 0, 1, tableSlot, playerSlots);
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

    public boolean isStillUsable(Player player) {
        return open && BlockContainerValidity.sameBlockWithinUseDistance(world, tablePos, player,
                BlockType.ENCHANTING_TABLE);
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
            } else if (cursorItem != null && slotItem == null && canPlace(slotIndex, cursorItem)) {
                setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (canPlace(slotIndex, cursorItem) && mergeIntoSlot(slotIndex, slotItem, cursorItem, 1) > 0) {
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
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
        if (slotIndex == 0) {
            if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, playerInventoryShiftClickDestinations())) {
                updateOffers(true);
            }
            return;
        }
        if (ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex, new int[] { 0 })) {
            return;
        }
        moveWithinPlayerInventory(slotIndex);
    }

    private boolean moveWithinPlayerInventory(int slotIndex) {
        int playerIndex = slotIndex - 1;
        if (playerIndex < 0) {
            return false;
        }
        if (playerIndex < Inventory.MAIN_SIZE) {
            return ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(1 + Inventory.MAIN_SIZE,
                            1 + Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE));
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
            return ContainerQuickMove.moveSlot(dragSlotAccess(), slotIndex,
                    ContainerSlotOrder.range(1, 1 + Inventory.MAIN_SIZE));
        }
        return false;
    }

    private int[] playerInventoryShiftClickDestinations() {
        return ContainerSlotOrder.playerInventoryReverse(1, Inventory.MAIN_SIZE, Inventory.HOTBAR_SIZE);
    }

    private boolean canPlace(int slotIndex, ItemStack stack) {
        return slotIndex != 0 || (stack != null && !stack.isEmpty());
    }

    private boolean isShiftDown() {
        return Input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || Input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
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

    private int maxStackSizeForSlot(int slotIndex, ItemStack stack) {
        if (slotIndex == 0) {
            return 1;
        }
        return stack == null || stack.isEmpty() ? 0 : stack.getMaxStackSize();
    }

    private void enchant(int offerSlot) {
        if (progression == null || tableItem == null || tableItem.isEmpty()
                || offerSlot < 0 || offerSlot >= offers.length) {
            return;
        }
        int cost = offers[offerSlot];
        if (cost <= 0 || progression.getLevel() < cost) {
            return;
        }
        ItemStack inputItem = tableItem.copy();
        long seed = offerSeed;
        Random random = offerRandom(offerSlot);
        List<EnchantmentInstance> enchantments = EnchantmentResolver.generate(random, tableItem, cost);
        if (enchantments.isEmpty()) {
            return;
        }
        if (!progression.consumeLevels(cost)) {
            return;
        }
        tableItem.setEnchantments(enchantments);
        lastEnchantAction = new EnchantAction(tablePos, offerSlot, cost, seed, inputItem, tableItem);
        updateOffers(true);
    }

    private void updateOffers(boolean force) {
        if (world == null || tablePos == null) {
            offers = new int[] { 0, 0, 0 };
            clearOfferPhrases();
            return;
        }
        bookshelfPower = BookshelfPower.count(world, tablePos.x, tablePos.y, tablePos.z);
        int hash = Objects.hash(stackOfferKey(tableItem), bookshelfPower, offerSeed);
        if (!force && hash == lastOfferHash) {
            return;
        }
        lastOfferHash = hash;
        for (int i = 0; i < offers.length; i++) {
            offers[i] = EnchantmentResolver.offerCost(offerRandom(i), i, bookshelfPower, tableItem);
        }
        updateOfferPhrases();
    }

    private Random offerRandom(int slot) {
        long seed = 0x5DEECE66DL ^ offerSeed;
        if (tablePos != null) {
            seed ^= tablePos.x * 341873128712L;
            seed ^= tablePos.y * 132897987541L;
            seed ^= tablePos.z * 42317861L;
        }
        seed ^= (long) stackOfferKey(tableItem) * 31L;
        seed ^= slot * 0x9E3779B97F4A7C15L;
        return new Random(seed);
    }

    private void rerollOfferSeed() {
        offerSeed = world == null ? 0L : world.getRandom().nextLong();
    }

    private void updateOfferPhrases() {
        Random random = new Random(offerSeed);
        for (int i = 0; i < offerPhrases.length; i++) {
            String phrase = generateOfferPhrase(random);
            offerPhrases[i] = offers[i] > 0 ? phrase : "";
        }
    }

    private void clearOfferPhrases() {
        offerPhrases = new String[] { "", "", "" };
    }

    public static String generateOfferPhrase(Random random) {
        Random source = random == null ? new Random(0L) : random;
        int words = source.nextInt(3) + 3;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(ENCHANTMENT_WORDS[source.nextInt(ENCHANTMENT_WORDS.length)]);
        }
        return out.toString();
    }

    public static int offerTextureV(int cost, int playerLevel, boolean hovered) {
        if (cost <= 0 || playerLevel < cost) {
            return TEX_OFFER_DISABLED_V;
        }
        return hovered ? TEX_OFFER_HOVER_V : TEX_OFFER_NORMAL_V;
    }

    private int stackOfferKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return Objects.hash(stack.getType(), stack.getDurability(), stack.getEnchantments(), stack.getPotionData(),
                stack.getMetadata());
    }

    private int getSlotAtPosition(int mx, int my) {
        if (mx < windowX || mx >= windowX + WINDOW_WIDTH || my < windowY || my >= windowY + WINDOW_HEIGHT) {
            return -1;
        }
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;
        if (inSlot(texX, texY, TEX_TABLE_SLOT_X, TEX_TABLE_SLOT_Y)) {
            return 0;
        }
        if (texX >= TEX_MAIN_INV_X && texX < TEX_MAIN_INV_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_MAIN_INV_Y && texY < TEX_MAIN_INV_Y + MAIN_ROWS * TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_MAIN_INV_X) / TEX_SLOT_SIZE);
            int row = (int) ((texY - TEX_MAIN_INV_Y) / TEX_SLOT_SIZE);
            return 1 + row * COLS + col;
        }
        if (texX >= TEX_HOTBAR_X && texX < TEX_HOTBAR_X + COLS * TEX_SLOT_SIZE
                && texY >= TEX_HOTBAR_Y && texY < TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            int col = (int) ((texX - TEX_HOTBAR_X) / TEX_SLOT_SIZE);
            return 1 + Inventory.MAIN_SIZE + col;
        }
        return -1;
    }

    private int getOfferAtPosition(int mx, int my) {
        float texX = (mx - windowX) / GUI_SCALE;
        float texY = (my - windowY) / GUI_SCALE;
        if (texX < TEX_OFFER_X || texX >= TEX_OFFER_X + TEX_OFFER_W) {
            return -1;
        }
        for (int i = 0; i < 3; i++) {
            int y = TEX_OFFER_Y + i * TEX_OFFER_H;
            if (texY >= y && texY < y + TEX_OFFER_H) {
                return i;
            }
        }
        return -1;
    }

    private boolean inSlot(float texX, float texY, int slotX, int slotY) {
        return texX >= slotX && texX < slotX + TEX_SLOT_SIZE
                && texY >= slotY && texY < slotY + TEX_SLOT_SIZE;
    }

    public ItemStack getItemInSlot(int slotIndex) {
        if (slotIndex == 0) {
            return tableItem;
        }
        int playerIndex = slotIndex - 1;
        if (playerIndex < Inventory.MAIN_SIZE) {
            return inventory.getMainInventory()[playerIndex];
        }
        int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
        return hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE ? inventory.getHotbar()[hotbarIndex] : null;
    }

    private void setItemInSlot(int slotIndex, ItemStack stack) {
        if (slotIndex == 0) {
            tableItem = stack;
            rerollOfferSeed();
            updateOffers(true);
            return;
        }
        int playerIndex = slotIndex - 1;
        if (playerIndex < Inventory.MAIN_SIZE) {
            inventory.getMainInventory()[playerIndex] = stack;
        } else {
            int hotbarIndex = playerIndex - Inventory.MAIN_SIZE;
            if (hotbarIndex >= 0 && hotbarIndex < Inventory.HOTBAR_SIZE) {
                inventory.getHotbar()[hotbarIndex] = stack;
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
                return slotIndex >= 0 && slotIndex <= Inventory.MAIN_SIZE + Inventory.HOTBAR_SIZE
                        && EnchantingTableScreen.this.canPlace(slotIndex, stack);
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

    public int getHoveredOffer() {
        return hoveredOffer;
    }

    public int getWindowX() {
        return windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public int[] getOffers() {
        return offers;
    }

    public String[] getOfferPhrases() {
        return offerPhrases.clone();
    }

    public String getOfferPhrase(int slot) {
        return slot >= 0 && slot < offerPhrases.length ? offerPhrases[slot] : "";
    }

    public int getBookshelfPower() {
        return bookshelfPower;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public PlayerProgression getProgression() {
        return progression;
    }

    public EnchantAction drainEnchantAction() {
        EnchantAction action = lastEnchantAction;
        lastEnchantAction = null;
        return action;
    }

    public boolean isAtTable(int x, int y, int z) {
        return tablePos != null && tablePos.x == x && tablePos.y == y && tablePos.z == z;
    }

    public void applyRemoteEnchantResult(int x, int y, int z, ItemStack resultItem) {
        if (!open || !isAtTable(x, y, z)) {
            return;
        }
        tableItem = resultItem == null ? null : resultItem.copy();
        updateOffers(true);
    }

    public record EnchantAction(Vector3i tablePos, int offerSlot, int cost, long offerSeed,
            ItemStack inputItem, ItemStack resultItem) {
        public EnchantAction {
            tablePos = tablePos == null ? null : new Vector3i(tablePos);
            inputItem = inputItem == null ? null : inputItem.copy();
            resultItem = resultItem == null ? null : resultItem.copy();
        }
    }
}
