package com.craftzero.world;

import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.mob.EnderDragon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndProgressionTest {
    @Test
    @DisplayName("End portal frames should activate a 3x3 End portal when the last eye is inserted")
    void completeEndPortalActivates() {
        World world = new World(8001L);
        try {
            int cx = 0;
            int y = 40;
            int cz = 0;
            for (int x = cx - 1; x <= cx + 1; x++) {
                world.setBlock(x, y, cz - 2, BlockType.END_PORTAL_FRAME, 4);
                world.setBlock(x, y, cz + 2, BlockType.END_PORTAL_FRAME, 4);
            }
            for (int z = cz - 1; z <= cz + 1; z++) {
                world.setBlock(cx - 2, y, z, BlockType.END_PORTAL_FRAME, 4);
                world.setBlock(cx + 2, y, z, BlockType.END_PORTAL_FRAME, 4);
            }
            world.setBlock(cx + 2, y, cz, BlockType.END_PORTAL_FRAME, 0);

            assertTrue(world.addEyeToEndPortalFrame(cx + 2, y, cz));
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    assertSame(BlockType.END_PORTAL, world.getBlock(x, y, z));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("The End generator should create island, pillars, crystals, and one dragon")
    void endGeneratorCreatesProgressionEntities() {
        World world = new World(8002L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            Chunk origin = world.getChunkNow(0, 0);
            assertTrue(contains(origin, BlockType.END_STONE));
            world.getChunkNow(2, 0);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream().anyMatch(EnderDragon.class::isInstance));
            assertTrue(world.getEntities().stream().anyMatch(EndCrystalEntity.class::isInstance));
            assertSame(BlockType.OBSIDIAN, world.getBlock(42, 74, 0));
        } finally {
            world.cleanup();
        }
    }

    private static boolean contains(Chunk chunk, BlockType type) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
