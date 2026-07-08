package com.craftzero.main;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCakeInteractionTest {
    @Test
    @DisplayName("Using cake should eat a slice when the player can eat")
    void useCakeEatsSlice() throws Exception {
        World world = new World(6284L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.CAKE, 0);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            player.getStats().restore(20.0f, 16.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

            assertTrue(eatCakeAt(player, world, new Vector3i(0, 100, 0)));

            assertSame(BlockType.CAKE, world.getBlock(0, 100, 0));
            assertEquals(1, world.getCakeBites(0, 100, 0));
            assertEquals(18.0f, player.getStats().getHunger(), 0.0001f);
            assertEquals(0.4f, player.getStats().getSaturation(), 0.0001f);
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(2, sounds.size());
            assertEquals(WorldSoundEvent.EAT, sounds.get(0).soundId());
            assertEquals(WorldSoundEvent.BURP, sounds.get(1).soundId());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Using cake should not eat when hunger is full")
    void useCakeRequiresHungerRoom() throws Exception {
        World world = new World(6285L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.CAKE, 0);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            player.getStats().restore(20.0f, PlayerStats.MAX_HUNGER, 0.0f, PlayerStats.MAX_AIR_SECONDS);

            assertFalse(eatCakeAt(player, world, new Vector3i(0, 100, 0)));

            assertSame(BlockType.CAKE, world.getBlock(0, 100, 0));
            assertEquals(0, world.getCakeBites(0, 100, 0));
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    private static boolean eatCakeAt(Player player, World world, Vector3i pos) throws Exception {
        Method method = Player.class.getDeclaredMethod("eatCakeAt", World.class, Vector3i.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, pos);
    }
}
