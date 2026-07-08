package com.craftzero.main;

import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerFishingRodInteractionTest {
    @Test
    @DisplayName("Fishing rods cast and reel one active Release 1.0 bobber")
    void fishingRodCastsAndReelsBobber() {
        World world = new World(6236L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ItemStack rod = new ItemStack(ItemType.FISHING_ROD, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = rod;

            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));
            FishingHookEntity hook = player.getFishingHook();
            assertNotNull(hook);
            assertEquals(1, player.getStats().getStatistics().getItemsUsed());
            assertEquals(1, player.getStats().getStatistics().getItemsUsed(ItemType.FISHING_ROD));
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream().anyMatch(FishingHookEntity.class::isInstance));
            assertEquals(ItemType.FISHING_ROD.getMaxDurability(), rod.getDurability());

            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));

            assertTrue(hook.isRemoved());
            assertNull(player.getFishingHook());
            assertEquals(ItemType.FISHING_ROD.getMaxDurability(), rod.getDurability());
            assertEquals(2, player.getStats().getStatistics().getItemsUsed());
            assertEquals(2, player.getStats().getStatistics().getItemsUsed(ItemType.FISHING_ROD));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Reeling a catchable bobber drops raw fish and damages the rod once")
    void fishingRodReelsCatchableBobber() {
        World world = new World(6237L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ItemStack rod = new ItemStack(ItemType.FISHING_ROD, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = rod;
            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));
            FishingHookEntity hook = player.getFishingHook();
            assertNotNull(hook);
            hook.setCatchableTicks(20);

            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));

            assertTrue(hook.isRemoved());
            assertNull(player.getFishingHook());
            assertEquals(ItemType.FISHING_ROD.getMaxDurability() - 1, rod.getDurability());
            assertEquals(1, player.getStats().getStatistics().getFishCaught());
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.RAW_FISH && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Reeling a hooked entity pulls it and damages the rod by the Release-era amount")
    void fishingRodReelsHookedEntity() {
        World world = new World(6240L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ItemStack rod = new ItemStack(ItemType.FISHING_ROD, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = rod;
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.0f);
            FishingHookEntity hook = new FishingHookEntity(3.0f, 100.8f, 0.0f,
                    0.0f, 0.0f, 0.0f, player);
            player.attachFishingHook(hook);
            world.spawnEntity(zombie);
            world.spawnEntity(hook);
            world.updateEntities(1.0f / 20.0f);

            assertSame(zombie, hook.getHookedEntity());
            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));

            assertTrue(hook.isRemoved());
            assertNull(player.getFishingHook());
            assertEquals(ItemType.FISHING_ROD.getMaxDurability() - 3, rod.getDurability());
            assertTrue(zombie.getMotionX() < 0.0f);
            assertTrue(zombie.getMotionY() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fishing rods count depleted item statistics when reeling breaks the rod")
    void fishingRodBreaksIntoDepletedStatistic() {
        World world = new World(6241L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ItemStack rod = new ItemStack(ItemType.FISHING_ROD, 1, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = rod;

            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));
            FishingHookEntity hook = player.getFishingHook();
            assertNotNull(hook);
            hook.setCatchableTicks(20);

            assertTrue(player.useFishingRod(world, rod, new Vector3f(1.0f, 0.0f, 0.0f)));

            assertTrue(hook.isRemoved());
            assertNull(player.getFishingHook());
            assertNull(player.getInventory().getItemInHand());
            assertEquals(2, player.getStats().getStatistics().getItemsUsed(ItemType.FISHING_ROD));
            assertEquals(1, player.getStats().getStatistics().getItemsDepleted());
            assertEquals(1, player.getStats().getStatistics().getItemsDepleted(ItemType.FISHING_ROD));
        } finally {
            world.cleanup();
        }
    }
}
