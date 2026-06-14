package com.craftzero.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackOpsTest {

    @Test
    @DisplayName("Stack split should preserve metadata on the split stack")
    void splitPreservesMetadata() {
        ItemStack source = namedStack(ItemType.DIAMOND, 7, "keepsake");
        source.putMetadata("origin", "test");

        ItemStack split = ItemStackOps.split(source, 3);

        assertEquals(4, source.getCount());
        assertEquals(3, split.getCount());
        assertSame(ItemType.DIAMOND, split.getType());
        assertEquals("keepsake", split.getCustomName());
        assertEquals("test", split.getMetadata().get("origin"));
        assertTrue(source.canMergeWith(split));
    }

    @Test
    @DisplayName("Stack movement should merge only matching metadata")
    void moveIntoSlotsRespectsMetadataIdentity() {
        ItemStack[] slots = new ItemStack[2];
        slots[0] = namedStack(ItemType.DIAMOND, 63, "other");
        ItemStack incoming = namedStack(ItemType.DIAMOND, 2, "keepsake");

        int moved = ItemStackOps.moveIntoSlots(SlotAccess.of(slots), incoming);

        assertEquals(2, moved);
        assertEquals(63, slots[0].getCount());
        assertEquals("other", slots[0].getCustomName());
        assertEquals(2, slots[1].getCount());
        assertEquals("keepsake", slots[1].getCustomName());
        assertTrue(incoming.isEmpty());
    }

    @Test
    @DisplayName("Stack movement should split oversized stacks into empty slots")
    void moveIntoSlotsSplitsOversizedStacks() {
        ItemStack[] slots = new ItemStack[2];
        ItemStack incoming = namedStack(ItemType.DIRT, 70, "pile");

        int moved = ItemStackOps.moveIntoSlots(SlotAccess.of(slots), incoming);

        assertEquals(70, moved);
        assertEquals(64, slots[0].getCount());
        assertEquals(6, slots[1].getCount());
        assertEquals("pile", slots[0].getCustomName());
        assertEquals("pile", slots[1].getCustomName());
        assertTrue(incoming.isEmpty());
    }

    @Test
    @DisplayName("Inventory capacity check should honor stack metadata")
    void inventoryCapacityCheckHonorsMetadata() {
        Inventory inventory = new Inventory();
        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            inventory.getHotbar()[i] = namedStack(ItemType.DIAMOND, 1, "other");
        }
        for (int i = 0; i < Inventory.MAIN_SIZE; i++) {
            inventory.getMainInventory()[i] = namedStack(ItemType.DIAMOND, 1, "other");
        }

        assertFalse(inventory.canAddItem(namedStack(ItemType.DIAMOND, 1, "keepsake")));
        assertTrue(inventory.canAddItem(namedStack(ItemType.DIAMOND, 1, "other")));
    }

    private static ItemStack namedStack(ItemType type, int count, String name) {
        ItemStack stack = new ItemStack(type, count);
        stack.setCustomName(name);
        return stack;
    }
}
