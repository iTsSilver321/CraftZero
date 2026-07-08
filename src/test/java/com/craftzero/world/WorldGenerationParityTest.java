package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorldGenerationParityTest {
    @Test
    @DisplayName("New worlds should default to the Release 1.0 overworld generator")
    void newWorldsUseReleaseOneGeneratorByDefault() {
        World world = new World(1234L);
        try {
            assertEquals(WorldGenerator.RELEASE_ONE, world.getGeneratorId());
            assertSame(Dimension.OVERWORLD, world.getDimension());
            assertNotSame(BiomeType.HELL, world.getReleaseBiome(0, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Saved dimensions should select matching Release 1.0 dimension generators")
    void explicitDimensionsSelectMatchingGenerators() {
        World nether = new World(1234L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        World end = new World(1234L, WorldGenerators.generatorIdFor(Dimension.THE_END), null);
        try {
            assertEquals("minecraft_java_1_0_nether", nether.getGeneratorId());
            assertSame(Dimension.NETHER, nether.getDimension());
            assertSame(BiomeType.HELL, nether.getReleaseBiome(12, -4));

            assertEquals("minecraft_java_1_0_end", end.getGeneratorId());
            assertSame(Dimension.THE_END, end.getDimension());
            assertSame(BiomeType.SKY, end.getReleaseBiome(12, -4));
        } finally {
            nether.cleanup();
            end.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 spawn search should pick deterministic grass terrain")
    void releaseOneSpawnSearchUsesSourceBiomeSearchAndGrassGate() {
        ReleaseOneWorldGenerator first = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        ReleaseOneWorldGenerator second = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        ReleaseOneWorldGenerator.SpawnPoint spawn = first.findSafeSpawn();

        assertEquals(spawn, second.findSafeSpawn());
        assertNotEquals(new ReleaseOneWorldGenerator.SpawnPoint(0, 80, 0), spawn);
        assertGrassSpawn(first, spawn);
    }

    @Test
    @DisplayName("Non-Overworld Release generators should keep the legacy spawn fallback")
    void nonOverworldSpawnSearchKeepsFallback() {
        ReleaseOneWorldGenerator nether = new ReleaseOneWorldGenerator(424242L, Dimension.NETHER);
        ReleaseOneWorldGenerator end = new ReleaseOneWorldGenerator(424242L, Dimension.THE_END);

        assertEquals(new ReleaseOneWorldGenerator.SpawnPoint(0, 80, 0), nether.findSafeSpawn());
        assertEquals(new ReleaseOneWorldGenerator.SpawnPoint(0, 80, 0), end.findSafeSpawn());
    }

    @Test
    @DisplayName("Dimension-specific Release generator ids should override stale saved dimensions")
    void dimensionSpecificGeneratorIdsOverrideStaleDimensions() {
        World nether = new World(1234L, WorldGenerators.generatorIdFor(Dimension.NETHER), Dimension.OVERWORLD);
        World end = new World(1234L, WorldGenerators.generatorIdFor(Dimension.THE_END), Dimension.OVERWORLD);
        try {
            assertEquals("minecraft_java_1_0_nether", nether.getGeneratorId());
            assertSame(Dimension.NETHER, nether.getDimension());
            assertSame(BiomeType.HELL, nether.getReleaseBiome(0, 0));

            assertEquals("minecraft_java_1_0_end", end.getGeneratorId());
            assertSame(Dimension.THE_END, end.getDimension());
            assertSame(BiomeType.SKY, end.getReleaseBiome(0, 0));
        } finally {
            nether.cleanup();
            end.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 biome source should be layered, deterministic, and varied")
    void releaseOneBiomeSourceIsDeterministicAndVaried() {
        ReleaseOneWorldGenerator first = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        ReleaseOneWorldGenerator second = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Set<BiomeType> seen = EnumSet.noneOf(BiomeType.class);

        for (int x = -4096; x <= 4096; x += 64) {
            for (int z = -4096; z <= 4096; z += 64) {
                BiomeType biome = first.getBiome(x, z);
                assertSame(biome, second.getBiome(x, z));
                seen.add(biome);
            }
        }
        Set<BiomeType> mushroomSeen = sampleBiomes(new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD));
        Set<BiomeType> combined = EnumSet.copyOf(seen);
        combined.addAll(mushroomSeen);

        assertTrue(seen.contains(BiomeType.OCEAN), "Layered source should retain large oceans");
        assertTrue(seen.contains(BiomeType.RIVER) || seen.contains(BiomeType.FROZEN_RIVER),
                "Layered source should carve river bands");
        assertTrue(mushroomSeen.contains(BiomeType.MUSHROOM_ISLAND_SHORE),
                "Release 1.0 should add mushroom shore biomes at mushroom island/ocean edges");
        assertTrue(seen.contains(BiomeType.DESERT) || seen.contains(BiomeType.PLAINS));
        assertTrue(seen.contains(BiomeType.FOREST) || seen.contains(BiomeType.TAIGA));
        assertTrue(seen.stream().anyMatch(BiomeType::isFrozen));
        assertTrue(combined.stream().noneMatch(biome -> biome.getId() >= BiomeType.BEACH.getId()),
                "Release 1.0 mc100 biome generation should not emit later beach/hills/edge biomes");
    }

    @Test
    @DisplayName("Release 1.0 biome source should match source-derived sample vectors")
    void releaseOneBiomeSourceMatchesSourceVectors() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        assertFinalBiome(generator, 0, 0, BiomeType.FOREST);
        assertFinalBiome(generator, 64, 64, BiomeType.PLAINS);
        assertFinalBiome(generator, 1024, -512, BiomeType.OCEAN);
        assertFinalBiome(generator, -2048, -1024, BiomeType.ICE_PLAINS);
        assertFinalBiome(generator, 333, -777, BiomeType.EXTREME_HILLS);
        assertFinalBiome(generator, -8192, -2032, BiomeType.FROZEN_RIVER);
        assertFinalBiome(generator, -7328, -2896, BiomeType.DESERT);
        assertFinalBiome(generator, -7392, -2576, BiomeType.FOREST);
        assertFinalBiome(generator, -6496, -1216, BiomeType.TAIGA);
        assertFinalBiome(generator, -6864, 640, BiomeType.EXTREME_HILLS);
        assertFinalBiome(generator, -9656, 3664, BiomeType.MUSHROOM_ISLAND_SHORE);
        assertFinalBiome(generator, -9656, 3666, BiomeType.OCEAN);

        assertGenerationBiome(generator, 0, 0, BiomeType.FOREST);
        assertGenerationBiome(generator, 16, 16, BiomeType.PLAINS);
        assertGenerationBiome(generator, 256, -128, BiomeType.OCEAN);
        assertGenerationBiome(generator, -512, -256, BiomeType.ICE_PLAINS);
        assertGenerationBiome(generator, 83, -194, BiomeType.EXTREME_HILLS);
        assertGenerationBiome(generator, -2048, -508, BiomeType.FROZEN_RIVER);
        assertGenerationBiome(generator, -1832, -724, BiomeType.DESERT);
        assertGenerationBiome(generator, -1848, -644, BiomeType.FOREST);
        assertGenerationBiome(generator, -1624, -304, BiomeType.TAIGA);
        assertGenerationBiome(generator, -1716, 160, BiomeType.EXTREME_HILLS);

        ReleaseOneWorldGenerator mushroomGenerator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        assertFinalBiome(mushroomGenerator, -19240, 16356, BiomeType.MUSHROOM_ISLAND_SHORE);
        assertFinalBiome(mushroomGenerator, -19280, 16352, BiomeType.MUSHROOM_ISLAND_SHORE);
    }

    @Test
    @DisplayName("Release 1.0 biome terrain constants should match source height ranges")
    void releaseOneBiomeTerrainConstantsMatchSource() {
        assertBiomeHeight(BiomeType.OCEAN, -1.0F, 0.4F);
        assertBiomeHeight(BiomeType.PLAINS, 0.1F, 0.3F);
        assertBiomeHeight(BiomeType.DESERT, 0.1F, 0.2F);
        assertBiomeHeight(BiomeType.EXTREME_HILLS, 0.2F, 1.8F);
        assertBiomeHeight(BiomeType.FOREST, 0.1F, 0.3F);
        assertBiomeHeight(BiomeType.TAIGA, 0.1F, 0.4F);
        assertBiomeHeight(BiomeType.SWAMPLAND, -0.2F, 0.1F);
        assertBiomeHeight(BiomeType.RIVER, -0.5F, 0.0F);
        assertBiomeHeight(BiomeType.FROZEN_OCEAN, -1.0F, 0.5F);
        assertBiomeHeight(BiomeType.FROZEN_RIVER, -0.5F, 0.0F);
        assertBiomeHeight(BiomeType.ICE_PLAINS, 0.1F, 0.3F);
        assertBiomeHeight(BiomeType.ICE_MOUNTAINS, 0.2F, 1.8F);
        assertBiomeHeight(BiomeType.MUSHROOM_ISLAND, 0.2F, 1.0F);
        assertBiomeHeight(BiomeType.MUSHROOM_ISLAND_SHORE, -1.0F, 0.1F);
    }

    @Test
    @DisplayName("Release 1.0 Overworld terrain should match source-shaped height vectors")
    void releaseOneOverworldTerrainMatchesHeightVectors() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        assertTerrainVector(generator, 0, 0, BiomeType.FOREST, 65, BlockType.GRASS, BlockType.DIRT);
        assertTerrainVector(generator, 1024, -512, BiomeType.OCEAN, 51, BlockType.DIRT, BlockType.WATER);
        assertTerrainVector(generator, -2048, -1024, BiomeType.ICE_PLAINS, 70, BlockType.GRASS, BlockType.STONE);
        assertTerrainVector(generator, 333, -777, BiomeType.EXTREME_HILLS, 109, BlockType.GRASS, BlockType.STONE);

        ReleaseOneWorldGenerator mushroomGenerator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        assertTerrainVector(mushroomGenerator, -19240, 16356, BiomeType.MUSHROOM_ISLAND_SHORE, 47,
                BlockType.DIRT, BlockType.WATER);
        assertTerrainVector(mushroomGenerator, -19280, 16352, BiomeType.MUSHROOM_ISLAND_SHORE, 46,
                BlockType.DIRT, BlockType.WATER);
    }

    @Test
    @DisplayName("Release 1.0 Overworld density should match source-derived raw grid vectors")
    void releaseOneOverworldDensityMatchesRawGridVectors() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        OverworldDensityField density = new OverworldDensityField(424242L, generator::getBiomeForGenerationLayer);

        assertDensityGridVector(density, 0, 64, 0, 7.259413761406352);
        assertDensityGridVector(density, 0, 72, 0, -29.961264378725403);
        assertDensityGridVector(density, 1024, 48, -512, 12.757951894422597);
        assertDensityGridVector(density, -2048, 72, -1024, -9.666266273368530);
        assertDensityGridVector(density, 332, 104, -780, 7.979133083206165);
        assertDensityGridVector(density, 336, 112, -780, -2.731431994601640);
        assertDensityAt(density, 2, 66, 2, 1.070256843255275);
        assertEquals(1041241046, overworldDensityHash(density, 0, 0));
        assertEquals(-1649786647, overworldDensityHash(density, 64, -32));
        assertEquals(-995545184, overworldDensityHash(density, 20, -49));
        assertEquals(1465012525, overworldDensityHash(density, -128, -64));

        ReleaseOneWorldGenerator mushroomGenerator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        OverworldDensityField mushroomDensity = new OverworldDensityField(24680L,
                mushroomGenerator::getBiomeForGenerationLayer);
        assertDensityGridVector(mushroomDensity, -19240, 64, 16356, -139.096529950929780);
        assertDensityGridVector(mushroomDensity, -19240, 72, 16356, -205.807503465733700);
        assertDensityGridVector(mushroomDensity, -19280, 48, 16352, -30.654338444224450);
        assertEquals(-1952953807, overworldDensityHash(mushroomDensity, -1203, 1022));
        assertEquals(-1321110432, overworldDensityHash(mushroomDensity, -1205, 1022));
        assertEquals(1199473894, overworldDensityHash(mushroomDensity, 0, 0));
    }

    @Test
    @DisplayName("Overworld density cache should not alias far source grid coordinates")
    void overworldDensityCacheKeepsFullGridCoordinates() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        OverworldDensityField cached = new OverworldDensityField(424242L, generator::getBiomeForGenerationLayer);
        OverworldDensityField fresh = new OverworldDensityField(424242L, generator::getBiomeForGenerationLayer);
        int farX = 8_388_608;

        double origin = cached.density(0, 64, 0, BiomeType.PLAINS);
        double farExpected = fresh.density(farX, 64, 0, BiomeType.PLAINS);
        double farAfterOrigin = cached.density(farX, 64, 0, BiomeType.PLAINS);

        assertNotEquals(origin, farExpected, 1.0E-9,
                "Fixture should exercise two distinct Release 1.0 density grid samples");
        assertEquals(farExpected, farAfterOrigin, 1.0E-9,
                "Density cache keys must not wrap full source grid coordinates");
    }

    @Test
    @DisplayName("Release 1.0 Nether density should match source-derived raw grid vectors")
    void releaseOneNetherDensityMatchesRawGridVectors() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);

        double[] origin = invokeNetherDensities(generator, 0, 0);
        assertNetherDensityVector(origin, 0, 0, 0, 647.296287669496600);
        assertNetherDensityVector(origin, 0, 8, 0, 29.459455519070627);
        assertNetherDensityVector(origin, 2, 8, 2, 31.418354769226180);
        assertNetherDensityVector(origin, 4, 8, 4, 48.634499675539650);
        assertNetherDensityVector(origin, 3, 4, 1, 18.253878814266862);
        assertNetherDensityVector(origin, 4, 12, 0, 5.353376584976667);
        assertNetherDensityVector(origin, 1, 16, 3, -10.000000000000000);
        assertEquals(950572053, Arrays.hashCode(origin));

        double[] negative = invokeNetherDensities(generator, -16, -6);
        assertNetherDensityVector(negative, 0, 8, 0, 2.168207023258584);
        assertNetherDensityVector(negative, 2, 8, 2, -1.909496471001510);
        assertNetherDensityVector(negative, 3, 4, 1, -11.012532850939312);
        assertNetherDensityVector(negative, 4, 12, 0, -0.876414658694262);
        assertEquals(-1624015280, Arrays.hashCode(negative));

        double[] far = invokeNetherDensities(generator, 8, 8);
        assertNetherDensityVector(far, 0, 8, 0, -13.173469259125856);
        assertNetherDensityVector(far, 2, 8, 2, -18.961467423220196);
        assertNetherDensityVector(far, 4, 8, 4, -22.199773910272590);
        assertNetherDensityVector(far, 4, 12, 0, -18.697544766802974);
        assertEquals(-1104972045, Arrays.hashCode(far));

        ReleaseOneWorldGenerator alternateSeed = new ReleaseOneWorldGenerator(1234L, Dimension.NETHER);
        double[] alternateOrigin = invokeNetherDensities(alternateSeed, 0, 0);
        assertNetherDensityVector(alternateOrigin, 0, 0, 0, 644.269321200140800);
        assertNetherDensityVector(alternateOrigin, 0, 8, 0, 32.894255048406606);
        assertNetherDensityVector(alternateOrigin, 2, 8, 2, 23.502713909816745);
        assertNetherDensityVector(alternateOrigin, 4, 12, 0, 11.044139696764086);
        assertEquals(-1711021147, Arrays.hashCode(alternateOrigin));
    }

    @Test
    @DisplayName("Release 1.0 Nether chunks should include Nether decorator features")
    void releaseOneNetherDecoratesTerrain() {
        World world = new World(515151L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            Chunk origin = world.getChunkNow(0, 0);
            assertTrue(countBlocks(origin, BlockType.BEDROCK, 0, 4) > 0,
                    "Nether floor should receive randomized bedrock");
            assertTrue(countBlocks(origin, BlockType.BEDROCK, 123, 127) > 0,
                    "Nether ceiling should receive randomized bedrock");
            assertTrue(findBlockInYRange(world, 2, BlockType.LAVA, 1, 31),
                    "Nether terrain should retain lava seas below y=32");
            assertTrue(countBlocks(origin, BlockType.AIR, 33, 122) > 0,
                    "Nether density field should carve open cave volume above the lava sea");
            assertTrue(countBlocks(origin, BlockType.NETHERRACK, 1, 126) > 1024,
                    "Nether terrain should be mostly netherrack around carved spaces");

            Set<BlockType> found = findBlocks(world, 8, EnumSet.of(
                    BlockType.GLOWSTONE, BlockType.FIRE, BlockType.SOUL_SAND, BlockType.GRAVEL,
                    BlockType.BROWN_MUSHROOM, BlockType.RED_MUSHROOM));
            assertTrue(found.contains(BlockType.GLOWSTONE), "Nether caves should hang glowstone clusters");
            assertTrue(found.contains(BlockType.FIRE), "Nether floors should receive fire patches");
            assertTrue(found.contains(BlockType.SOUL_SAND), "Nether terrain should include soul sand patches");
            assertTrue(found.contains(BlockType.GRAVEL), "Nether terrain should include gravel patches");
            assertTrue(found.contains(BlockType.BROWN_MUSHROOM) || found.contains(BlockType.RED_MUSHROOM),
                    "Nether caves should occasionally place mushrooms");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Generated Nether chunks should apply Hell cave carving after surface replacement")
    void generatedNetherBaseChunkAppliesHellCavesAfterSurfaceReplacement() throws Exception {
        List<NetherBaseChunkExpectation> expectations = List.of(
                new NetherBaseChunkExpectation(515151L, 0, -4,
                        290, 48, 898, 30314, 0, 1508,
                        0, 36, 0, BlockType.SOUL_SAND, 0, 60, 9),
                new NetherBaseChunkExpectation(1234L, 7, -8,
                        352, 152, 4561, 25194, 1298, 1563,
                        0, 57, 0, BlockType.SOUL_SAND, 0, 22, 7));
        for (NetherBaseChunkExpectation expectation : expectations) {
            assertGeneratedNetherBaseChunkMatchesExpectation(expectation);
        }
    }

    @Test
    @DisplayName("Release 1.0 Nether decorators should spill from shifted neighboring origins")
    void releaseOneNetherDecoratorsSpillAcrossChunkBorders() {
        World world = new World(515151L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            Chunk spill = world.getChunkNow(-16, -6);

            assertSame(BlockType.GLOWSTONE, spill.getBlock(0, 67, 7),
                    "Glowstone from shifted population origins should spill across the local x=0 chunk edge");
            assertSame(BlockType.GLOWSTONE, spill.getBlock(0, 66, 8));
            assertSame(BlockType.GLOWSTONE, spill.getBlock(0, 65, 9));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether decorator random should resume after source surface replacement RNG")
    void netherDecoratorRandomResumesAfterSourceSurfaceReplacement() throws Exception {
        ReleaseOneWorldGenerator first = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        ReleaseOneWorldGenerator second = new ReleaseOneWorldGenerator(1234L, Dimension.NETHER);

        Random origin = invokeNetherDecoratorRandom(first, 0, 0);
        assertEquals(6, origin.nextInt(16));
        assertEquals(100, origin.nextInt(120) + 4);
        assertEquals(14, origin.nextInt(16));

        Random sameChunkDifferentSeed = invokeNetherDecoratorRandom(second, 0, 0);
        assertEquals(6, sameChunkDifferentSeed.nextInt(16),
                "ChunkProviderHell reseeds population from chunk constants, not the world seed");
        assertEquals(100, sameChunkDifferentSeed.nextInt(120) + 4);
        assertEquals(14, sameChunkDifferentSeed.nextInt(16));

        Random neighbor = invokeNetherDecoratorRandom(first, -11, -2);
        assertEquals(11, neighbor.nextInt(16));
        assertEquals(110, neighbor.nextInt(120) + 4);
        assertEquals(1, neighbor.nextInt(16));
    }

    @Test
    @DisplayName("Nether decorator random should advance through fortress structure placement")
    void netherDecoratorRandomAdvancesThroughFortressPlacement() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(1L, Dimension.NETHER);

        Random surfaceOnly = invokeNetherDecoratorRandom(generator, 7, -8);
        assertEquals(9, surfaceOnly.nextInt(16));
        assertEquals(100, surfaceOnly.nextInt(120) + 4);
        assertEquals(9, surfaceOnly.nextInt(16));

        Random afterFortress = invokeNetherPopulationRandomAfterStructures(generator, 7, -8);
        assertEquals(9, afterFortress.nextInt(16));
        assertEquals(100, afterFortress.nextInt(120) + 4);
        assertEquals(9, afterFortress.nextInt(16),
                "Source fortress spawner placement should not draw a generated delay before decorators");
    }

    @Test
    @DisplayName("Overworld population random should advance through structure placement before lakes")
    void overworldPopulationRandomAdvancesThroughStructurePlacement() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(1L, Dimension.OVERWORLD);

        Random surfaceOnly = invokePopulationRandom(generator, -80, -61);
        assertEquals(9, surfaceOnly.nextInt(16));
        assertEquals(26, surfaceOnly.nextInt(128));
        assertEquals(6, surfaceOnly.nextInt(16));

        Random afterStructures = invokeOverworldPopulationRandomAfterStructures(generator, -80, -61);
        assertEquals(14, afterStructures.nextInt(16));
        assertEquals(70, afterStructures.nextInt(128));
        assertEquals(15, afterStructures.nextInt(16),
                "ChunkProviderGenerate uses the same population RNG for structures before lake placement");
    }

    @Test
    @DisplayName("Overworld structure replay should use carved terrain for liquid aborts")
    void overworldStructureReplayUsesCarvedTerrainLiquidChecks() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(1L, Dimension.OVERWORLD);

        Random surfaceOnly = invokePopulationRandom(generator, -77, -33);
        assertEquals(0, surfaceOnly.nextInt(16));
        assertEquals(35, surfaceOnly.nextInt(128));
        assertEquals(3, surfaceOnly.nextInt(16));

        Random afterStructures = invokeOverworldPopulationRandomAfterStructures(generator, -77, -33);
        assertEquals(10, afterStructures.nextInt(16));
        assertEquals(44, afterStructures.nextInt(128));
        assertEquals(4, afterStructures.nextInt(16),
                "Mineshaft liquid-envelope aborts depend on carved terrain, not an empty scratch chunk");
    }

    @Test
    @DisplayName("Overworld structure replay should use source mineshaft-village-stronghold order")
    void overworldStructureReplayUsesSourceStructureOrder() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(1L, Dimension.OVERWORLD);

        Random surfaceOnly = invokePopulationRandom(generator, -45, 38);
        assertEquals(12, surfaceOnly.nextInt(16));
        assertEquals(96, surfaceOnly.nextInt(128));
        assertEquals(0, surfaceOnly.nextInt(16));

        Random afterStructures = invokeOverworldPopulationRandomAfterStructures(generator, -45, 38);
        assertEquals(9, afterStructures.nextInt(16));
        assertEquals(50, afterStructures.nextInt(128));
        assertEquals(14, afterStructures.nextInt(16),
                "ChunkProviderGenerate populates mineshafts, villages, then strongholds before lake checks");
    }

    @Test
    @DisplayName("Overworld dungeon random should resume after Release 1.0 lake branches")
    void overworldDungeonRandomResumesAfterLakeBranches() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        Random random = invokeOverworldDungeonRandom(generator, null, -20, -18);

        assertEquals(-298, -20 * Chunk.WIDTH + random.nextInt(16) + 8);
        assertEquals(126, random.nextInt(128));
        assertEquals(-277, -18 * Chunk.DEPTH + random.nextInt(16) + 8);
    }

    @Test
    @DisplayName("Overworld ore random should resume after Release 1.0 lake and dungeon phases")
    void overworldOreRandomResumesAfterLakeAndDungeonPhases() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        Random random = invokeOverworldOreRandom(generator, null, -20, -18);

        assertEquals(-310, -20 * Chunk.WIDTH + random.nextInt(16));
        assertEquals(115, random.nextInt(128));
        assertEquals(-285, -18 * Chunk.DEPTH + random.nextInt(16));
    }

    @Test
    @DisplayName("Overworld decorator random should resume after the Release 1.0 ore helper")
    void overworldDecoratorRandomResumesAfterOreHelper() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        Random random = invokeOverworldDecoratorRandom(generator, null, -20, -18);

        assertEquals(-300, -20 * Chunk.WIDTH + random.nextInt(16) + 8);
        assertEquals(-269, -18 * Chunk.DEPTH + random.nextInt(16) + 8);
    }

    @Test
    @DisplayName("Nether lava springs should use the Release 1.0 five-neighbor gate")
    void netherLavaSpringsUseSourceFiveNeighborGate() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(8, 64, 8, BlockType.NETHERRACK);
        chunk.setBlock(8, 65, 8, BlockType.NETHERRACK);
        chunk.setBlock(7, 64, 8, BlockType.NETHERRACK);
        chunk.setBlock(9, 64, 8, BlockType.NETHERRACK);
        chunk.setBlock(8, 64, 7, BlockType.NETHERRACK);
        chunk.setBlock(8, 63, 8, BlockType.NETHERRACK);

        invokeNetherLavaSpring(generator, chunk, 8, 64, 8);

        assertSame(BlockType.FLOWING_LAVA, chunk.getBlock(8, 64, 8),
                "WorldGenHellLava allows an existing netherrack target and writes moving lava");
    }

    @Test
    @DisplayName("Nether lava springs should reject the old horizontal-only loose gate")
    void netherLavaSpringsRejectLooseHorizontalGate() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(8, 65, 8, BlockType.NETHERRACK);
        chunk.setBlock(7, 64, 8, BlockType.NETHERRACK);
        chunk.setBlock(9, 64, 8, BlockType.NETHERRACK);
        chunk.setBlock(8, 64, 7, BlockType.NETHERRACK);

        invokeNetherLavaSpring(generator, chunk, 8, 64, 8);

        assertSame(BlockType.AIR, chunk.getBlock(8, 64, 8),
                "Release 1.0 counts the block below, so three horizontal walls plus two air neighbors must reject");
    }

    @Test
    @DisplayName("Nether fire should replay the Release 1.0 64-attempt scatter")
    void netherFireUsesSourceScatterAndRngCost() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(8, 63, 8, BlockType.NETHERRACK);
        ScriptedRecordingRandom random = new ScriptedRecordingRandom(0, 0, 0, 0, 0, 0);

        invokeNetherFire(generator, chunk, random, 8, 64, 8);

        assertSame(BlockType.FIRE, chunk.getBlock(8, 64, 8));
        assertEquals(List.of(8, 8, 4, 4, 8, 8), random.bounds().subList(0, 6),
                "WorldGenFire draws candidate coordinates as x, y, then z");
        assertEquals(384, random.bounds().size(),
                "WorldGenFire consumes six nextInt calls for each of its 64 scatter attempts");
    }

    @Test
    @DisplayName("Nether mushrooms should replay the WorldGenFlowers 64-attempt scatter")
    void netherMushroomsUseWorldGenFlowersScatterAndRngCost() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(8, 63, 8, BlockType.NETHERRACK);
        chunk.setBlock(9, 63, 8, BlockType.GRAVEL);
        ScriptedRecordingRandom random = new ScriptedRecordingRandom(0, 0, 0, 0, 0, 0);

        invokeNetherMushroom(generator, chunk, random, BlockType.BROWN_MUSHROOM, 8, 64, 8);
        invokeNetherMushroom(generator, chunk, new ScriptedRecordingRandom(1, 0, 0, 0, 0, 0),
                BlockType.RED_MUSHROOM, 8, 64, 8);

        assertSame(BlockType.BROWN_MUSHROOM, chunk.getBlock(8, 64, 8));
        assertSame(BlockType.RED_MUSHROOM, chunk.getBlock(9, 64, 8),
                "Release 1.0 mushroom support uses the source opaque-block rule, so gravel can support mushrooms");
        assertEquals(List.of(8, 8, 4, 4, 8, 8), random.bounds().subList(0, 6),
                "WorldGenFlowers draws mushroom candidate coordinates as x, y, then z");
        assertEquals(384, random.bounds().size(),
                "WorldGenFlowers consumes six nextInt calls for each of its 64 scatter attempts");

        Chunk brightChunk = new Chunk(0, 0);
        brightChunk.setBlock(8, 63, 8, BlockType.GRAVEL);
        brightChunk.setBlock(9, 64, 8, BlockType.GLOWSTONE);
        invokeNetherMushroom(generator, brightChunk, new ScriptedRecordingRandom(0, 0, 0, 0, 0, 0),
                BlockType.BROWN_MUSHROOM, 8, 64, 8);
        assertSame(BlockType.AIR, brightChunk.getBlock(8, 64, 8),
                "BlockMushroom.canBlockStay rejects generated Nether mushrooms at block light 13 or higher");
    }

    @Test
    @DisplayName("Nether glowstone clusters should replay the WorldGenGlowStone1 scatter")
    void netherGlowstoneUsesSourceScatterAndSingleNeighborGrowth() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(8, 65, 8, BlockType.NETHERRACK);
        ScriptedRecordingRandom random = new ScriptedRecordingRandom(1, 0, 0, 0, 0);

        invokeNetherGlowstone(generator, chunk, random, 8, 64, 8);

        assertSame(BlockType.GLOWSTONE, chunk.getBlock(8, 64, 8));
        assertSame(BlockType.GLOWSTONE, chunk.getBlock(9, 64, 8),
                "WorldGenGlowStone1 grows only from candidates touching exactly one glowstone block");
        assertEquals(List.of(8, 8, 12, 8, 8), random.bounds().subList(0, 5),
                "WorldGenGlowStone1 draws candidate coordinates as x, y, then z");
        assertEquals(7500, random.bounds().size(),
                "WorldGenGlowStone1 consumes five nextInt calls for each of its 1500 scatter attempts");
    }

    @Test
    @DisplayName("Nether decorator scratch should preserve off-target glowstone growth")
    void netherDecoratorScratchPreservesOffTargetGlowstoneGrowth() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);
        Object scratch = newNetherDecoratorScratch(generator, chunk, 0, 0);
        setNetherScratchBlock(scratch, -1, 64, 8, BlockType.AIR);
        setNetherScratchBlock(scratch, -1, 65, 8, BlockType.NETHERRACK);

        invokeNetherGlowstone(generator, chunk, scratch,
                new ScriptedRecordingRandom(1, 0, 0, 0, 0), -1, 64, 8);

        assertSame(BlockType.GLOWSTONE, chunk.getBlock(0, 64, 8),
                "An off-target seed glowstone block should stay in the decorator overlay and grow into the target");
    }

    @Test
    @DisplayName("Nether mushroom scratch light should read off-target glowstone")
    void netherMushroomScratchLightReadsOffTargetGlowstone() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(515151L, Dimension.NETHER);
        Chunk chunk = new Chunk(0, 0);
        Object darkScratch = newNetherDecoratorScratch(generator, chunk, 0, 0);
        setNetherScratchBlock(darkScratch, -1, 63, 8, BlockType.NETHERRACK);
        setNetherScratchBlock(darkScratch, -1, 64, 8, BlockType.AIR);

        invokeNetherMushroom(generator, chunk, darkScratch,
                new ScriptedRecordingRandom(0, 0, 0, 0, 0, 0),
                BlockType.BROWN_MUSHROOM, -1, 64, 8);

        assertSame(BlockType.BROWN_MUSHROOM, getNetherScratchBlock(darkScratch, -1, 64, 8),
                "Dark off-target scratch candidates should be preserved for later Nether decorators");

        Object brightScratch = newNetherDecoratorScratch(generator, chunk, 0, 0);
        setNetherScratchBlock(brightScratch, -1, 63, 8, BlockType.NETHERRACK);
        setNetherScratchBlock(brightScratch, -1, 64, 8, BlockType.AIR);
        setNetherScratchBlock(brightScratch, -2, 64, 8, BlockType.GLOWSTONE);

        invokeNetherMushroom(generator, chunk, brightScratch,
                new ScriptedRecordingRandom(0, 0, 0, 0, 0, 0),
                BlockType.BROWN_MUSHROOM, -1, 64, 8);

        assertSame(BlockType.AIR, getNetherScratchBlock(brightScratch, -1, 64, 8),
                "Off-target Nether mushrooms should reject block light 13+ from scratch glowstone");
    }

    @Test
    @DisplayName("Release 1.0 Overworld chunks should include surface detail decorators")
    void releaseOneOverworldDecoratesSurfaceDetails() {
        World world = new World(424242L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            Set<BlockType> found = findBlocks(world, 7,
                    EnumSet.of(BlockType.TALL_GRASS, BlockType.CLAY));
            assertTrue(found.contains(BlockType.TALL_GRASS), "Overworld grass biomes should place tall grass");
            assertTrue(countBlocks(world.getChunkNow(-47, -7), BlockType.SUGAR_CANE, 0, Chunk.HEIGHT - 1) > 0,
                    "Overworld shorelines should place sugar cane");
            assertTrue(found.contains(BlockType.CLAY), "Overworld shallow water should include clay disks");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 taiga decoration should keep long-grass metadata")
    void releaseOneTaigaDecorationKeepsLongGrassMetadata() {
        World world = new World(424242L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        try {
            int taigaChunkX = Math.floorDiv(-6496, Chunk.WIDTH);
            int taigaChunkZ = Math.floorDiv(-1216, Chunk.DEPTH);
            assertSame(BiomeType.TAIGA, generator.getBiome(taigaChunkX * Chunk.WIDTH + 16,
                    taigaChunkZ * Chunk.DEPTH + 16));

            boolean foundFern = false;
            boolean foundLongGrass = false;
            for (int cx = taigaChunkX - 1; cx <= taigaChunkX + 1; cx++) {
                for (int cz = taigaChunkZ - 1; cz <= taigaChunkZ + 1; cz++) {
                    Chunk chunk = world.getChunkNow(cx, cz);
                    foundFern |= chunkContainsTallGrassMetadata(chunk, 2);
                    foundLongGrass |= chunkContainsTallGrassMetadata(chunk, 1);
                }
            }

            assertTrue(foundLongGrass, "Taiga WorldGenTallGrass should place long-grass metadata 1");
            assertFalse(foundFern, "Release 1.0 BiomeDecorator should not draw taiga fern metadata");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Base terrain block lookup should not duplicate ore generation")
    void baseTerrainLookupDoesNotInjectOres() throws Exception {
        World world = new World(1234L);
        try {
            Class<?> biomeType = Class.forName("com.craftzero.world.World$BiomeType");
            Object plains = enumConstant(biomeType, "PLAINS");
            Method getBlockType = World.class.getDeclaredMethod(
                    "getBlockType", int.class, int.class, biomeType, int.class, int.class);
            getBlockType.setAccessible(true);

            for (int x = -16; x <= 16; x += 4) {
                for (int z = -16; z <= 16; z += 4) {
                    for (int y = 1; y < 50; y++) {
                        BlockType type = (BlockType) getBlockType.invoke(world, y, 70, plains, x, z);
                        assertSame(BlockType.STONE, type,
                                "Base terrain should return stone below the dirt layer; ores belong to OreGenerator");
                    }
                }
            }
        } finally {
            world.cleanup();
        }
    }

    private static Set<BiomeType> sampleBiomes(ReleaseOneWorldGenerator generator) {
        Set<BiomeType> seen = EnumSet.noneOf(BiomeType.class);
        for (int x = -4096; x <= 4096; x += 64) {
            for (int z = -4096; z <= 4096; z += 64) {
                seen.add(generator.getBiome(x, z));
            }
        }
        return seen;
    }

    private static void assertFinalBiome(ReleaseOneWorldGenerator generator, int x, int z, BiomeType expected) {
        assertSame(expected, generator.getBiome(x, z), "Unexpected final biome at " + x + "," + z);
    }

    private static void assertGenerationBiome(ReleaseOneWorldGenerator generator, int x, int z, BiomeType expected) {
        assertSame(expected, generator.getBiomeForGenerationLayer(x, z),
                "Unexpected terrain-generation biome at layer " + x + "," + z);
    }

    private static void assertBiomeHeight(BiomeType biome, float minHeight, float maxHeight) {
        assertEquals(minHeight, biome.minHeight(), 1.0E-6F, "Unexpected min height for " + biome);
        assertEquals(maxHeight, biome.maxHeight(), 1.0E-6F, "Unexpected max height for " + biome);
    }

    private static void assertTerrainVector(ReleaseOneWorldGenerator generator, int x, int z, BiomeType biome,
            int topY, BlockType topBlock, BlockType seaBlock) {
        assertSame(biome, generator.getBiome(x, z));
        assertEquals(topY, generator.terrainTopY(x, z), "Unexpected terrain top at " + x + "," + z);
        assertSame(topBlock, generator.baseBlockAt(x, topY, z));
        assertSame(seaBlock, generator.baseBlockAt(x, ReleaseOneWorldGenerator.SEA_LEVEL, z));
    }

    private static void assertGrassSpawn(ReleaseOneWorldGenerator generator,
            ReleaseOneWorldGenerator.SpawnPoint spawn) {
        int top = generator.terrainTopY(spawn.x(), spawn.z());
        assertEquals(top + 1, spawn.y());
        assertSame(BlockType.GRASS, generator.baseBlockAt(spawn.x(), top, spawn.z()));
        assertSame(BlockType.AIR, generator.baseBlockAt(spawn.x(), spawn.y(), spawn.z()));
        assertSame(BlockType.AIR, generator.baseBlockAt(spawn.x(), spawn.y() + 1, spawn.z()));
    }

    private static void assertDensityGridVector(OverworldDensityField density, int x, int y, int z,
            double expected) {
        assertEquals(0, Math.floorMod(x, 4), "Density grid x should align to the Release 4-block cell");
        assertEquals(0, Math.floorMod(y, 8), "Density grid y should align to the Release 8-block cell");
        assertEquals(0, Math.floorMod(z, 4), "Density grid z should align to the Release 4-block cell");
        assertDensityAt(density, x, y, z, expected);
    }

    private static void assertDensityAt(OverworldDensityField density, int x, int y, int z, double expected) {
        assertEquals(expected, density.density(x, y, z, BiomeType.PLAINS), 1.0E-9,
                "Unexpected raw Overworld density at " + x + "," + y + "," + z);
    }

    private static int overworldDensityHash(OverworldDensityField density, int chunkX, int chunkZ) {
        double[] values = new double[5 * 17 * 5];
        int index = 0;
        for (int gridX = 0; gridX < 5; gridX++) {
            for (int gridZ = 0; gridZ < 5; gridZ++) {
                for (int gridY = 0; gridY < 17; gridY++) {
                    values[index++] = density.density(chunkX * Chunk.WIDTH + gridX * 4,
                            gridY * 8, chunkZ * Chunk.DEPTH + gridZ * 4, BiomeType.PLAINS);
                }
            }
        }
        return Arrays.hashCode(values);
    }

    private static double[] invokeNetherDensities(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("netherDensities",
                int.class, int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (double[]) method.invoke(generator, chunkX * 4, 0, chunkZ * 4, 5, 17, 5);
    }

    private static void assertNetherDensityVector(double[] densities, int gridX, int gridY, int gridZ,
            double expected) {
        assertTrue(gridX >= 0 && gridX < 5, "Nether density grid x should fit the 5-cell source array");
        assertTrue(gridY >= 0 && gridY < 17, "Nether density grid y should fit the 17-sample source array");
        assertTrue(gridZ >= 0 && gridZ < 5, "Nether density grid z should fit the 5-cell source array");
        int index = (gridX * 5 + gridZ) * 17 + gridY;
        assertEquals(expected, densities[index], 1.0E-9,
                "Unexpected raw Nether density at grid " + gridX + "," + gridY + "," + gridZ);
    }

    private static void assertGeneratedNetherBaseChunkMatchesExpectation(NetherBaseChunkExpectation expectation)
            throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(expectation.seed(), Dimension.NETHER);
        Chunk surfaceOnly = netherSurfaceChunkBeforeCaves(generator, expectation.chunkX(), expectation.chunkZ());
        Chunk carved = generatedNetherBaseChunk(generator, expectation.chunkX(), expectation.chunkZ());
        String label = "seed " + expectation.seed() + " chunk ("
                + expectation.chunkX() + "," + expectation.chunkZ() + ")";

        int netherrackToAir = 0;
        int preservedSurfacePatches = 0;
        int patchToAir = 0;
        int airToNetherrack = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType before = surfaceOnly.getBlock(x, y, z);
                    BlockType after = carved.getBlock(x, y, z);
                    if (before == BlockType.NETHERRACK && after == BlockType.AIR) {
                        netherrackToAir++;
                    }
                    if (isNetherSurfacePatch(before) && after == before) {
                        preservedSurfacePatches++;
                    }
                    if (isNetherSurfacePatch(before) && after == BlockType.AIR) {
                        patchToAir++;
                    }
                    if (before == BlockType.AIR && after == BlockType.NETHERRACK) {
                        airToNetherrack++;
                    }
                }
            }
        }

        assertEquals(expectation.netherrackToAir(), netherrackToAir,
                label + " should carve netherrack after source surface replacement");
        assertEquals(expectation.preservedSurfacePatches(), preservedSurfacePatches,
                label + " should preserve soul-sand/gravel surface patches");
        assertEquals(0, patchToAir, label + " should not carve source surface patches to air");
        assertEquals(0, airToNetherrack, label + " cave carving must be subtractive");
        assertEquals(expectation.airCount(), countBlocks(carved, BlockType.AIR, 0, Chunk.HEIGHT - 1),
                label + " generated base chunk air count");
        assertEquals(expectation.netherrackCount(), countBlocks(carved, BlockType.NETHERRACK, 0, Chunk.HEIGHT - 1),
                label + " generated base chunk netherrack count");
        assertEquals(expectation.lavaCount(), countBlocks(carved, BlockType.LAVA, 0, Chunk.HEIGHT - 1),
                label + " generated base chunk lava count");
        assertEquals(expectation.bedrockCount(), countBlocks(carved, BlockType.BEDROCK, 0, Chunk.HEIGHT - 1),
                label + " generated base chunk bedrock count");
        assertSame(BlockType.NETHERRACK, surfaceOnly.getBlock(expectation.carvedX(),
                expectation.carvedY(), expectation.carvedZ()), label + " carved sample before caves");
        assertSame(BlockType.AIR, carved.getBlock(expectation.carvedX(),
                expectation.carvedY(), expectation.carvedZ()), label + " carved sample after caves");
        assertSame(expectation.patchType(), surfaceOnly.getBlock(expectation.patchX(),
                expectation.patchY(), expectation.patchZ()), label + " surface-patch sample before caves");
        assertSame(expectation.patchType(), carved.getBlock(expectation.patchX(),
                expectation.patchY(), expectation.patchZ()), label + " surface-patch sample after caves");
    }

    private record NetherBaseChunkExpectation(long seed, int chunkX, int chunkZ, int netherrackToAir,
            int preservedSurfacePatches, int airCount, int netherrackCount, int lavaCount, int bedrockCount,
            int carvedX, int carvedY, int carvedZ, BlockType patchType, int patchX, int patchY, int patchZ) {
    }

    private static Chunk netherSurfaceChunkBeforeCaves(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        Method terrain = ReleaseOneWorldGenerator.class.getDeclaredMethod("generateNetherTerrain",
                Chunk.class, int.class, int.class);
        terrain.setAccessible(true);
        terrain.invoke(generator, chunk, chunkX, chunkZ);

        Method surfaceRandom = ReleaseOneWorldGenerator.class.getDeclaredMethod("netherSurfaceRandom",
                int.class, int.class);
        surfaceRandom.setAccessible(true);
        Random random = (Random) surfaceRandom.invoke(generator, chunkX, chunkZ);

        Method surface = ReleaseOneWorldGenerator.class.getDeclaredMethod("replaceNetherSurface",
                Chunk.class, int.class, int.class, Random.class);
        surface.setAccessible(true);
        surface.invoke(generator, chunk, chunkX, chunkZ, random);
        return chunk;
    }

    private static Chunk generatedNetherBaseChunk(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("generateNetherBaseChunk",
                Chunk.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, chunkX, chunkZ);
        return chunk;
    }

    private static boolean isNetherSurfacePatch(BlockType block) {
        return block == BlockType.SOUL_SAND || block == BlockType.GRAVEL;
    }

    private static boolean findBlock(World world, int chunkRadius, BlockType type) {
        return findBlocks(world, chunkRadius, EnumSet.of(type)).contains(type);
    }

    private static boolean findBlockInYRange(World world, int chunkRadius, BlockType type, int minY, int maxY) {
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                if (countBlocks(world.getChunkNow(cx, cz), type, minY, maxY) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<BlockType> findBlocks(World world, int chunkRadius, Set<BlockType> types) {
        Set<BlockType> found = EnumSet.noneOf(BlockType.class);
        Set<BlockType> remaining = EnumSet.copyOf(types);
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                Chunk chunk = world.getChunkNow(cx, cz);
                collectBlocks(chunk, remaining, found);
                if (remaining.isEmpty()) {
                    return found;
                }
            }
        }
        return found;
    }

    private static void collectBlocks(Chunk chunk, Set<BlockType> remaining, Set<BlockType> found) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    BlockType block = chunk.getBlock(x, y, z);
                    if (remaining.remove(block)) {
                        found.add(block);
                        if (remaining.isEmpty()) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean chunkContainsTallGrassMetadata(Chunk chunk, int metadata) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == BlockType.TALL_GRASS
                            && chunk.getBlockMetadata(x, y, z) == metadata) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int countBlocks(Chunk chunk, BlockType type, int minY, int maxY) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = Math.max(0, minY); y <= Math.min(Chunk.HEIGHT - 1, maxY); y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static Object enumConstant(Class<?> enumType, String name) {
        for (Object value : enumType.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(name)) {
                return value;
            }
        }
        fail("Missing enum constant " + name);
        return null;
    }

    private static Random invokeNetherDecoratorRandom(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("netherDecoratorRandom",
                int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, chunkX, chunkZ);
    }

    private static Random invokeNetherPopulationRandomAfterStructures(ReleaseOneWorldGenerator generator,
            int chunkX, int chunkZ) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("netherPopulationRandomAfterStructures",
                int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, chunkX, chunkZ);
    }

    private static Random invokePopulationRandom(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("populationRandom", int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, chunkX, chunkZ);
    }

    private static Random invokeOverworldPopulationRandomAfterStructures(ReleaseOneWorldGenerator generator,
            int chunkX, int chunkZ) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("overworldPopulationRandomAfterStructures",
                int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, chunkX, chunkZ);
    }

    private static Random invokeOverworldDungeonRandom(ReleaseOneWorldGenerator generator, World world,
            int chunkX, int chunkZ) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("overworldDungeonRandom",
                World.class, int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, world, chunkX, chunkZ);
    }

    private static Random invokeOverworldOreRandom(ReleaseOneWorldGenerator generator, World world,
            int chunkX, int chunkZ) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("overworldOreRandom",
                World.class, int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, world, chunkX, chunkZ);
    }

    private static Random invokeOverworldDecoratorRandom(ReleaseOneWorldGenerator generator, World world,
            int chunkX, int chunkZ) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("overworldDecoratorRandom",
                World.class, int.class, int.class);
        method.setAccessible(true);
        return (Random) method.invoke(generator, world, chunkX, chunkZ);
    }

    private static void invokeNetherLavaSpring(ReleaseOneWorldGenerator generator, Chunk chunk,
            int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeNetherLavaSpring",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, new Random(0L), x, y, z);
    }

    private static void invokeNetherFire(ReleaseOneWorldGenerator generator, Chunk chunk,
            Random random, int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeNetherFire",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, x, y, z);
    }

    private static void invokeNetherMushroom(ReleaseOneWorldGenerator generator, Chunk chunk,
            Random random, BlockType mushroom, int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeNetherMushroom",
                Chunk.class, int.class, int.class, Random.class, BlockType.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, mushroom, x, y, z);
    }

    private static void invokeNetherMushroom(ReleaseOneWorldGenerator generator, Chunk chunk,
            Object scratch, Random random, BlockType mushroom, int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeNetherMushroom",
                Chunk.class, int.class, int.class, Random.class, netherDecoratorScratchClass(),
                BlockType.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, scratch, mushroom, x, y, z);
    }

    private static void invokeNetherGlowstone(ReleaseOneWorldGenerator generator, Chunk chunk,
            Random random, int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeGlowstoneCluster",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, x, y, z);
    }

    private static void invokeNetherGlowstone(ReleaseOneWorldGenerator generator, Chunk chunk,
            Object scratch, Random random, int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeGlowstoneCluster",
                Chunk.class, int.class, int.class, Random.class, netherDecoratorScratchClass(),
                int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, scratch, x, y, z);
    }

    private static Object newNetherDecoratorScratch(ReleaseOneWorldGenerator generator, Chunk chunk,
            int chunkX, int chunkZ) throws Exception {
        Constructor<?> constructor = netherDecoratorScratchClass().getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(generator, chunk, chunkX, chunkZ);
    }

    private static void setNetherScratchBlock(Object scratch, int x, int y, int z, BlockType type) throws Exception {
        Method method = scratch.getClass().getDeclaredMethod("setBlock",
                int.class, int.class, int.class, BlockType.class);
        method.setAccessible(true);
        method.invoke(scratch, x, y, z, type);
    }

    private static BlockType getNetherScratchBlock(Object scratch, int x, int y, int z) throws Exception {
        Method method = scratch.getClass().getDeclaredMethod("getBlock",
                int.class, int.class, int.class);
        method.setAccessible(true);
        return (BlockType) method.invoke(scratch, x, y, z);
    }

    private static Class<?> netherDecoratorScratchClass() throws ClassNotFoundException {
        return Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$NetherDecoratorScratch");
    }

    private static final class ScriptedRecordingRandom extends Random {
        private final int[] scriptedValues;
        private final List<Integer> bounds = new ArrayList<>();
        private int index;

        private ScriptedRecordingRandom(int... scriptedValues) {
            this.scriptedValues = scriptedValues;
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            if (index >= scriptedValues.length) {
                return bound / 2;
            }
            return Math.floorMod(scriptedValues[index++], bound);
        }

        private List<Integer> bounds() {
            return bounds;
        }
    }
}
