package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInventoryStackTest {

    @Test
    @DisplayName("Dropping one item from hand should preserve stack metadata")
    void dropOneFromHandPreservesMetadata() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        ItemStack held = new ItemStack(ItemType.DIAMOND, 5);
        held.setCustomName("keepsake");
        held.putMetadata("origin", "test");
        player.getInventory().getHotbar()[0] = held;

        ItemStack dropped = player.dropOneFromHand();

        assertEquals(1, dropped.getCount());
        assertSame(ItemType.DIAMOND, dropped.getType());
        assertEquals("keepsake", dropped.getCustomName());
        assertEquals("test", dropped.getMetadata().get("origin"));
        assertEquals(4, player.getInventory().getHotbar()[0].getCount());
        assertTrue(player.getInventory().getHotbar()[0].canMergeWith(dropped));
    }
}
