package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.progression.BookshelfPower;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentResolver;
import com.craftzero.progression.PlayerProgression;
import com.craftzero.world.World;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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
    public static final int TEX_MAIN_INV_X = 8;
    public static final int TEX_MAIN_INV_Y = 84;
    public static final int TEX_HOTBAR_X = 8;
    public static final int TEX_HOTBAR_Y = 142;
    public static final int COLS = 9;
    public static final int MAIN_ROWS = 3;

    private final Inventory inventory;
    private final List<ItemStack> itemsToThrow = new ArrayList<>();
    private World world;
    private PlayerProgression progression;
    private Vector3i tablePos;
    private ItemStack tableItem;
    private boolean open;
    private int windowX;
    private int windowY;
    private int hoveredSlot = -1;
    private int hoveredOffer = -1;
    private int bookshelfPower;
    private int[] offers = new int[] { 0, 0, 0 };
    private int lastOfferHash;

    public EnchantingTableScreen(Inventory inventory) {
        this.inventory = inventory;
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
        updateOffers(true);
        Input.setCursorLocked(false);
    }

    public void close() {
        if (!open) {
            return;
        }
        if (tableItem != null && !tableItem.isEmpty() && !inventory.addItem(tableItem)) {
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
        hoveredOffer = getOfferAtPosition((int) Input.getMouseX(), (int) Input.getMouseY());
        updateOffers(false);
        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            if (hoveredOffer >= 0) {
                enchant(hoveredOffer);
            } else if (hoveredSlot == -1 && inventory.getCursorItem() != null) {
                itemsToThrow.add(inventory.getCursorItem());
                inventory.setCursorItem(null);
            } else {
                handleClick(hoveredSlot, false);
            }
        }
        if (Input.isButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            handleClick(hoveredSlot, true);
        }
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
            } else if (cursorItem != null && slotItem == null && canPlace(slotIndex, cursorItem)) {
                setItemInSlot(slotIndex, ItemStackOps.splitOne(cursorItem));
                if (cursorItem.isEmpty()) {
                    inventory.setCursorItem(null);
                }
            } else if (ItemStackOps.mergeAmountInto(slotItem, cursorItem, 1) > 0) {
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
            setItemInSlot(slotIndex, cursorItem);
            inventory.setCursorItem(null);
        } else if (ItemStackOps.canMerge(slotItem, cursorItem)) {
            ItemStackOps.mergeInto(slotItem, cursorItem);
            if (cursorItem.isEmpty()) {
                inventory.setCursorItem(null);
            }
        } else if (cursorItem != null && canPlace(slotIndex, cursorItem)) {
            setItemInSlot(slotIndex, cursorItem);
            inventory.setCursorItem(slotItem);
        }
    }

    private void shiftClick(int slotIndex, ItemStack slotItem) {
        if (slotItem == null || slotItem.isEmpty()) {
            return;
        }
        if (slotIndex == 0) {
            if (inventory.addItem(slotItem)) {
                tableItem = null;
            }
            return;
        }
        if (tableItem == null && EnchantmentResolver.isEnchantable(slotItem)) {
            tableItem = ItemStackOps.split(slotItem, 1);
            if (slotItem.isEmpty()) {
                setItemInSlot(slotIndex, null);
            }
        }
    }

    private boolean canPlace(int slotIndex, ItemStack stack) {
        return slotIndex != 0 || EnchantmentResolver.isEnchantable(stack);
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
        Random random = offerRandom(offerSlot);
        List<EnchantmentInstance> enchantments = EnchantmentResolver.generate(random, tableItem, cost);
        if (enchantments.isEmpty()) {
            return;
        }
        if (!progression.consumeLevels(cost)) {
            return;
        }
        tableItem.setEnchantments(enchantments);
        updateOffers(true);
    }

    private void updateOffers(boolean force) {
        if (world == null || tablePos == null) {
            offers = new int[] { 0, 0, 0 };
            return;
        }
        bookshelfPower = BookshelfPower.count(world, tablePos.x, tablePos.y, tablePos.z);
        int hash = Objects.hash(stackOfferKey(tableItem), bookshelfPower);
        if (!force && hash == lastOfferHash) {
            return;
        }
        lastOfferHash = hash;
        for (int i = 0; i < offers.length; i++) {
            offers[i] = EnchantmentResolver.offerCost(offerRandom(i), i, bookshelfPower, tableItem);
        }
    }

    private Random offerRandom(int slot) {
        long seed = 0x5DEECE66DL;
        if (tablePos != null) {
            seed ^= tablePos.x * 341873128712L;
            seed ^= tablePos.y * 132897987541L;
            seed ^= tablePos.z * 42317861L;
        }
        seed ^= (long) stackOfferKey(tableItem) * 31L;
        seed ^= slot * 0x9E3779B97F4A7C15L;
        return new Random(seed);
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

    public int getBookshelfPower() {
        return bookshelfPower;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public PlayerProgression getProgression() {
        return progression;
    }
}
