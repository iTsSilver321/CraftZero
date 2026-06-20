package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructureGeneratorTest {
    @Test
    @DisplayName("Stronghold generation should be deterministic and dimension-gated")
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

    @Test
    @DisplayName("Structure locator should point at a generated stronghold chunk")
    void locateStrongholdMatchesGeneratedPortalRoom() {
        long seed = 24681357L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.OVERWORLD,
                StructureType.STRONGHOLD, 0, 0, generator);
        assertNotNull(location);

        World world = new World(seed);
        try {
            boolean found = false;
            for (int cx = location.chunkX() - 5; cx <= location.chunkX() + 5 && !found; cx++) {
                for (int cz = location.chunkZ() - 5; cz <= location.chunkZ() + 5; cz++) {
                    Chunk chunk = new Chunk(cx, cz);
                    structures.generate(world, chunk, seed, cx, cz, Dimension.OVERWORLD, generator);
                    if (contains(chunk, BlockType.END_PORTAL_FRAME)) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found, "Locator should resolve to a stronghold with a portal room nearby");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether fortress locator should generate blaze spawner and wart pieces")
    void locateNetherFortressMatchesGeneratedPieces() {
        long seed = 97531L;
        StructureGenerator structures = new StructureGenerator();
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.NETHER);
        StructureGenerator.StructureLocation location = structures.locate(seed, Dimension.NETHER,
                StructureType.NETHER_FORTRESS, 0, 0, generator);
        assertNotNull(location);

        World world = new World(seed, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            boolean hasSpawner = false;
            boolean hasWart = false;
            for (int cx = location.chunkX() - 4; cx <= location.chunkX() + 4; cx++) {
                for (int cz = location.chunkZ() - 4; cz <= location.chunkZ() + 4; cz++) {
                    Chunk chunk = new Chunk(cx, cz);
                    structures.generate(world, chunk, seed, cx, cz, Dimension.NETHER, generator);
                    hasSpawner |= contains(chunk, BlockType.MOB_SPAWNER);
                    hasWart |= contains(chunk, BlockType.NETHER_WART);
                }
            }
            assertTrue(hasSpawner);
            assertTrue(hasWart);
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
