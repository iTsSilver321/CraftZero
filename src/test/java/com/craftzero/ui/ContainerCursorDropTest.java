package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerCursorDropTest {
    @Test
    @DisplayName("Left-clicking outside a container drops the whole cursor stack")
    void leftClickOutsideDropsWholeCursorStack() {
        Inventory inventory = new Inventory();
        List<ItemStack> dropped = new ArrayList<>();
        ItemStack cursor = new ItemStack(ItemType.COBBLESTONE, 12);
        inventory.setCursorItem(cursor);

        assertTrue(ContainerCursorDrop.dropOutside(inventory, dropped, false));

        assertNull(inventory.getCursorItem());
        assertEquals(1, dropped.size());
        assertSame(cursor, dropped.get(0));
        assertEquals(12, dropped.get(0).getCount());
    }

    @Test
    @DisplayName("Right-clicking outside a container drops one cursor item")
    void rightClickOutsideDropsOneCursorItem() {
        Inventory inventory = new Inventory();
        List<ItemStack> dropped = new ArrayList<>();
        inventory.setCursorItem(new ItemStack(ItemType.COBBLESTONE, 12));

        assertTrue(ContainerCursorDrop.dropOutside(inventory, dropped, true));

        assertEquals(11, inventory.getCursorItem().getCount());
        assertEquals(1, dropped.size());
        assertSame(ItemType.COBBLESTONE, dropped.get(0).getType());
        assertEquals(1, dropped.get(0).getCount());
    }

    @Test
    @DisplayName("Right-clicking outside with one cursor item clears the cursor")
    void rightClickOutsideLastItemClearsCursor() {
        Inventory inventory = new Inventory();
        List<ItemStack> dropped = new ArrayList<>();
        inventory.setCursorItem(new ItemStack(ItemType.STICK, 1));

        assertTrue(ContainerCursorDrop.dropOutside(inventory, dropped, true));

        assertNull(inventory.getCursorItem());
        assertEquals(1, dropped.size());
        assertSame(ItemType.STICK, dropped.get(0).getType());
        assertEquals(1, dropped.get(0).getCount());
    }
}
