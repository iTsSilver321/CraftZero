package com.craftzero.ui.menu;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreativeInventoryScreenTest {

    @Test
    @DisplayName("Creative item grid should create a full cursor stack")
    void gridClickCreatesCursorStack() {
        Inventory inventory = new Inventory();
        CreativeInventoryScreen screen = new CreativeInventoryScreen(inventory, 400, 240, () -> {
        });

        screen.update(input(screen, CreativeInventoryScreen.TEX_GRID_X + 1,
                CreativeInventoryScreen.TEX_GRID_Y + 1, true, 0.0));

        ItemStack cursor = inventory.getCursorItem();
        assertSame(ItemType.STONE, cursor.getType());
        assertEquals(ItemType.STONE.getMaxStackSize(), cursor.getCount());
    }

    @Test
    @DisplayName("Creative hotbar row should accept and swap cursor stacks")
    void hotbarClickPlacesAndSwapsCursorStacks() {
        Inventory inventory = new Inventory();
        CreativeInventoryScreen screen = new CreativeInventoryScreen(inventory, 400, 240, () -> {
        });

        inventory.setCursorItem(new ItemStack(ItemType.DIAMOND, 64));
        screen.update(input(screen, CreativeInventoryScreen.TEX_HOTBAR_X + 1,
                CreativeInventoryScreen.TEX_HOTBAR_Y + 1, true, 0.0));

        assertSame(ItemType.DIAMOND, inventory.getHotbar()[0].getType());
        assertNull(inventory.getCursorItem());

        inventory.setCursorItem(new ItemStack(ItemType.STICK, 64));
        screen.update(input(screen, 0, 0, false, 0.0));
        screen.update(input(screen, CreativeInventoryScreen.TEX_HOTBAR_X + 1,
                CreativeInventoryScreen.TEX_HOTBAR_Y + 1, true, 0.0));

        assertSame(ItemType.STICK, inventory.getHotbar()[0].getType());
        assertSame(ItemType.DIAMOND, inventory.getCursorItem().getType());
    }

    @Test
    @DisplayName("Creative inventory scrolls by rows and clamps to valid range")
    void scrollsAndClamps() {
        Inventory inventory = new Inventory();
        CreativeInventoryScreen screen = new CreativeInventoryScreen(inventory, 400, 240, () -> {
        });

        ItemType first = screen.itemAtVisibleSlot(0);
        screen.update(input(screen, 0, 0, false, -1.0));

        assertEquals(1, screen.scrollRow());
        assertNotSame(first, screen.itemAtVisibleSlot(0));

        screen.scroll(10_000);
        assertEquals(screen.maxScrollRow(), screen.scrollRow());

        screen.scroll(-10_000);
        assertEquals(0, screen.scrollRow());
        assertSame(first, screen.itemAtVisibleSlot(0));
    }

    private static MenuInput input(CreativeInventoryScreen screen, int relX, int relY, boolean leftPressed,
            double scrollY) {
        return new MenuInput(400, 240, screen.windowX() + relX, screen.windowY() + relY,
                leftPressed, scrollY, List.of(), List.of());
    }
}
