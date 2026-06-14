package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructureGeneratorTest {
    @Test
    @DisplayName("First-pass stronghold generation should be deterministic and dimension-gated")
    void strongholdGenerationIsDeterministicAndDimensionGated() {
        long seed = 8128L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator overworldGenerator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        World world = new World(seed);
        try {
            Chunk first = null;
            Chunk second = null;
            int foundX = 0;
            int foundZ = 0;
            for (int cx = -80; cx <= 80 && first == null; cx++) {
                for (int cz = -80; cz <= 80; cz++) {
                    Chunk candidate = new Chunk(cx, cz);
                    structures.generate(world, candidate, seed, cx, cz, Dimension.OVERWORLD, overworldGenerator);
                    if (contains(candidate, BlockType.END_PORTAL_FRAME)) {
                        first = candidate;
                        foundX = cx;
                        foundZ = cz;
                        break;
                    }
                }
            }
            assertNotNull(first, "Expected one first-pass stronghold candidate near spawn");

            second = new Chunk(foundX, foundZ);
            structures.generate(world, second, seed, foundX, foundZ, Dimension.OVERWORLD, overworldGenerator);
            assertEquals(count(first, BlockType.END_PORTAL_FRAME), count(second, BlockType.END_PORTAL_FRAME));
            assertEquals(count(first, BlockType.STONE_BRICK), count(second, BlockType.STONE_BRICK));
            assertTrue(contains(first, BlockType.MOB_SPAWNER));

            Chunk netherChunk = new Chunk(foundX, foundZ);
            structures.generate(world, netherChunk, seed, foundX, foundZ, Dimension.NETHER,
                    new ReleaseOneWorldGenerator(seed, Dimension.NETHER));
            assertFalse(contains(netherChunk, BlockType.END_PORTAL_FRAME));
        } finally {
            world.cleanup();
        }
    }

    private static boolean contains(Chunk chunk, BlockType type) {
        return count(chunk, type) > 0;
    }

    private static int count(Chunk chunk, BlockType type) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
