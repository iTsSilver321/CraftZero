package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OverworldGenerationSprintTest {

    @Test
    @DisplayName("Tree feature rooted on a chunk edge should place the intersecting crown in the neighbor chunk")
    void treeFeaturePlacesAcrossChunkBoundary() {
        TreeFeature.Candidate tree = new TreeFeature.Candidate(15, 65, 8, 5, 100);
        TreeFeature.BlockQuery flatGround = OverworldGenerationSprintTest::flatGroundBlock;
        assertTrue(tree.canPlace(flatGround));

        Chunk west = flatChunk(0, 0);
        Chunk east = flatChunk(1, 0);
        tree.placeInto(west, 0, 0);
        tree.placeInto(east, 1, 0);

        assertSame(BlockType.OAK_LOG, west.getBlock(15, 65, 8));
        assertSame(BlockType.LEAVES, east.getBlock(0, 68, 8));
        assertSame(BlockType.LEAVES, east.getBlock(1, 68, 8));
    }

    @Test
    @DisplayName("Feature planner should be deterministic and keep accepted trees spaced apart")
    void featurePlannerIsDeterministicAndRejectsStackedTrees() {
        ReleaseOneWorldGenerator firstGenerator = new ReleaseOneWorldGenerator(987654321L, Dimension.OVERWORLD);
        ReleaseOneWorldGenerator secondGenerator = new ReleaseOneWorldGenerator(987654321L, Dimension.OVERWORLD);
        FeaturePlanner first = new FeaturePlanner(987654321L, firstGenerator);
        FeaturePlanner second = new FeaturePlanner(987654321L, secondGenerator);

        List<TreeFeature.Candidate> firstList = first.acceptedTreesIntersectingChunk(0, 0);
        List<TreeFeature.Candidate> secondList = second.acceptedTreesIntersectingChunk(0, 0);
        assertEquals(firstList, secondList);

        Set<TreeFeature.Candidate> candidates = new HashSet<>();
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                candidates.addAll(first.acceptedTreesIntersectingChunk(cx, cz));
            }
        }
        TreeFeature.Candidate[] array = candidates.toArray(TreeFeature.Candidate[]::new);
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                assertFalse(array[i].conflictsWith(array[j]),
                        "Accepted tree candidates should not overlap or stack");
            }
        }
    }

    @Test
    @DisplayName("Density terrain should produce bedrock, surface layers, and sea water")
    void densityTerrainHasReleaseOneInvariants() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);

        assertSame(BlockType.BEDROCK, generator.baseBlockAt(0, 0, 0));
        int top = generator.terrainTopY(0, 0);
        assertTrue(top > 4 && top < Chunk.HEIGHT - 2);
        assertNotSame(BlockType.STONE, generator.baseBlockAt(0, top, 0));

        int[] ocean = findOceanWaterColumn(generator);
        assertNotNull(ocean, "Expected to find an ocean/river column near spawn for terrain invariant test");
        assertTrue(generator.baseBlockAt(ocean[0], SEA(), ocean[1]).isWater()
                || generator.baseBlockAt(ocean[0], SEA(), ocean[1]) == BlockType.ICE);
    }

    @Test
    @DisplayName("Ice should generate only on frozen biome sea-level water")
    void iceOnlyGeneratesInFrozenWaterBiomes() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        int checkedNonFrozenWater = 0;
        int checkedFrozenWater = 0;

        for (int x = -768; x <= 768; x += 8) {
            for (int z = -768; z <= 768; z += 8) {
                BiomeType biome = generator.getBiome(x, z);
                if (generator.terrainTopY(x, z) >= ReleaseOneWorldGenerator.SEA_LEVEL - 2) {
                    continue;
                }
                BlockType seaBlock = generator.baseBlockAt(x, ReleaseOneWorldGenerator.SEA_LEVEL, z);
                if (biome.canFreezeWater()) {
                    checkedFrozenWater++;
                    assertSame(BlockType.ICE, seaBlock);
                } else {
                    checkedNonFrozenWater++;
                    assertSame(BlockType.WATER, seaBlock,
                            "Non-frozen biome " + biome + " should not create sea-level ice");
                }
            }
        }

        assertTrue(checkedNonFrozenWater > 0, "Expected at least one ordinary water column");
        assertTrue(checkedFrozenWater > 0, "Expected at least one frozen water column");
        assertFalse(BiomeType.OCEAN.canFreezeWater());
        assertFalse(BiomeType.RIVER.canFreezeWater());
        assertFalse(BiomeType.TAIGA.canFreezeWater());
        assertFalse(BiomeType.PLAINS.canFreezeWater());
    }

    @Test
    @DisplayName("Generated ocean columns should keep solid seafloors below water")
    void cavesDoNotPunctureOceanSeafloor() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(13579L, Dimension.OVERWORLD);
        int[] ocean = findOceanWaterColumn(generator);
        assertNotNull(ocean);
        int chunkX = Math.floorDiv(ocean[0], Chunk.WIDTH);
        int chunkZ = Math.floorDiv(ocean[1], Chunk.DEPTH);
        Chunk chunk = new Chunk(chunkX, chunkZ);

        generator.generateChunk(null, chunk, chunkX, chunkZ);

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 1; y <= ReleaseOneWorldGenerator.SEA_LEVEL; y++) {
                    BlockType block = chunk.getBlock(x, y, z);
                    BlockType below = chunk.getBlock(x, y - 1, z);
                    if ((block.isWater() || block == BlockType.ICE) && !below.isWater() && below != BlockType.ICE) {
                        assertTrue(below.isSolid(), "Water column should rest on solid seafloor, not a cave hole");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Cave generation should create deep lava when carving below lava level")
    void cavesCreateDeepLava() {
        boolean foundLava = false;
        for (long seed = 1; seed < 50 && !foundLava; seed++) {
            Chunk chunk = stoneChunk(0, 0);
            new CaveGenerator().generate(chunk, seed);
            for (int x = 0; x < Chunk.WIDTH && !foundLava; x++) {
                for (int z = 0; z < Chunk.DEPTH && !foundLava; z++) {
                    for (int y = 1; y < ReleaseOneWorldGenerator.LAVA_LEVEL; y++) {
                        if (chunk.getBlock(x, y, z) == BlockType.LAVA) {
                            foundLava = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(foundLava, "Expected at least one deterministic cave origin to carve lava below lava level");
    }

    private static int SEA() {
        return ReleaseOneWorldGenerator.SEA_LEVEL;
    }

    private static int[] findOceanWaterColumn(ReleaseOneWorldGenerator generator) {
        for (int x = -512; x <= 512; x += 8) {
            for (int z = -512; z <= 512; z += 8) {
                BiomeType biome = generator.getBiome(x, z);
                int top = generator.terrainTopY(x, z);
                if ((biome.isOceanic() || biome == BiomeType.RIVER || biome == BiomeType.FROZEN_RIVER)
                        && top < ReleaseOneWorldGenerator.SEA_LEVEL - 2) {
                    return new int[] { x, z };
                }
            }
        }
        return null;
    }

    private static Chunk flatChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y <= 63; y++) {
                    chunk.setBlock(x, y, z, y == 63 ? BlockType.GRASS : BlockType.DIRT);
                }
            }
        }
        return chunk;
    }

    private static Chunk stoneChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, y == 0 ? BlockType.BEDROCK : BlockType.STONE);
                }
            }
        }
        return chunk;
    }

    private static BlockType flatGroundBlock(int x, int y, int z) {
        if (y == 64) {
            return BlockType.GRASS;
        }
        if (y < 64) {
            return BlockType.DIRT;
        }
        return BlockType.AIR;
    }
}
