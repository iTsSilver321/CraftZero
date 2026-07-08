package com.craftzero.main;

import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Pig;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAnimalBreedingInteractionTest {
    @Test
    @DisplayName("Player wheat use should put eligible animals into Release 1.0 love mode")
    void playerFeedsWheatForBreeding() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        ItemStack wheat = new ItemStack(ItemType.WHEAT, 2);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = wheat;

        assertTrue(player.feedBreedingAnimal(pig, wheat));

        assertTrue(pig.isInLove());
        assertEquals(1, wheat.getCount());
    }

    @Test
    @DisplayName("Player wheat use should refresh Release 1.0 love mode on in-love adults")
    void playerCanRefreshLoveModeOnAdult() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        ItemStack wheat = new ItemStack(ItemType.WHEAT, 2);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = wheat;

        assertTrue(player.feedBreedingAnimal(pig, wheat));
        pig.setLoveTicks(100);
        assertTrue(player.feedBreedingAnimal(pig, wheat));

        assertEquals(Mob.LOVE_MODE_TICKS, pig.getLoveTicks());
        assertTrue(wheat.isEmpty());
        assertNull(player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()]);
    }

    @Test
    @DisplayName("Player wheat use should reject baby animals without consuming wheat")
    void playerCannotBreedBabyAnimal() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        pig.setGrowingAge(Pig.BABY_GROWING_AGE);
        ItemStack wheat = new ItemStack(ItemType.WHEAT, 1);

        assertFalse(player.feedBreedingAnimal(pig, wheat));

        assertFalse(pig.isInLove());
        assertEquals(1, wheat.getCount());
    }
}
