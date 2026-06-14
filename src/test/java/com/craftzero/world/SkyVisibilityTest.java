package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkyVisibilityTest {
    @Test
    @DisplayName("Sky visibility should require a loaded column and no opaque cover")
    void skyVisibilityUsesLoadedColumnAndOpaqueCover() {
        World world = new World(211L);
        try {
            assertFalse(world.canSeeSky(0, 100, 0));

            world.getChunkNow(0, 0);
            for (int y = 100; y < 128; y++) {
                world.setBlock(0, y, 0, BlockType.AIR);
            }
            assertTrue(world.canSeeSky(0, 100, 0));

            world.setBlock(0, 110, 0, BlockType.GLASS);
            assertTrue(world.canSeeSky(0, 100, 0));

            world.setBlock(0, 111, 0, BlockType.STONE);
            assertFalse(world.canSeeSky(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }
}
