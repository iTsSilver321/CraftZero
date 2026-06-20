package com.craftzero.ui.menu;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.SlotAccess;

import java.util.List;

public class CreativeInventoryScreen implements Screen, ScreenManager.EscapeHandler {
    public static final int TEX_WIDTH = 176;
    public static final int TEX_HEIGHT = 207;
    public static final int TEX_GRID_X = 7;
    public static final int TEX_GRID_Y = 17;
    public static final int TEX_HOTBAR_X = 7;
    public static final int TEX_HOTBAR_Y = 183;
    public static final int TEX_SCROLL_X = 155;
    public static final int TEX_SCROLL_Y = 18;
    public static final int TEX_SCROLL_HEIGHT = 160;
    public static final int TEX_SCROLL_THUMB_HEIGHT = 15;
    public static final int TEX_SLOT_SIZE = 18;
    public static final int TEX_ITEM_SIZE = 16;
    public static final int GRID_COLS = 8;
    public static final int GRID_ROWS = 9;
    public static final int GRID_SLOT_COUNT = GRID_COLS * GRID_ROWS;
    public static final int HOTBAR_COLS = 9;

    private final Inventory inventory;
    private final Runnable closeAction;
    private final List<CreativeCatalogEntry> items;
    private int width;
    private int height;
    private int windowX;
    private int windowY;
    private int scrollRow;
    private int hoveredCreativeSlot = -1;
    private int hoveredHotbarSlot = -1;
    private boolean wasLeftPressed;
    private boolean draggingScrollbar;

    public CreativeInventoryScreen(Inventory inventory, int width, int height, Runnable closeAction) {
        this.inventory = inventory;
        this.closeAction = closeAction;
        this.items = CreativeCatalog.entries();
        resize(width, height);
    }

    @Override
    public void update(MenuInput input) {
        if (input == null) {
            return;
        }

        if (input.width() != width || input.height() != height) {
            resize(input.width(), input.height());
        }

        int mouseX = (int) Math.round(input.mouseX());
        int mouseY = (int) Math.round(input.mouseY());
        updateHover(mouseX, mouseY);

        if (input.scrollY() > 0.0) {
            scroll(-1);
        } else if (input.scrollY() < 0.0) {
            scroll(1);
        }

        boolean leftPressed = input.leftPressed();
        if (leftPressed) {
            if (draggingScrollbar || (!wasLeftPressed && scrollbarAt(mouseX, mouseY))) {
                draggingScrollbar = true;
                setScrollFromMouse(mouseY);
            }
        } else {
            draggingScrollbar = false;
        }

        boolean clicked = leftPressed && !wasLeftPressed && !draggingScrollbar;
        wasLeftPressed = leftPressed;
        if (clicked) {
            handleLeftClick(mouseX, mouseY);
        }
    }

    @Override
    public void render(MenuRenderer renderer, MenuInput input, float deltaTime) {
        renderer.drawRect(0, 0, input.width(), input.height(), 0.0f, 0.0f, 0.0f, 0.42f);
    }

    public void resize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.windowX = (this.width - TEX_WIDTH) / 2;
        this.windowY = (this.height - TEX_HEIGHT) / 2;
        clampScroll();
    }

    private void updateHover(int mouseX, int mouseY) {
        hoveredCreativeSlot = creativeSlotAt(mouseX, mouseY);
        hoveredHotbarSlot = hotbarSlotAt(mouseX, mouseY);
    }

    private void handleLeftClick(int mouseX, int mouseY) {
        int creativeSlot = creativeSlotAt(mouseX, mouseY);
        if (creativeSlot >= 0) {
            ItemStack stack = stackAtVisibleSlot(creativeSlot);
            if (stack != null) {
                inventory.setCursorItem(stack);
            }
            return;
        }

        int hotbarSlot = hotbarSlotAt(mouseX, mouseY);
        if (hotbarSlot >= 0) {
            ItemStack cursor = inventory.getCursorItem();
            SlotAccess hotbar = SlotAccess.of(inventory.getHotbar());
            ItemStack existing = hotbar.get(hotbarSlot);
            if (cursor == null || cursor.isEmpty()) {
                inventory.setCursorItem(existing);
                hotbar.set(hotbarSlot, null);
            } else {
                hotbar.set(hotbarSlot, cursor.copy());
                inventory.setCursorItem(existing);
            }
            return;
        }

        if (inventory.getCursorItem() != null) {
            inventory.setCursorItem(null);
        }
    }

    private int creativeSlotAt(int mouseX, int mouseY) {
        int relX = mouseX - windowX;
        int relY = mouseY - windowY;
        if (relX < TEX_GRID_X || relX >= TEX_GRID_X + GRID_COLS * TEX_SLOT_SIZE
                || relY < TEX_GRID_Y || relY >= TEX_GRID_Y + GRID_ROWS * TEX_SLOT_SIZE) {
            return -1;
        }
        int col = (relX - TEX_GRID_X) / TEX_SLOT_SIZE;
        int row = (relY - TEX_GRID_Y) / TEX_SLOT_SIZE;
        int visibleSlot = row * GRID_COLS + col;
        return itemAtVisibleSlot(visibleSlot) == null ? -1 : visibleSlot;
    }

    private int hotbarSlotAt(int mouseX, int mouseY) {
        int relX = mouseX - windowX;
        int relY = mouseY - windowY;
        if (relX < TEX_HOTBAR_X || relX >= TEX_HOTBAR_X + HOTBAR_COLS * TEX_SLOT_SIZE
                || relY < TEX_HOTBAR_Y || relY >= TEX_HOTBAR_Y + TEX_SLOT_SIZE) {
            return -1;
        }
        return (relX - TEX_HOTBAR_X) / TEX_SLOT_SIZE;
    }

    public void scroll(int rows) {
        scrollRow += rows;
        clampScroll();
    }

    private boolean scrollbarAt(int mouseX, int mouseY) {
        if (maxScrollRow() <= 0) {
            return false;
        }
        int relX = mouseX - windowX;
        int relY = mouseY - windowY;
        return relX >= TEX_SCROLL_X - 1 && relX < TEX_SCROLL_X + 13
                && relY >= TEX_SCROLL_Y && relY < TEX_SCROLL_Y + TEX_SCROLL_HEIGHT;
    }

    private void setScrollFromMouse(int mouseY) {
        int max = maxScrollRow();
        if (max <= 0) {
            scrollRow = 0;
            return;
        }
        int relY = mouseY - windowY;
        int track = TEX_SCROLL_HEIGHT - TEX_SCROLL_THUMB_HEIGHT;
        int thumbCenter = relY - TEX_SCROLL_Y - TEX_SCROLL_THUMB_HEIGHT / 2;
        float normalized = Math.max(0.0f, Math.min(1.0f, thumbCenter / (float) track));
        scrollRow = Math.round(normalized * max);
        clampScroll();
    }

    private void clampScroll() {
        scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow));
    }

    public int maxScrollRow() {
        int totalRows = (items.size() + GRID_COLS - 1) / GRID_COLS;
        return Math.max(0, totalRows - GRID_ROWS);
    }

    public ItemType itemAtVisibleSlot(int visibleSlot) {
        ItemStack stack = stackAtVisibleSlot(visibleSlot);
        return stack == null ? null : stack.getType();
    }

    public ItemStack stackAtVisibleSlot(int visibleSlot) {
        if (visibleSlot < 0 || visibleSlot >= GRID_SLOT_COUNT) {
            return null;
        }
        int index = scrollRow * GRID_COLS + visibleSlot;
        return index >= 0 && index < items.size() ? items.get(index).createStack() : null;
    }

    public ItemStack stackFor(ItemType type) {
        if (type == null) {
            return null;
        }
        int count = type.getMaxStackSize() <= 1 || type.isDamageable() ? 1 : type.getMaxStackSize();
        return new ItemStack(type, count);
    }

    public ItemStack hoveredStack() {
        ItemStack creativeItem = stackAtVisibleSlot(hoveredCreativeSlot);
        if (creativeItem != null) {
            return creativeItem;
        }
        return hoveredHotbarSlot >= 0 ? inventory.getHotbar()[hoveredHotbarSlot] : null;
    }

    @Override
    public void onClosed() {
        ItemStack cursor = inventory.getCursorItem();
        if (cursor != null && inventory.addItem(cursor)) {
            inventory.setCursorItem(null);
        }
    }

    public Inventory inventory() {
        return inventory;
    }

    public int windowX() {
        return windowX;
    }

    public int windowY() {
        return windowY;
    }

    public int scrollRow() {
        return scrollRow;
    }

    public int hoveredCreativeSlot() {
        return hoveredCreativeSlot;
    }

    public int hoveredHotbarSlot() {
        return hoveredHotbarSlot;
    }

    @Override
    public boolean pausesGame() {
        return false;
    }

    public int scrollThumbTexY() {
        int max = maxScrollRow();
        if (max <= 0) {
            return TEX_SCROLL_Y;
        }
        int track = TEX_SCROLL_HEIGHT - TEX_SCROLL_THUMB_HEIGHT;
        return TEX_SCROLL_Y + Math.round(track * (scrollRow / (float) max));
    }

    @Override
    public boolean onEscape() {
        closeAction.run();
        return true;
    }
}
