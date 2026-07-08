package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerAnimationTest {
    @Test
    @DisplayName("Player arm swing should advance on the old six-tick cadence")
    void playerArmSwingUsesSixTickCadence() {
        assertEquals(1.0f / 6.0f, swingProgressAfterTicks(false, 1), 0.0001f);
        assertEquals(5.0f / 6.0f, swingProgressAfterTicks(false, 5), 0.0001f);
        assertEquals(0.0f, swingProgressAfterTicks(false, 6), 0.0001f);
    }

    @Test
    @DisplayName("Held items should not use a faster arm swing cadence")
    void heldItemsUseSameSwingCadenceAsEmptyHand() {
        assertEquals(swingProgressAfterTicks(false, 1), swingProgressAfterTicks(true, 1), 0.0001f);
        assertEquals(swingProgressAfterTicks(false, 5), swingProgressAfterTicks(true, 5), 0.0001f);
    }

    @Test
    @DisplayName("Player arm swing can restart after the old half-swing gate")
    void playerArmSwingCanRestartAfterHalfSwingGate() {
        World world = new World(507L);
        try {
            Player player = new Player(0.0f, 90.0f, 0.0f);
            player.swingArm();
            tickPlayer(player, world, 3);

            player.swingArm();

            assertEquals(0.0f, player.getSwingProgress(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player arm swing should not restart before the old half-swing gate")
    void playerArmSwingDoesNotRestartBeforeHalfSwingGate() {
        World world = new World(509L);
        try {
            Player player = new Player(0.0f, 90.0f, 0.0f);
            player.swingArm();
            tickPlayer(player, world, 2);

            player.swingArm();

            assertEquals(2.0f / 6.0f, player.getSwingProgress(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    private static float swingProgressAfterTicks(boolean holdingItem, int ticks) {
        World world = new World(508L + ticks + (holdingItem ? 100L : 0L));
        try {
            Player player = new Player(0.0f, 90.0f, 0.0f);
            if (holdingItem) {
                player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                        new ItemStack(ItemType.STONE_PICKAXE, 1);
            }
            player.swingArm();
            tickPlayer(player, world, ticks);
            return player.getSwingProgress(1.0f);
        } finally {
            world.cleanup();
        }
    }

    private static void tickPlayer(Player player, World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.update(1.0f / 20.0f, world);
        }
    }
}
