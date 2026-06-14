package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DungeonFeatureTest {
    @Test
    @DisplayName("Dungeon generation should place a mossy room shell with a mob spawner when the envelope is valid")
    void dungeonPlacesRoomShellAndSpawner() {
        DungeonGenerator generator = new DungeonGenerator();
        World world = new World(701L);
        try {
            Chunk generated = null;
            for (long seed = 0; seed < 500 && generated == null; seed++) {
                Chunk chunk = solidStoneChunk();
                generator.generate(world, chunk, seed, 0, 0);
                if (contains(chunk, BlockType.MOB_SPAWNER)) {
                    generated = chunk;
                }
            }

            assertNotNull(generated, "Expected at least one deterministic dungeon candidate in the search window");
            assertTrue(contains(generated, BlockType.MOB_SPAWNER));
            assertTrue(contains(generated, BlockType.MOSSY_COBBLESTONE));
            assertTrue(contains(generated, BlockType.COBBLESTONE));
            assertTrue(contains(generated, BlockType.AIR));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dungeon generation should reject invalid open envelopes")
    void dungeonRejectsInvalidEnvelope() {
        DungeonGenerator generator = new DungeonGenerator();
        World world = new World(702L);
        try {
            for (long seed = 0; seed < 100; seed++) {
                Chunk chunk = new Chunk(0, 0);
                generator.generate(world, chunk, seed, 0, 0);
                assertFalse(contains(chunk, BlockType.MOB_SPAWNER));
            }
        } finally {
            world.cleanup();
        }
    }

    private static Chunk solidStoneChunk() {
        Chunk chunk = new Chunk(0, 0);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }
        return chunk;
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
