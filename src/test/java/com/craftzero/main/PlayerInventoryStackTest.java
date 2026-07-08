package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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
        assertEquals(1, player.getStats().getStatistics().getItemsDropped());
        assertEquals(1, player.getStats().getStatistics().getItemsDropped(ItemType.DIAMOND));
    }

    @Test
    @DisplayName("Dropping from an empty hand should not count dropped items")
    void emptyHandDropDoesNotCountStatistic() {
        Player player = new Player(0.0f, 64.0f, 0.0f);

        ItemStack dropped = player.dropOneFromHand();

        assertNull(dropped);
        assertEquals(0, player.getStats().getStatistics().getItemsDropped());
        assertEquals(0, player.getStats().getStatistics().getItemsDropped(ItemType.DIAMOND));
    }

    @Test
    @DisplayName("Depleted durable items should emit item crack particles")
    void depletedDurableItemsEmitItemCrackParticles() throws Exception {
        World world = new World(7101L);
        try {
            Player player = new Player(2.0f, 64.0f, 3.0f);
            player.setWorld(world);
            ItemStack shovel = new ItemStack(ItemType.WOODEN_SHOVEL, 1, 1);
            Method useDurability = Player.class.getDeclaredMethod("useDurabilityWithEnchantments", ItemStack.class);
            useDurability.setAccessible(true);

            assertEquals(Boolean.TRUE, useDurability.invoke(player, shovel));

            assertEquals(8, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_CRACK)
                    .filter(particle -> particle.getItemParticleType() == ItemType.WOODEN_SHOVEL)
                    .count());
        } finally {
            world.cleanup();
        }
    }
}
