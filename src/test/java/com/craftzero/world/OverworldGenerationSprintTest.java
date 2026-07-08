package com.craftzero.world;

import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    @DisplayName("Normal tree feature should use Release-style leaf layers and random corner skips")
    void normalTreeFeatureUsesReleaseLeafShape() {
        TreeFeature.Candidate tree = new TreeFeature.Candidate(8, 65, 8, 5, 100, 0,
                TreeFeature.Kind.NORMAL, 1, 0, 0, 0);
        TreeFeature.BlockQuery flatGround = OverworldGenerationSprintTest::flatGroundBlock;
        assertTrue(tree.canPlace(flatGround));

        Chunk chunk = flatChunk(0, 0);
        tree.placeInto(chunk, 0, 0);

        assertSame(BlockType.DIRT, chunk.getBlock(8, 64, 8));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(8, 65, 8));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(8, 69, 8));
        assertSame(BlockType.AIR, chunk.getBlock(6, 67, 6),
                "Stored source RNG corner bit should skip this lower leaf corner");
        assertSame(BlockType.LEAVES, chunk.getBlock(6, 67, 10));
        assertSame(BlockType.LEAVES, chunk.getBlock(8, 70, 8));
        assertSame(BlockType.AIR, chunk.getBlock(7, 70, 7),
                "Release top leaf layer skips all corners");
        assertSame(BlockType.AIR, chunk.getBlock(8, 71, 8),
                "Release normal trees should not add the old extra top leaf layer");
    }

    @Test
    @DisplayName("Big tree feature should use Release-style leaf nodes and branch logs")
    void bigTreeFeatureUsesReleaseLeafNodesAndBranches() {
        TreeFeature.Candidate tree = new TreeFeature.Candidate(8, 65, 8, 14, 100, 0,
                TreeFeature.Kind.BIG, 0, 1, 0, 0);
        TreeFeature.BlockQuery flatGround = OverworldGenerationSprintTest::flatGroundBlock;
        assertTrue(tree.canPlace(flatGround));

        Chunk chunk = flatChunk(0, 0);
        tree.placeInto(chunk, 0, 0);

        assertSame(BlockType.DIRT, chunk.getBlock(8, 64, 8));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(8, 65, 8));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(8, 73, 8));
        assertSame(BlockType.OAK_LOG, chunk.getBlock(7, 69, 10),
                "Release big trees should place off-trunk branch log lines");
        assertSame(BlockType.OAK_LOG, chunk.getBlock(7, 70, 10));
        assertSame(BlockType.LEAVES, chunk.getBlock(6, 76, 11),
                "Release big trees should place generated leaf-node disks away from the trunk");
        assertSame(BlockType.AIR, chunk.getBlock(8, 80, 8),
                "Release big trees should not degrade into a tall normal-tree top crown");
    }

    @Test
    @DisplayName("Source tree replay should mutate the big-oak scratch overlay")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void sourceTreeReplayMutatesBigOakScratchOverlay() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = flatChunk(0, 0);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, 0, 0);
        Class<?> kindClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeKind");
        Object big = Enum.valueOf(kindClass.asSubclass(Enum.class), "BIG");
        Method advance = ReleaseOneWorldGenerator.class.getDeclaredMethod("advanceSourceTreeGenerator",
                scratchClass, Random.class, int.class, int.class, int.class, kindClass);
        advance.setAccessible(true);

        advance.invoke(generator, scratch, new Random(1234L), 8, 64, 8, big);

        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);
        assertSame(BlockType.DIRT, getBlock.invoke(scratch, 8, 63, 8));
        assertSame(BlockType.OAK_LOG, getBlock.invoke(scratch, 8, 64, 8));
        assertTrue(scratchContains(getBlock, scratch, BlockType.LEAVES, 2, 96, 2, 14, 14),
                "Replayed big oak should write source leaf-node disks into the tree scratch overlay");
    }

    @Test
    @DisplayName("Source tree scratch should see off-chunk lake side effects before trees")
    void sourceTreeScratchOverlaysOffChunkLakesBeforeTrees() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-8, -8);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, -8, -8);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);
        Method overlay = ReleaseOneWorldGenerator.class.getDeclaredMethod("overlayOverworldLakeSideEffects",
                World.class, scratchClass, int.class, int.class);
        overlay.setAccessible(true);

        assertSame(BlockType.STONE, getBlock.invoke(scratch, -149, 31, -109));
        assertSame(BlockType.STONE, getBlock.invoke(scratch, -148, 33, -110));

        overlay.invoke(generator, null, scratch, -8, -8);

        assertSame(BlockType.WATER, getBlock.invoke(scratch, -149, 31, -109),
                "Tree replay should see water lakes carved by nearby population origins outside the target chunk");
        assertSame(BlockType.AIR, getBlock.invoke(scratch, -148, 33, -110),
                "Tree replay should see upper lake cavities outside the target chunk");
        assertFalse(scratchHasTargetChunkOverlay(scratchClass, scratch, -8, -8),
                "The overlay should not overwrite the target chunk's later ore/dungeon state");
    }

    @Test
    @DisplayName("Dungeon validation should read off-chunk lake scratch state")
    void dungeonValidationReadsOffChunkLakeScratchState() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-8, -8);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, -8, -8);
        Method overlay = ReleaseOneWorldGenerator.class.getDeclaredMethod("overlayOverworldLakeSideEffects",
                World.class, scratchClass, int.class, int.class);
        overlay.setAccessible(true);
        overlay.invoke(generator, null, scratch, -8, -8);
        Method readerMethod = ReleaseOneWorldGenerator.class.getDeclaredMethod("overworldDungeonBlockReader",
                Chunk.class, int.class, int.class, scratchClass);
        readerMethod.setAccessible(true);

        DungeonGenerator.BlockReader reader = (DungeonGenerator.BlockReader) readerMethod.invoke(
                generator, chunk, -8, -8, scratch);

        assertSame(BlockType.WATER, reader.getBlock(-149, 31, -109),
                "Dungeon room checks should see off-chunk lake fluid blocks before accepting an envelope");
        assertSame(BlockType.AIR, reader.getBlock(-148, 33, -110),
                "Dungeon room checks should see off-chunk lake cavities instead of carved-only terrain");
    }

    @Test
    @DisplayName("Null-world Overworld chunks should still place visible dungeon blocks")
    void nullWorldOverworldGenerationPlacesDungeonBlocks() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-64, 25);

        generator.generateChunk(null, chunk, -64, 25);

        assertSame(BlockType.MOSSY_COBBLESTONE, chunk.getBlock(9, 44, 2));
        assertSame(BlockType.CHEST, chunk.getBlock(10, 45, 4));
        assertSame(BlockType.MOB_SPAWNER, chunk.getBlock(12, 45, 5),
                "Dungeons are block population, not only world-backed tile-entity staging");
    }

    @Test
    @DisplayName("World-backed Overworld population should run Release 1.0 creature spawning")
    void worldBackedOverworldPopulationRunsCreatureSpawning() {
        World world = new World(38L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.getChunkNow(8, 0);
            world.updateEntities(1.0f / 20.0f);

            List<Mob> mobs = world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .toList();

            assertEquals(4, mobs.size());
            assertTrue(mobs.stream().allMatch(mob -> mob.getDefinition() == MobDefinition.COW));
            assertTrue(mobs.stream().allMatch(mob -> mob.getX() >= 136.0f && mob.getX() < 152.0f));
            assertTrue(mobs.stream().allMatch(mob -> mob.getZ() >= 8.0f && mob.getZ() < 24.0f));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Forest and taiga creature population should spawn wolves")
    void worldBackedOverworldPopulationSpawnsWolvesInTaiga() {
        int chunkX = -29;
        int chunkZ = 28;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(5L, Dimension.OVERWORLD);
        assertSame(BiomeType.TAIGA, generator.getBiome(chunkX * Chunk.WIDTH + 16, chunkZ * Chunk.DEPTH + 16));

        World world = new World(5L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.getChunkNow(chunkX, chunkZ);
            world.updateEntities(1.0f / 20.0f);

            List<Mob> wolves = world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .filter(mob -> mob.getDefinition() == MobDefinition.WOLF)
                    .toList();

            assertEquals(5, wolves.size());
            assertTrue(wolves.stream().allMatch(mob -> "/textures/mob/wolf.png".equals(mob.getTexturePath())));
            assertTrue(wolves.stream().allMatch(mob -> mob.getX() >= -456.0f && mob.getX() < -440.0f));
            assertTrue(wolves.stream().allMatch(mob -> mob.getZ() >= 456.0f && mob.getZ() < 472.0f));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Worldgen creature lists should use Release 1.0 wolf spawn weights")
    void worldGenCreatureListsUseReleaseOneWolfWeights() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);

        assertWorldGenSpawnEntry(generator, BiomeType.FOREST, MobDefinition.WOLF, 5, 4, 4);
        assertWorldGenSpawnEntry(generator, BiomeType.TAIGA, MobDefinition.WOLF, 8, 4, 4);
        assertEquals(-1, worldGenCreatureWeight(generator, BiomeType.PLAINS, MobDefinition.WOLF),
                "Release 1.0 only adds wolves to forest and taiga creature spawn lists");
    }

    @Test
    @DisplayName("Worldgen creature spawning should use collision volume, not literal air cells")
    void worldGenCreatureSpawningUsesCollisionVolume() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);
        Chunk chunk = flatChunk(0, 0);

        assertTrue(invokeCanSpawnWorldGenCreature(generator, chunk, MobDefinition.COW, 8, 64, 8));

        chunk.setBlock(8, 64, 8, BlockType.TALL_GRASS, 1);
        assertTrue(invokeCanSpawnWorldGenCreature(generator, chunk, MobDefinition.COW, 8, 64, 8),
                "Release spawning checks collision boxes, so harmless ground cover should not block animals");

        chunk.setBlock(8, 64, 8, BlockType.WATER, 0);
        assertFalse(invokeCanSpawnWorldGenCreature(generator, chunk, MobDefinition.COW, 8, 64, 8),
                "Release spawning rejects liquid inside the mob volume");

        chunk.setBlock(8, 64, 8, BlockType.AIR, 0);
        chunk.setBlock(8, 65, 8, BlockType.STONE, 0);
        assertFalse(invokeCanSpawnWorldGenCreature(generator, chunk, MobDefinition.COW, 8, 64, 8),
                "Release spawning rejects actual collision in the mob body volume");
    }

    @Test
    @DisplayName("Worldgen creature spawning should read post-decoration scratch state")
    void worldGenCreatureSpawningReadsDecoratorScratchState() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(38L, Dimension.OVERWORLD);
        Chunk chunk = flatChunk(0, 0);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, 0, 0);
        Method setBlock = scratchClass.getDeclaredMethod("setBlock", int.class, int.class, int.class, BlockType.class);
        setBlock.setAccessible(true);

        setBlock.invoke(scratch, 16, 63, 8, BlockType.GRASS);
        setBlock.invoke(scratch, 16, 64, 8, BlockType.AIR);
        setBlock.invoke(scratch, 16, 65, 8, BlockType.AIR);

        assertTrue(invokeCanSpawnWorldGenCreature(generator, chunk, MobDefinition.COW, 16, 64, 8, scratch),
                "One-time creature spawning should see decorated off-chunk grass in the shifted spawn area");

        setBlock.invoke(scratch, 16, 65, 8, BlockType.STONE);

        assertFalse(invokeCanSpawnWorldGenCreature(generator, chunk, MobDefinition.COW, 16, 64, 8, scratch),
                "One-time creature spawning should reject off-chunk scratch obstructions from late decoration");
    }

    @Test
    @DisplayName("Mushroom island creature population should spawn mooshrooms")
    void worldBackedOverworldPopulationSpawnsMooshroomsOnMushroomIslands() {
        int chunkX = -1877;
        int chunkZ = -349;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(5L, Dimension.OVERWORLD);
        assertSame(BiomeType.MUSHROOM_ISLAND,
                generator.getBiome(chunkX * Chunk.WIDTH + 16, chunkZ * Chunk.DEPTH + 16));

        World world = new World(5L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.getChunkNow(chunkX, chunkZ);
            world.updateEntities(1.0f / 20.0f);

            List<Mob> mooshrooms = world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .filter(mob -> mob.getDefinition() == MobDefinition.MOOSHROOM)
                    .toList();

            assertEquals(7, mooshrooms.size());
            assertTrue(mooshrooms.stream().allMatch(mob -> "/textures/mob/redcow.png".equals(mob.getTexturePath())));
            assertTrue(mooshrooms.stream().allMatch(mob -> generator.getBiome(
                    (int) Math.floor(mob.getX()), (int) Math.floor(mob.getZ())) == BiomeType.MUSHROOM_ISLAND));
            assertTrue(mooshrooms.stream().allMatch(mob -> hasMyceliumUnderMobFoot(generator, mob)));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lake validation should read mutable source scratch state")
    void lakeValidationReadsMutableScratchState() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = stoneChunk(0, 0);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, 0, 0);
        Class<?> lakeClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$LakeCandidate");
        Constructor<?> lakeConstructor = lakeClass.getDeclaredConstructor(
                int.class, int.class, int.class, BlockType.class, boolean[].class, boolean[].class);
        lakeConstructor.setAccessible(true);
        boolean[] mask = new boolean[16 * 16 * 8];
        mask[(1 * 16 + 1) * 8 + 4] = true;
        Object lake = lakeConstructor.newInstance(0, 20, 0, BlockType.WATER, mask, null);
        Method validate = ReleaseOneWorldGenerator.class.getDeclaredMethod("validateLakeCandidate",
                scratchClass, lakeClass);
        validate.setAccessible(true);
        Method setBlock = scratchClass.getDeclaredMethod("setBlock", int.class, int.class, int.class, BlockType.class);
        setBlock.setAccessible(true);

        assertTrue((boolean) validate.invoke(generator, scratch, lake));

        setBlock.invoke(scratch, 0, 24, 1, BlockType.WATER);

        assertFalse((boolean) validate.invoke(generator, scratch, lake),
                "Later lake validation should reject fluid boundaries from earlier scratch-applied lakes");
    }

    @Test
    @DisplayName("Decorator scratch should include off-chunk ore side effects before late features")
    void decoratorScratchOverlaysOffChunkOresBeforeLateFeatures() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Method sourceScratch = ReleaseOneWorldGenerator.class.getDeclaredMethod("sourceDecoratorScratch",
                World.class, Chunk.class, int.class, int.class);
        sourceScratch.setAccessible(true);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);

        assertSame(BlockType.STONE, generator.baseBlockAt(-1, 31, -8));

        Object scratch = sourceScratch.invoke(generator, null, chunk, 0, 0);

        assertSame(BlockType.IRON_ORE, getBlock.invoke(scratch, -1, 31, -8),
                "Tree/detail replay should see off-chunk ores produced earlier in the source population order");
        assertFalse(scratchHasTargetChunkOverlay(scratchClass, scratch, 0, 0),
                "Ore side-effect replay should not overwrite the target chunk's generated ore state");
    }

    @Test
    @DisplayName("Overworld ore population should carry off-chunk mutations in the live scratch")
    void overworldOrePopulationCarriesOffChunkScratchMutations() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, 0, 0);
        Method placeOres = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeOverworldOres",
                World.class, Chunk.class, int.class, int.class, scratchClass);
        placeOres.setAccessible(true);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);

        assertSame(BlockType.STONE, generator.baseBlockAt(-1, 31, -8));

        placeOres.invoke(generator, null, chunk, 0, 0, scratch);

        assertSame(BlockType.IRON_ORE, getBlock.invoke(scratch, -1, 31, -8),
                "The live population scratch should preserve ore mutations that later decorators can read");
    }

    @Test
    @DisplayName("Decorator scratch should include off-chunk dungeon side effects before late features")
    void decoratorScratchOverlaysOffChunkDungeonsBeforeLateFeatures() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-63, 25);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Method sourceScratch = ReleaseOneWorldGenerator.class.getDeclaredMethod("sourceDecoratorScratch",
                World.class, Chunk.class, int.class, int.class);
        sourceScratch.setAccessible(true);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);

        Object scratch = sourceScratch.invoke(generator, null, chunk, -63, 25);

        assertSame(BlockType.MOSSY_COBBLESTONE, getBlock.invoke(scratch, -1015, 44, 402));
        assertSame(BlockType.CHEST, getBlock.invoke(scratch, -1014, 45, 404));
        assertSame(BlockType.MOB_SPAWNER, getBlock.invoke(scratch, -1012, 45, 405));
        assertSame(BlockType.COBBLESTONE, getBlock.invoke(scratch, -1009, 45, 405));
        assertFalse(scratchHasTargetChunkOverlay(scratchClass, scratch, -63, 25),
                "Dungeon side-effect replay should not overwrite the target chunk's generated state");
    }

    @Test
    @DisplayName("Decorator scratch should include off-chunk structure side effects before late features")
    void decoratorScratchOverlaysOffChunkStructuresBeforeLateFeatures() throws Exception {
        long seed = 38L;
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, Dimension.OVERWORLD);
        int targetChunkX = -6;
        int targetChunkZ = -44;
        int structureChunkX = -5;
        int structureChunkZ = -44;
        Chunk chunk = new Chunk(targetChunkX, targetChunkZ);
        StructureSample sample = firstGeneratedStructureBlock(generator, seed, structureChunkX, structureChunkZ);

        assertNotNull(sample, "Expected deterministic neighboring stronghold blocks to sample");

        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Method sourceScratch = ReleaseOneWorldGenerator.class.getDeclaredMethod("sourceDecoratorScratch",
                World.class, Chunk.class, int.class, int.class);
        sourceScratch.setAccessible(true);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);
        Method getMetadata = scratchClass.getDeclaredMethod("getMetadata", int.class, int.class, int.class);
        getMetadata.setAccessible(true);

        Object scratch = sourceScratch.invoke(generator, null, chunk, targetChunkX, targetChunkZ);

        assertSame(sample.type(), getBlock.invoke(scratch, sample.x(), sample.y(), sample.z()),
                "Late population replay should see neighboring structure blocks before decorators run");
        assertEquals(sample.metadata(), getMetadata.invoke(scratch, sample.x(), sample.y(), sample.z()));
        assertFalse(scratchHasTargetChunkOverlay(scratchClass, scratch, targetChunkX, targetChunkZ),
                "Structure side-effect replay should not overwrite the target chunk's generated state");
    }

    @Test
    @DisplayName("Late decorators should read off-chunk source scratch state")
    void lateDecoratorsReadOffChunkSourceScratchState() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(0, 89, 8, BlockType.SAND);

        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, 0, 0);
        Method setBlock = scratchClass.getDeclaredMethod("setBlock", int.class, int.class, int.class, BlockType.class);
        setBlock.setAccessible(true);
        Method canStayWithoutScratch = ReleaseOneWorldGenerator.class.getDeclaredMethod("canGeneratedPlantStay",
                Chunk.class, int.class, int.class, int.class, int.class, int.class, BlockType.class);
        canStayWithoutScratch.setAccessible(true);
        Method canStayWithScratch = ReleaseOneWorldGenerator.class.getDeclaredMethod("canGeneratedPlantStay",
                Chunk.class, int.class, int.class, int.class, int.class, int.class, BlockType.class, scratchClass);
        canStayWithScratch.setAccessible(true);

        assertFalse((boolean) canStayWithoutScratch.invoke(generator,
                chunk, 0, 0, 0, 90, 8, BlockType.SUGAR_CANE));

        setBlock.invoke(scratch, -1, 89, 8, BlockType.WATER);

        assertTrue((boolean) canStayWithScratch.invoke(generator,
                chunk, 0, 0, 0, 90, 8, BlockType.SUGAR_CANE, scratch),
                "Late reed/cactus/plant decorators should see off-chunk lakes and trees from the source scratch");
    }

    @Test
    @DisplayName("Late decorator light gates should read off-chunk scratch emitters")
    void lateDecoratorLightGatesReadOffChunkScratchEmitters() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(0, 63, 8, BlockType.DIRT);
        chunk.setBlock(0, 70, 8, BlockType.STONE);

        Object scratch = newSourceTreeScratch(generator, chunk, 0, 0);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 0, 64, 8, BlockType.BROWN_MUSHROOM, scratch),
                "Covered mushrooms should be allowed in dark generated block light");

        setScratchBlock(scratch, -1, 64, 8, BlockType.TORCH, Block.FACE_EAST);

        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 0, 64, 8, BlockType.BROWN_MUSHROOM, scratch),
                "BlockFlower checks should see off-chunk generated light from the mutable population scratch");

        Object offTargetScratch = newSourceTreeScratch(generator, chunk, 0, 0);
        setScratchBlock(offTargetScratch, -1, 63, 8, BlockType.DIRT, 0);
        setScratchBlock(offTargetScratch, -1, 70, 8, BlockType.STONE, 0);

        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, -1, 64, 8, BlockType.BROWN_MUSHROOM,
                offTargetScratch), "Off-target scratch-backed decorator placements should validate in dark light");

        setScratchBlock(offTargetScratch, -2, 64, 8, BlockType.TORCH, Block.FACE_EAST);

        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, -1, 64, 8, BlockType.BROWN_MUSHROOM,
                offTargetScratch), "Off-target scratch placements should use world-coordinate generated light");
    }

    @Test
    @DisplayName("Huge mushrooms should spill from off-chunk source scratch centers")
    void hugeMushroomsSpillFromOffChunkSourceScratchCenters() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        int centerX = -19232;
        int centerY = 64;
        int centerZ = 16360;
        int targetChunkX = -1203;
        int targetChunkZ = 1022;
        Chunk chunk = new Chunk(targetChunkX, targetChunkZ);

        assertSame(BiomeType.MUSHROOM_ISLAND_SHORE, generator.getBiome(centerX, centerZ));

        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, targetChunkX, targetChunkZ);
        Method setBlock = scratchClass.getDeclaredMethod("setBlock", int.class, int.class, int.class, BlockType.class);
        setBlock.setAccessible(true);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);
        Method placeHugeMushroom = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeHugeMushroom",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class, scratchClass);
        placeHugeMushroom.setAccessible(true);

        setBlock.invoke(scratch, centerX, centerY - 1, centerZ, BlockType.MYCELIUM);
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int y = centerY; y <= centerY + 5; y++) {
                for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                    setBlock.invoke(scratch, x, y, z, BlockType.AIR);
                }
            }
        }
        for (int y = centerY + 6; y < Chunk.HEIGHT; y++) {
            setBlock.invoke(scratch, centerX, y, centerZ, BlockType.AIR);
        }

        placeHugeMushroom.invoke(generator, chunk, targetChunkX, targetChunkZ,
                new ScriptedRecordingRandom(0, 0), centerX, centerZ, scratch);

        assertSame(BlockType.DIRT, getBlock.invoke(scratch, centerX, centerY - 1, centerZ),
                "The off-chunk mushroom stem should still mutate source support in the scratch overlay");
        assertSame(BlockType.BROWN_MUSHROOM_BLOCK, getBlock.invoke(scratch, centerX, centerY, centerZ));
        assertSame(BlockType.BROWN_MUSHROOM_BLOCK, chunk.getBlock(15, centerY + 4, 8),
                "A cap centered just outside the chunk should spill into the target chunk");
    }

    @Test
    @DisplayName("Red huge mushroom caps should keep source upper interior cells")
    void redHugeMushroomCapsKeepSourceUpperInteriorCells() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        int chunkX = -1201;
        int chunkZ = 1026;
        int blockX = -19208;
        int blockZ = 16428;
        int localX = blockX - chunkX * Chunk.WIDTH;
        int localZ = blockZ - chunkZ * Chunk.DEPTH;
        Chunk chunk = flatChunk(chunkX, chunkZ);
        chunk.setBlock(localX, 63, localZ, BlockType.MYCELIUM);
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeHugeMushroom",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class);
        method.setAccessible(true);

        assertSame(BiomeType.MUSHROOM_ISLAND, generator.getBiome(blockX, blockZ));

        method.invoke(generator, chunk, chunkX, chunkZ, new ScriptedRecordingRandom(1, 0), blockX, blockZ);

        assertSame(BlockType.RED_MUSHROOM_BLOCK, chunk.getBlock(localX, 67, localZ),
                "The source cap condition only skips metadata-0 interiors below the second-highest red cap layer");
        assertEquals(0, chunk.getBlockMetadata(localX, 67, localZ));
        assertSame(BlockType.RED_MUSHROOM_BLOCK, chunk.getBlock(localX, 66, localZ));
        assertEquals(10, chunk.getBlockMetadata(localX, 66, localZ),
                "Lower skipped cap interiors should still be filled by the source stem pass");
    }

    @Test
    @DisplayName("Swamp tree feature should use Release-style crown corners")
    void swampTreeFeatureUsesReleaseLeafShape() {
        TreeFeature.Candidate tree = new TreeFeature.Candidate(8, 65, 8, 5, 100, 0,
                TreeFeature.Kind.SWAMP, 1, 0, 2, 0);
        TreeFeature.BlockQuery flatGround = OverworldGenerationSprintTest::flatGroundBlock;
        assertTrue(tree.canPlace(flatGround));

        Chunk chunk = flatChunk(0, 0);
        tree.placeInto(chunk, 0, 0);

        assertSame(BlockType.DIRT, chunk.getBlock(8, 64, 8));
        assertSame(BlockType.LEAVES, chunk.getBlock(5, 67, 5),
                "Stored source RNG corner bit should keep this lower swamp leaf corner");
        assertSame(BlockType.AIR, chunk.getBlock(5, 67, 11),
                "Unset source RNG corner bit should skip this lower swamp leaf corner");
        assertSame(BlockType.AIR, chunk.getBlock(6, 70, 6),
                "Release swamp top leaf layer skips all corners");
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
    @DisplayName("Overworld density should preserve release-era ocean, plain, and hill height separation")
    void overworldDensityUsesBiomeBlendedHeightField() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        int[] ocean = findBiomeColumn(generator, BiomeType.OCEAN);
        int[] plains = findHighBiomeColumn(generator, BiomeType.PLAINS,
                ReleaseOneWorldGenerator.SEA_LEVEL - 2);
        int[] hills = findHighBiomeColumn(generator, BiomeType.EXTREME_HILLS,
                ReleaseOneWorldGenerator.SEA_LEVEL + 10);

        assertNotNull(ocean, "Expected a deterministic ocean sample near spawn");
        assertNotNull(plains, "Expected a deterministic plains sample near spawn");
        assertNotNull(hills, "Expected a deterministic mountain sample near spawn");

        int oceanTop = generator.terrainTopY(ocean[0], ocean[1]);
        int plainsTop = generator.terrainTopY(plains[0], plains[1]);
        int hillsTop = generator.terrainTopY(hills[0], hills[1]);

        assertTrue(oceanTop < ReleaseOneWorldGenerator.SEA_LEVEL);
        assertTrue(plainsTop >= ReleaseOneWorldGenerator.SEA_LEVEL - 2);
        assertTrue(hillsTop > plainsTop + 4,
                "Biome min/max height blending should lift hill terrain above nearby lowlands");
    }

    @Test
    @DisplayName("Ice should generate only on frozen biome sea-level water")
    void iceOnlyGeneratesInFrozenWaterBiomes() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        int[] ordinary = findWaterColumn(generator, false);
        int[] frozen = findWaterColumn(generator, true);

        assertNotNull(ordinary, "Expected at least one ordinary water column");
        assertNotNull(frozen, "Expected at least one frozen water column");
        assertSame(BlockType.WATER, generator.baseBlockAt(ordinary[0], ReleaseOneWorldGenerator.SEA_LEVEL, ordinary[1]));
        assertSame(BlockType.ICE, generator.baseBlockAt(frozen[0], ReleaseOneWorldGenerator.SEA_LEVEL, frozen[1]));
        assertFalse(BiomeType.OCEAN.canFreezeWater());
        assertFalse(BiomeType.RIVER.canFreezeWater());
        assertFalse(BiomeType.TAIGA.canFreezeWater());
        assertFalse(BiomeType.PLAINS.canFreezeWater());
    }

    @Test
    @DisplayName("Frozen biome surfaces should generate snow layers, not snow blocks")
    void frozenBiomeSurfacesGenerateSnowLayers() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        int[] frozenChunk = findFrozenSurfaceChunk(generator);
        assertNotNull(frozenChunk, "Expected deterministic frozen surface chunk");
        int chunkX = frozenChunk[0];
        int chunkZ = frozenChunk[1];
        Chunk chunk = new Chunk(chunkX, chunkZ);

        generator.generateChunk(null, chunk, chunkX, chunkZ);

        int snowLayers = 0;
        int snowBlocks = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 1; y < Chunk.HEIGHT - 1; y++) {
                    BlockType block = chunk.getBlock(x, y, z);
                    if (block == BlockType.SNOW) {
                        snowBlocks++;
                    }
                    if (block != BlockType.SNOW_LAYER) {
                        continue;
                    }
                    snowLayers++;
                    int blockX = chunkX * Chunk.WIDTH + x;
                    int blockZ = chunkZ * Chunk.DEPTH + z;
                    BlockType support = chunk.getBlock(x, y - 1, z);
                    assertTrue(generator.getBiome(blockX, blockZ).isFrozen());
                    assertTrue(support == BlockType.LEAVES || BlockShape.isOpaqueCube(support));
                    assertSame(BlockType.AIR, chunk.getBlock(x, y + 1, z));
                }
            }
        }

        assertTrue(snowLayers > 0, "Expected deterministic frozen chunk to contain surface snow layers");
        assertEquals(0, snowBlocks, "Generated surface snow should use ID 78 snow layers, not ID 80 snow blocks");
    }

    @Test
    @DisplayName("Final shifted ice/snow pass should freeze exposed frozen-biome water")
    void finalIceAndSnowPassFreezesExposedWater() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        int[] frozenWater = findWaterColumn(generator, true);
        assertNotNull(frozenWater, "Expected deterministic frozen water column");
        int chunkX = Math.floorDiv(frozenWater[0], Chunk.WIDTH);
        int chunkZ = Math.floorDiv(frozenWater[1], Chunk.DEPTH);
        int localX = Math.floorMod(frozenWater[0], Chunk.WIDTH);
        int localZ = Math.floorMod(frozenWater[1], Chunk.DEPTH);
        Chunk chunk = new Chunk(chunkX, chunkZ);
        chunk.setBlock(localX, ReleaseOneWorldGenerator.SEA_LEVEL, localZ, BlockType.WATER);

        invokeIceAndSnowFinishing(generator, chunk, chunkX, chunkZ);

        assertSame(BlockType.ICE, chunk.getBlock(localX, ReleaseOneWorldGenerator.SEA_LEVEL, localZ));
        assertSame(BlockType.AIR, chunk.getBlock(localX, ReleaseOneWorldGenerator.SEA_LEVEL + 1, localZ),
                "Release 1.0 rejects snow placement directly on ice in the same finishing pass");
    }

    @Test
    @DisplayName("Final shifted ice/snow pass should honor source fluid and block-light gates")
    void finalIceAndSnowPassHonorsSourceFluidAndLightGates() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        int[] frozenColumn = findInteriorFrozenColumn(generator);
        assertNotNull(frozenColumn, "Expected deterministic interior frozen-biome column");
        int chunkX = Math.floorDiv(frozenColumn[0], Chunk.WIDTH);
        int chunkZ = Math.floorDiv(frozenColumn[1], Chunk.DEPTH);
        int localX = Math.floorMod(frozenColumn[0], Chunk.WIDTH);
        int localZ = Math.floorMod(frozenColumn[1], Chunk.DEPTH);
        int surfaceY = ReleaseOneWorldGenerator.SEA_LEVEL;

        Chunk brightWater = new Chunk(chunkX, chunkZ);
        brightWater.setBlock(localX, surfaceY, localZ, BlockType.WATER, 0);
        brightWater.setBlock(localX + 1, surfaceY + 1, localZ, BlockType.TORCH, Block.FACE_WEST);
        invokeIceAndSnowFinishing(generator, brightWater, chunkX, chunkZ);

        assertSame(BlockType.WATER, brightWater.getBlock(localX, surfaceY, localZ),
                "Release 1.0 canBlockFreeze rejects source water under bright block light");

        Chunk brightGround = new Chunk(chunkX, chunkZ);
        brightGround.setBlock(localX, surfaceY, localZ, BlockType.STONE);
        brightGround.setBlock(localX + 1, surfaceY + 1, localZ, BlockType.TORCH, Block.FACE_WEST);
        invokeIceAndSnowFinishing(generator, brightGround, chunkX, chunkZ);

        assertSame(BlockType.AIR, brightGround.getBlock(localX, surfaceY + 1, localZ),
                "Release 1.0 canSnowAt rejects snow placement under bright block light");

        Chunk flowingWater = new Chunk(chunkX, chunkZ);
        flowingWater.setBlock(localX, surfaceY, localZ, BlockType.FLOWING_WATER, 3);
        invokeIceAndSnowFinishing(generator, flowingWater, chunkX, chunkZ);

        assertSame(BlockType.FLOWING_WATER, flowingWater.getBlock(localX, surfaceY, localZ),
                "Release 1.0 canBlockFreeze only freezes level-0 water");
        assertEquals(3, flowingWater.getBlockMetadata(localX, surfaceY, localZ));

        Chunk coveredWater = new Chunk(chunkX, chunkZ);
        coveredWater.setBlock(localX, surfaceY, localZ, BlockType.WATER, 0);
        coveredWater.setBlock(localX, surfaceY + 1, localZ, BlockType.LILY_PAD, 0);
        invokeIceAndSnowFinishing(generator, coveredWater, chunkX, chunkZ);

        assertSame(BlockType.ICE, coveredWater.getBlock(localX, surfaceY, localZ),
                "Release 1.0 canBlockFreeze does not require air above the level-0 water");
        assertSame(BlockType.AIR, coveredWater.getBlock(localX, surfaceY + 1, localZ),
                "Release setBlockWithNotify removes a lily pad after freezing its water support");
    }

    @Test
    @DisplayName("Final shifted ice/snow pass should read off-chunk scratch block light")
    void finalIceAndSnowPassReadsOffChunkScratchBlockLight() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        int[] frozenColumn = findFrozenBorderWaterColumn(generator);
        assertNotNull(frozenColumn, "Expected deterministic frozen water on a chunk border");
        int blockX = frozenColumn[0];
        int blockZ = frozenColumn[1];
        int torchX = frozenColumn[2];
        int torchZ = frozenColumn[3];
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        int localX = Math.floorMod(blockX, Chunk.WIDTH);
        int localZ = Math.floorMod(blockZ, Chunk.DEPTH);
        int surfaceY = ReleaseOneWorldGenerator.SEA_LEVEL;

        Chunk dark = new Chunk(chunkX, chunkZ);
        dark.setBlock(localX, surfaceY, localZ, BlockType.WATER, 0);
        invokeIceAndSnowFinishing(generator, dark, chunkX, chunkZ);
        assertSame(BlockType.ICE, dark.getBlock(localX, surfaceY, localZ));

        Chunk lit = new Chunk(chunkX, chunkZ);
        lit.setBlock(localX, surfaceY, localZ, BlockType.WATER, 0);
        Object scratch = newSourceTreeScratch(generator, lit, chunkX, chunkZ);
        setScratchBlock(scratch, torchX, surfaceY + 1, torchZ, BlockType.TORCH, Block.FACE_WEST);
        invokeIceAndSnowFinishing(generator, lit, chunkX, chunkZ, scratch);

        assertSame(BlockType.WATER, lit.getBlock(localX, surfaceY, localZ),
                "Release 1.0 final freezing checks world block light, including neighboring populated chunks");
    }

    @Test
    @DisplayName("Overworld population should carve Release-style water and lava lakes")
    void overworldPopulationCarvesLakes() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        Chunk waterChunk = new Chunk(-6, 1);
        Chunk lavaChunk = new Chunk(-5, 5);

        generator.generateChunk(null, waterChunk, -6, 1);
        generator.generateChunk(null, lavaChunk, -5, 5);

        assertSame(BlockType.WATER, waterChunk.getBlock(12, 30, 14));
        assertSame(BlockType.AIR, waterChunk.getBlock(12, 31, 14));
        assertSame(BlockType.LAVA, lavaChunk.getBlock(3, 32, 15));
        assertSame(BlockType.AIR, lavaChunk.getBlock(3, 33, 15));
    }

    @Test
    @DisplayName("Overworld lake population should keep Release 1.0 desert water-lake attempts")
    void overworldPopulationAttemptsWaterLakesInDeserts() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-220, -159);

        for (int originX = -221; originX <= -220; originX++) {
            for (int originZ = -160; originZ <= -159; originZ++) {
                assertSame(BiomeType.DESERT, generator.getBiome(originX * Chunk.WIDTH + 16,
                        originZ * Chunk.DEPTH + 16));
            }
        }
        assertSame(BiomeType.DESERT, generator.getBiome(-220 * Chunk.WIDTH, -159 * Chunk.DEPTH + 12));

        generator.generateChunk(null, chunk, -220, -159);

        assertSame(BlockType.WATER, chunk.getBlock(0, 39, 12));
        assertSame(BlockType.AIR, chunk.getBlock(0, 40, 12));
    }

    @Test
    @DisplayName("Rejected low lake attempts should still consume source ellipsoid RNG")
    void rejectedLowLakeAttemptsConsumeSourceMaskRandom() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        CountingHalfRandom random = new CountingHalfRandom();
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("buildLakeCandidate",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class, int.class, BlockType.class);
        method.setAccessible(true);

        Object lake = method.invoke(generator, chunk, 0, 0, random, 8, 0, 8, BlockType.WATER);

        assertNull(lake);
        assertEquals(1, random.nextIntCalls(),
                "WorldGenLakes draws nextInt(4)+4 for ellipsoid count before boundary validation");
        assertEquals(36, random.nextDoubleCalls(),
                "Six source doubles should be consumed for each of the six deterministic ellipsoids");
    }

    @Test
    @DisplayName("Overworld decoration should place Release-style cave liquid springs")
    void overworldDecorationPlacesLiquidSprings() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk waterChunk = new Chunk(-25, -16);
        Chunk lavaChunk = new Chunk(-25, -17);

        generator.generateChunk(null, waterChunk, -25, -16);
        generator.generateChunk(null, lavaChunk, -25, -17);

        assertSame(BlockType.FLOWING_WATER, waterChunk.getBlock(10, 16, 0));
        assertSame(BlockType.FLOWING_LAVA, lavaChunk.getBlock(2, 47, 10));
    }

    @Test
    @DisplayName("Overworld decoration should replace underwater dirt with sand and clay disks")
    void overworldDecorationPlacesUnderwaterDisks() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk sandChunk = new Chunk(0, 0);
        Chunk clayChunk = new Chunk(0, -6);

        generator.generateChunk(null, sandChunk, 0, 0);
        generator.generateChunk(null, clayChunk, 0, -6);

        assertSame(BlockType.DIRT, generator.baseBlockAt(12, 61, 0));
        assertSame(BlockType.SAND, sandChunk.getBlock(12, 61, 0));
        assertSame(BlockType.DIRT, generator.baseBlockAt(6, 62, -96));
        assertSame(BlockType.CLAY, clayChunk.getBlock(6, 62, 0));
    }

    @Test
    @DisplayName("Underwater disks should start at the first water block above the floor")
    void underwaterDisksUseFloorWaterHeight() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        for (int y = 0; y <= 61; y++) {
            chunk.setBlock(8, y, 8, y == 61 ? BlockType.DIRT : BlockType.STONE);
        }
        for (int y = 62; y <= 66; y++) {
            chunk.setBlock(8, y, 8, BlockType.WATER);
        }
        ScriptedRecordingRandom random = new ScriptedRecordingRandom(0, 0, 0);

        invokeUnderwaterDisk(generator, chunk, random, BlockType.SAND, 7, 2);

        assertSame(BlockType.SAND, chunk.getBlock(8, 61, 8),
                "World.f returns the first water block above the solid floor, not the top water surface");
        assertSame(BlockType.WATER, chunk.getBlock(8, 66, 8));
        assertEquals(List.of(16, 16, 5), random.bounds());
    }

    @Test
    @DisplayName("Underwater disks should read and write off-chunk source scratch state")
    void underwaterDisksUseOffChunkSourceScratchState() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> scratchConstructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        scratchConstructor.setAccessible(true);
        Object scratch = scratchConstructor.newInstance(generator, chunk, 0, 0);
        Method setBlock = scratchClass.getDeclaredMethod("setBlock", int.class, int.class, int.class, BlockType.class);
        setBlock.setAccessible(true);
        Method getBlock = scratchClass.getDeclaredMethod("getBlock", int.class, int.class, int.class);
        getBlock.setAccessible(true);
        for (int y = 0; y <= 61; y++) {
            setBlock.invoke(scratch, -1, y, 8, y == 61 ? BlockType.DIRT : BlockType.STONE);
        }
        for (int y = 62; y <= 66; y++) {
            setBlock.invoke(scratch, -1, y, 8, BlockType.WATER);
        }
        for (int y = 67; y < Chunk.HEIGHT; y++) {
            setBlock.invoke(scratch, -1, y, 8, BlockType.AIR);
        }
        Method startY = ReleaseOneWorldGenerator.class.getDeclaredMethod("underwaterDiskStartY",
                Chunk.class, int.class, int.class, int.class, int.class, scratchClass);
        startY.setAccessible(true);
        Method disk = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeUnderwaterDisk",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class,
                BlockType.class, int.class, int.class, scratchClass);
        disk.setAccessible(true);

        assertEquals(62, startY.invoke(generator, chunk, 0, 0, -1, 8, scratch));

        disk.invoke(generator, chunk, 0, 0, new ScriptedRecordingRandom(7, 0, 0),
                -1, 0, BlockType.SAND, 7, 2, scratch);

        assertSame(BlockType.SAND, getBlock.invoke(scratch, -1, 61, 8),
                "Disk placement should record valid off-chunk replacements for later source decorators");
    }

    @Test
    @DisplayName("Mushroom island decorators should place Release-style huge mushrooms")
    void mushroomIslandDecoratorPlacesHugeMushrooms() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-1201, 1026);

        assertSame(BiomeType.MUSHROOM_ISLAND, generator.getBiome(-19208, 16428));

        generator.generateChunk(null, chunk, -1201, 1026);

        assertSame(BlockType.BROWN_MUSHROOM_BLOCK, chunk.getBlock(5, 72, 10),
                "Huge mushroom caps should decorate corrected source-shaped mushroom islands");
        assertEquals(1, chunk.getBlockMetadata(5, 72, 10),
                "Brown huge mushroom caps should use the source edge metadata");
        assertSame(BlockType.BROWN_MUSHROOM_BLOCK, chunk.getBlock(6, 72, 12));
        assertEquals(5, chunk.getBlockMetadata(6, 72, 12));
        assertSame(BlockType.BROWN_MUSHROOM_BLOCK, chunk.getBlock(8, 66, 12));
        assertEquals(10, chunk.getBlockMetadata(8, 66, 12));
    }

    @Test
    @DisplayName("Biome decorators should use source-shaped detail scatter passes")
    void biomeDecoratorsUseSourceShapedDetailScatter() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk plains = new Chunk(-20, -20);
        Chunk taiga = new Chunk(-58, 0);
        Chunk detailChunk = new Chunk(-84, -37);
        Chunk desertDetail = new Chunk(-35, -4);
        Chunk reedChunk = new Chunk(-47, -7);
        Chunk swampLily = new Chunk(-60, 5);
        Chunk deadBushChunk = new Chunk(-360, -249);

        generator.generateChunk(null, plains, -20, -20);
        generator.generateChunk(null, taiga, -58, 0);
        generator.generateChunk(null, detailChunk, -84, -37);
        generator.generateChunk(null, desertDetail, -35, -4);
        generator.generateChunk(null, reedChunk, -47, -7);
        generator.generateChunk(null, swampLily, -60, 5);
        generator.generateChunk(null, deadBushChunk, -360, -249);

        assertSame(BlockType.YELLOW_FLOWER, detailChunk.getBlock(1, 65, 7));
        assertSame(BlockType.TALL_GRASS, plains.getBlock(4, 63, 1));
        assertEquals(1, plains.getBlockMetadata(4, 63, 1));
        assertSame(BiomeType.TAIGA, generator.getBiome(-58 * Chunk.WIDTH + 5, 12));
        assertSame(BlockType.TALL_GRASS, taiga.getBlock(5, 68, 12));
        assertEquals(1, taiga.getBlockMetadata(5, 68, 12),
                "Release 1.0 BiomeDecorator passes long-grass metadata 1 for every biome");
        assertSame(BlockType.SUGAR_CANE, reedChunk.getBlock(4, 70, 3));
        assertSame(BlockType.SUGAR_CANE, reedChunk.getBlock(4, 71, 3));
        assertSame(BlockType.SUGAR_CANE, reedChunk.getBlock(4, 72, 3));
        assertSame(BlockType.CACTUS, desertDetail.getBlock(11, 67, 6));
        assertSame(BlockType.LILY_PAD, swampLily.getBlock(4, 64, 4));
        assertSame(BlockType.AIR, detailChunk.getBlock(4, 72, 8),
                "Open-sky red mushroom candidates should be rejected by the Release 1.0 light rule");
        assertSame(BlockType.DEAD_BUSH, deadBushChunk.getBlock(12, 65, 5));
    }

    @Test
    @DisplayName("Biome grass metadata should keep Release 1.0 long-grass RNG coupling")
    void biomeGrassMetadataDoesNotConsumeTaigaSpecificRandomness() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        CountingHalfRandom random = new CountingHalfRandom();
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("tallGrassMetadataForBiome",
                BiomeType.class, Random.class);
        method.setAccessible(true);

        for (BiomeType biome : BiomeType.values()) {
            assertEquals(1, method.invoke(generator, biome, random),
                    "Release 1.0 BiomeDecorator uses the same long-grass metadata for " + biome);
        }
        assertEquals(0, random.nextIntCalls(),
                "Tall-grass metadata selection must not consume biome-specific decorator RNG");
    }

    @Test
    @DisplayName("Generated lily pads should require source-level water below")
    void generatedLilyPadsRequireSourceLevelWater() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(8, 63, 8, BlockType.WATER, 0);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.LILY_PAD));

        chunk.setBlock(8, 63, 8, BlockType.FLOWING_WATER, 3);
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.LILY_PAD),
                "Release 1.0 BlockWaterlily requires water material with metadata 0 below");
    }

    @Test
    @DisplayName("Generated overworld mushrooms should require mycelium or low-light opaque support")
    void generatedMushroomsRequireMyceliumOrLowLightSupport() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(8, 63, 8, BlockType.GRASS);
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.BROWN_MUSHROOM),
                "Open sky grass should be too bright for Release 1.0 mushroom generation");

        chunk.setBlock(8, 66, 8, BlockType.LEAVES);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.BROWN_MUSHROOM),
                "Covered opaque support should allow low-light mushroom generation");

        chunk.setBlock(9, 64, 8, BlockType.TORCH, Block.FACE_WEST);
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.RED_MUSHROOM),
                "BlockMushroom.canBlockStay rejects non-mycelium mushrooms at block light 13 or higher");

        chunk.setBlock(9, 64, 8, BlockType.AIR);
        chunk.setBlock(8, 66, 8, BlockType.AIR);
        chunk.setBlock(7, 63, 8, BlockType.GRASS);
        chunk.setBlock(7, 66, 8, BlockType.TALL_GRASS);
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 7, 64, 8, BlockType.RED_MUSHROOM),
                "Soft plants above a mushroom should not count as source sky cover");

        chunk.setBlock(9, 63, 8, BlockType.MYCELIUM);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 9, 64, 8, BlockType.RED_MUSHROOM),
                "Mycelium should allow mushrooms even without overhead cover");
    }

    @Test
    @DisplayName("Generated BlockFlower-style plants should require sky or block light")
    void generatedFlowersAndGrassRequireSourceLightGate() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(8, 63, 8, BlockType.DIRT);
        chunk.setBlock(8, 66, 8, BlockType.STONE);
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.YELLOW_FLOWER),
                "BlockFlower.canBlockStay rejects dark covered flower candidates");
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.TALL_GRASS),
                "WorldGenGrass should inherit the same light gate");

        chunk.setBlock(9, 64, 8, BlockType.TORCH);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.YELLOW_FLOWER),
                "Block light 8 or higher should allow covered flower candidates");

        chunk.setBlock(5, 63, 8, BlockType.DIRT);
        chunk.setBlock(5, 66, 8, BlockType.TALL_GRASS);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 5, 64, 8, BlockType.YELLOW_FLOWER),
                "Source height-map sky checks ignore soft plant blocks above flower candidates");

        chunk.setBlock(10, 63, 8, BlockType.FARMLAND);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 10, 64, 8, BlockType.RED_ROSE),
                "Release-era BlockFlower support includes farmland");

        chunk.setBlock(2, 63, 8, BlockType.SAND);
        chunk.setBlock(2, 66, 8, BlockType.STONE);
        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 2, 64, 8, BlockType.DEAD_BUSH),
                "WorldGenDeadBush also calls the BlockFlower stay predicate");
    }

    @Test
    @DisplayName("Generated cactus should use source material adjacency")
    void generatedCactusUsesSourceMaterialAdjacency() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(8, 63, 8, BlockType.SAND);
        chunk.setBlock(9, 64, 8, BlockType.REDSTONE_WIRE);

        assertFalse(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.CACTUS),
                "BlockCactus rejects adjacent blocking material, even for non-colliding redstone wire");

        chunk.setBlock(9, 64, 8, BlockType.TORCH);
        assertTrue(invokeCanGeneratedPlantStay(generator, chunk, 8, 64, 8, BlockType.CACTUS),
                "Torch material is non-blocking in the Release 1.0 cactus stay check");
    }

    @Test
    @DisplayName("Biome decorators should preserve Release-era tree metadata and desert cactus density")
    void biomeDecoratorsUseReleaseTreeAndCactusVariants() {
        ReleaseOneWorldGenerator forestGenerator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk forestChunk = new Chunk(0, 0);
        Chunk desertChunk = new Chunk(-113, -31);
        forestGenerator.generateChunk(null, forestChunk, 0, 0);
        forestGenerator.generateChunk(null, desertChunk, -113, -31);

        assertSame(BlockType.OAK_LOG, forestChunk.getBlock(1, 64, 13));
        assertEquals(2, forestChunk.getBlockMetadata(1, 64, 13),
                "Forest decorator should occasionally use birch log metadata");
        assertSame(BiomeType.DESERT, forestGenerator.getBiome(-113 * Chunk.WIDTH + 10, -31 * Chunk.DEPTH + 13));
        assertSame(BlockType.CACTUS, desertChunk.getBlock(10, 66, 13));
        assertSame(BlockType.CACTUS, desertChunk.getBlock(10, 67, 13));
        assertSame(BlockType.CACTUS, desertChunk.getBlock(10, 68, 13));

        ReleaseOneWorldGenerator taigaGenerator = new ReleaseOneWorldGenerator(86420L, Dimension.OVERWORLD);
        Chunk taigaChunk = new Chunk(-60, 28);
        taigaGenerator.generateChunk(null, taigaChunk, -60, 28);

        assertSame(BlockType.OAK_LOG, taigaChunk.getBlock(11, 65, 8));
        assertEquals(1, taigaChunk.getBlockMetadata(11, 65, 8),
                "Taiga decorator should use spruce log metadata");
        assertSame(BlockType.LEAVES, taigaChunk.getBlock(10, 67, 8));
        assertEquals(1, taigaChunk.getBlockMetadata(10, 67, 8),
                "Taiga decorator should use spruce leaf metadata");
        assertSame(BlockType.LEAVES, taigaChunk.getBlock(11, 73, 8));
        assertSame(BlockType.AIR, taigaChunk.getBlock(8, 67, 7),
                "Source conifer leaves should not form the broad normal-tree corner crown");
    }

    @Test
    @DisplayName("Reed and cactus scatters should preserve source RNG height draws")
    void reedAndCactusScatterConsumeSourceHeightDraws() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);

        Chunk cactusChunk = new Chunk(0, 0);
        CountingHalfRandom cactusRandom = new CountingHalfRandom();
        invokeScatter(generator, "placeCactusScatter", cactusChunk, cactusRandom);

        assertEquals(83, cactusRandom.nextIntCalls(),
                "WorldGenCactus draws height after an empty target, even when support later rejects placement");
        assertSame(BlockType.AIR, cactusChunk.getBlock(8, 64, 8));

        Chunk reedChunk = new Chunk(0, 0);
        reedChunk.setBlock(8, 63, 8, BlockType.STONE);
        reedChunk.setBlock(7, 63, 8, BlockType.WATER);
        CountingHalfRandom reedRandom = new CountingHalfRandom();
        invokeScatter(generator, "placeReedScatter", reedChunk, reedRandom);

        assertEquals(123, reedRandom.nextIntCalls(),
                "WorldGenReed draws height after the empty-and-water gate before per-block support checks");
        assertSame(BlockType.AIR, reedChunk.getBlock(8, 64, 8));
    }

    @Test
    @DisplayName("Biome mushroom loops should draw red mushroom start as x, z, then y")
    void biomeMushroomLoopDrawsRedStartAsSourceOrder() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        ScriptedRecordingRandom random = new ScriptedRecordingRandom(1, 0, 0, 0, 64);
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeMushroomPair",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class);
        method.setAccessible(true);

        method.invoke(generator, chunk, 0, 0, random, -8, -8);

        assertEquals(List.of(4, 8, 16, 16, 128), random.bounds().subList(0, 5),
                "Release 1.0 draws red mushroom x, z, then y inside the per-biome mushroom loop");
    }

    @Test
    @DisplayName("Biome reed loops should draw source start as x, z, then y")
    void biomeReedLoopDrawsStartAsSourceOrder() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(424242L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(0, 0);
        ScriptedRecordingRandom random = new ScriptedRecordingRandom(0, 0, 64);
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeBiomeReedScatter",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class);
        method.setAccessible(true);

        method.invoke(generator, chunk, 0, 0, random, -8, -8);

        assertEquals(List.of(16, 16, 128), random.bounds().subList(0, 3),
                "Release 1.0 draws biome-counted reed starts as x, z, then y");
    }

    @Test
    @DisplayName("Swamp tree decorators should hang Release-style vines from leaf edges")
    void swampDecoratorsHangVinesFromTreeLeaves() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-13, 40);

        generator.generateChunk(null, chunk, -13, 40);

        assertSame(BlockType.VINES, chunk.getBlock(12, 67, 11));
        assertEquals(8, chunk.getBlockMetadata(12, 67, 11),
                "Release swamp vines store the source vine side bitmask");
        assertSame(BlockType.LEAVES, chunk.getBlock(13, 67, 11));
    }

    @Test
    @DisplayName("Release 1.0 desert decorators should not place later-era desert wells")
    void desertDecoratorSkipsLaterEraWells() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(24680L, Dimension.OVERWORLD);
        Chunk chunk = new Chunk(-67, 69);

        generator.generateChunk(null, chunk, -67, 69);

        assertNotSame(BlockType.STONE_SLAB, chunk.getBlock(9, 70, 4),
                "Desert wells were added after Java Release 1.0 and should not decorate this fixture");
        assertNotSame(BlockType.STONE_SLAB, chunk.getBlock(13, 70, 4));
        assertNotSame(BlockType.FLOWING_WATER, chunk.getBlock(11, 69, 4));
        assertNotSame(BlockType.SANDSTONE, chunk.getBlock(11, 73, 4));
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

    @Test
    @DisplayName("Cave nodes should use the Release MathHelper sine table")
    void caveNodesUseReleaseSineTableForBoundaryCarving() {
        Chunk chunk = stoneChunk(0, 0);

        new ExposedCaveGenerator().carveSourceTrigFixture(chunk);

        assertSame(BlockType.AIR, chunk.getBlock(15, 40, 15),
                "The old Math.sin/cos path leaves this source sine-table boundary block solid");
    }

    @Test
    @DisplayName("Ravine generation should carve tall Release-style ellipsoid fissures")
    void ravinesCarveTallReleaseStyleFissures() {
        Chunk chunk = stoneChunk(-2, 0);

        new RavineGenerator().generate(chunk, 1L);

        int carved = 0;
        int minY = Chunk.HEIGHT;
        int maxY = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 1; y < 120; y++) {
                    if (chunk.getBlock(x, y, z) == BlockType.AIR || chunk.getBlock(x, y, z) == BlockType.LAVA) {
                        carved++;
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
        }

        assertTrue(carved > 300);
        assertTrue(maxY - minY >= 12);
        assertSame(BlockType.AIR, chunk.getBlock(4, 24, 10));
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

    private static void invokeScatter(ReleaseOneWorldGenerator generator, String methodName, Chunk chunk,
            Random random) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod(methodName, Chunk.class, int.class, int.class,
                Random.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, -8, -8);
    }

    private static void assertWorldGenSpawnEntry(ReleaseOneWorldGenerator generator, BiomeType biome,
            MobDefinition mob, int weight, int minGroup, int maxGroup) throws Exception {
        Object entry = worldGenCreatureEntry(generator, biome, mob);
        assertNotNull(entry, "Missing worldgen creature entry for " + mob + " in " + biome);
        assertEquals(weight, invokeRecordInt(entry, "weight"));
        assertEquals(minGroup, invokeRecordInt(entry, "minGroup"));
        assertEquals(maxGroup, invokeRecordInt(entry, "maxGroup"));
    }

    private static int worldGenCreatureWeight(ReleaseOneWorldGenerator generator, BiomeType biome, MobDefinition mob)
            throws Exception {
        Object entry = worldGenCreatureEntry(generator, biome, mob);
        return entry == null ? -1 : invokeRecordInt(entry, "weight");
    }

    private static Object worldGenCreatureEntry(ReleaseOneWorldGenerator generator, BiomeType biome,
            MobDefinition mob) throws Exception {
        Method entriesMethod = ReleaseOneWorldGenerator.class.getDeclaredMethod("worldGenCreatureEntries",
                BiomeType.class);
        entriesMethod.setAccessible(true);
        Object[] entries = (Object[]) entriesMethod.invoke(generator, biome);
        for (Object entry : entries) {
            Method mobMethod = entry.getClass().getDeclaredMethod("mob");
            mobMethod.setAccessible(true);
            if (mobMethod.invoke(entry) == mob) {
                return entry;
            }
        }
        return null;
    }

    private static int invokeRecordInt(Object record, String methodName) throws Exception {
        Method method = record.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (int) method.invoke(record);
    }

    private static boolean hasMyceliumUnderMobFoot(ReleaseOneWorldGenerator generator, Mob mob) {
        int x = (int) Math.floor(mob.getX());
        int y = (int) Math.floor(mob.getY());
        int z = (int) Math.floor(mob.getZ());
        return generator.baseBlockAt(x, y, z) == BlockType.MYCELIUM
                || generator.baseBlockAt(x, y - 1, z) == BlockType.MYCELIUM;
    }

    private static void invokeIceAndSnowFinishing(ReleaseOneWorldGenerator generator, Chunk chunk,
            int chunkX, int chunkZ) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeSnowLayers",
                Chunk.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, chunkX, chunkZ);
    }

    private static void invokeIceAndSnowFinishing(ReleaseOneWorldGenerator generator, Chunk chunk,
            int chunkX, int chunkZ, Object scratch) throws Exception {
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeSnowLayers",
                Chunk.class, int.class, int.class, scratchClass);
        method.setAccessible(true);
        method.invoke(generator, chunk, chunkX, chunkZ, scratch);
    }

    private static Object newSourceTreeScratch(ReleaseOneWorldGenerator generator, Chunk chunk,
            int chunkX, int chunkZ) throws Exception {
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Constructor<?> constructor = scratchClass.getDeclaredConstructor(
                ReleaseOneWorldGenerator.class, Chunk.class, int.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(generator, chunk, chunkX, chunkZ);
    }

    private static void setScratchBlock(Object scratch, int x, int y, int z, BlockType type, int metadata)
            throws Exception {
        Method method = scratch.getClass().getDeclaredMethod("setBlock",
                int.class, int.class, int.class, BlockType.class, int.class);
        method.setAccessible(true);
        method.invoke(scratch, x, y, z, type, metadata);
    }

    private static void invokeUnderwaterDisk(ReleaseOneWorldGenerator generator, Chunk chunk,
            Random random, BlockType replacement, int radiusBound, int verticalRadius) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("placeUnderwaterDisk",
                Chunk.class, int.class, int.class, Random.class, int.class, int.class,
                BlockType.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, 0, 0, random, 0, 0, replacement, radiusBound, verticalRadius);
    }

    private static boolean invokeCanGeneratedPlantStay(ReleaseOneWorldGenerator generator, Chunk chunk,
            int x, int y, int z, BlockType type) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("canGeneratedPlantStay",
                Chunk.class, int.class, int.class, int.class, int.class, int.class, BlockType.class);
        method.setAccessible(true);
        return (boolean) method.invoke(generator, chunk, 0, 0, x, y, z, type);
    }

    private static boolean invokeCanGeneratedPlantStay(ReleaseOneWorldGenerator generator, Chunk chunk,
            int x, int y, int z, BlockType type, Object scratch) throws Exception {
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("canGeneratedPlantStay",
                Chunk.class, int.class, int.class, int.class, int.class, int.class, BlockType.class, scratchClass);
        method.setAccessible(true);
        return (boolean) method.invoke(generator, chunk, 0, 0, x, y, z, type, scratch);
    }

    private static boolean invokeCanSpawnWorldGenCreature(ReleaseOneWorldGenerator generator, Chunk chunk,
            MobDefinition mob, int x, int y, int z) throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("canSpawnWorldGenCreature",
                Chunk.class, int.class, int.class, MobDefinition.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(generator, chunk, 0, 0, mob, x, y, z);
    }

    private static boolean invokeCanSpawnWorldGenCreature(ReleaseOneWorldGenerator generator, Chunk chunk,
            MobDefinition mob, int x, int y, int z, Object scratch) throws Exception {
        Class<?> scratchClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceTreeScratch");
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("canSpawnWorldGenCreature",
                Chunk.class, int.class, int.class, MobDefinition.class, int.class, int.class, int.class,
                scratchClass);
        method.setAccessible(true);
        return (boolean) method.invoke(generator, chunk, 0, 0, mob, x, y, z, scratch);
    }

    private static boolean scratchContains(Method getBlock, Object scratch, BlockType target,
            int minX, int maxY, int minZ, int maxX, int maxZ) throws Exception {
        for (int y = 0; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (getBlock.invoke(scratch, x, y, z) == target) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean scratchHasTargetChunkOverlay(Class<?> scratchClass, Object scratch,
            int chunkX, int chunkZ) throws Exception {
        FieldAccessors fields = sourceBlockPositionAccessors();
        var blocksField = scratchClass.getDeclaredField("blocks");
        blocksField.setAccessible(true);
        Map<?, ?> blocks = (Map<?, ?>) blocksField.get(scratch);
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        for (Object position : blocks.keySet()) {
            int x = (int) fields.x().invoke(position);
            int z = (int) fields.z().invoke(position);
            if (x >= minX && x < minX + Chunk.WIDTH && z >= minZ && z < minZ + Chunk.DEPTH) {
                return true;
            }
        }
        return false;
    }

    private static StructureSample firstGeneratedStructureBlock(ReleaseOneWorldGenerator generator, long seed,
            int chunkX, int chunkZ) throws Exception {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        Method populationRandom = ReleaseOneWorldGenerator.class.getDeclaredMethod("populationRandom",
                int.class, int.class);
        populationRandom.setAccessible(true);
        Random random = (Random) populationRandom.invoke(generator, chunkX, chunkZ);
        new StructureGenerator().generate(null, chunk, seed, chunkX, chunkZ, Dimension.OVERWORLD,
                generator, random);
        int baseX = chunkX * Chunk.WIDTH;
        int baseZ = chunkZ * Chunk.DEPTH;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    if (type != BlockType.AIR) {
                        return new StructureSample(baseX + x, y, baseZ + z,
                                type, chunk.getBlockMetadata(x, y, z));
                    }
                }
            }
        }
        return null;
    }

    private static FieldAccessors sourceBlockPositionAccessors() throws Exception {
        Class<?> positionClass = Class.forName("com.craftzero.world.ReleaseOneWorldGenerator$SourceBlockPos");
        Method x = positionClass.getDeclaredMethod("x");
        Method z = positionClass.getDeclaredMethod("z");
        x.setAccessible(true);
        z.setAccessible(true);
        return new FieldAccessors(x, z);
    }

    private record FieldAccessors(Method x, Method z) {
    }

    private record StructureSample(int x, int y, int z, BlockType type, int metadata) {
    }

    private static final class CountingHalfRandom extends Random {
        private int nextIntCalls;
        private int nextDoubleCalls;

        @Override
        public int nextInt(int bound) {
            nextIntCalls++;
            return bound / 2;
        }

        @Override
        public double nextDouble() {
            nextDoubleCalls++;
            return 0.5;
        }

        int nextIntCalls() {
            return nextIntCalls;
        }

        int nextDoubleCalls() {
            return nextDoubleCalls;
        }
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

    private static final class ExposedCaveGenerator extends CaveGenerator {
        private void carveSourceTrigFixture(Chunk chunk) {
            generateCaveNode(1L, chunk, 0, 0, 8.0D, 40.0D, 8.0D, 1.7F, 0.7F, -0.04F, 0, 24, 1.0D);
        }
    }

    private static int[] findBiomeColumn(ReleaseOneWorldGenerator generator, BiomeType target) {
        for (int radius = 0; radius <= 2048; radius += 16) {
            int[] found = findBiomeColumnOnRing(generator, target, radius);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] findHighBiomeColumn(ReleaseOneWorldGenerator generator, BiomeType target, int minTop) {
        for (int radius = 0; radius <= 2048; radius += 16) {
            int[] found = findHighBiomeColumnOnRing(generator, target, minTop, radius);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] findBiomeColumnOnRing(ReleaseOneWorldGenerator generator, BiomeType target, int radius) {
        if (radius == 0 && generator.getBiome(0, 0) == target) {
            return new int[] { 0, 0 };
        }
        for (int x = -radius; x <= radius; x += 16) {
            if (generator.getBiome(x, -radius) == target) {
                return new int[] { x, -radius };
            }
            if (generator.getBiome(x, radius) == target) {
                return new int[] { x, radius };
            }
        }
        for (int z = -radius + 16; z <= radius - 16; z += 16) {
            if (generator.getBiome(-radius, z) == target) {
                return new int[] { -radius, z };
            }
            if (generator.getBiome(radius, z) == target) {
                return new int[] { radius, z };
            }
        }
        return null;
    }

    private static int[] findHighBiomeColumnOnRing(ReleaseOneWorldGenerator generator, BiomeType target,
            int minTop, int radius) {
        if (radius == 0) {
            return highBiomeColumnAt(generator, target, minTop, 0, 0);
        }
        for (int x = -radius; x <= radius; x += 16) {
            int[] found = highBiomeColumnAt(generator, target, minTop, x, -radius);
            if (found != null) {
                return found;
            }
            found = highBiomeColumnAt(generator, target, minTop, x, radius);
            if (found != null) {
                return found;
            }
        }
        for (int z = -radius + 16; z <= radius - 16; z += 16) {
            int[] found = highBiomeColumnAt(generator, target, minTop, -radius, z);
            if (found != null) {
                return found;
            }
            found = highBiomeColumnAt(generator, target, minTop, radius, z);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] highBiomeColumnAt(ReleaseOneWorldGenerator generator, BiomeType target,
            int minTop, int x, int z) {
        return generator.getBiome(x, z) == target && generator.terrainTopY(x, z) >= minTop
                ? new int[] { x, z }
                : null;
    }

    private static int[] findWaterColumn(ReleaseOneWorldGenerator generator, boolean frozen) {
        for (int radius = 0; radius <= 2048; radius += 16) {
            int[] found = findWaterColumnOnRing(generator, frozen, radius);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] findFrozenBorderWaterColumn(ReleaseOneWorldGenerator generator) {
        for (int radius = 0; radius <= 128; radius++) {
            if (radius == 0) {
                int[] found = frozenBorderWaterColumnInChunk(generator, 0, 0);
                if (found != null) {
                    return found;
                }
                continue;
            }
            for (int chunkX = -radius; chunkX <= radius; chunkX++) {
                int[] north = frozenBorderWaterColumnInChunk(generator, chunkX, -radius);
                if (north != null) {
                    return north;
                }
                int[] south = frozenBorderWaterColumnInChunk(generator, chunkX, radius);
                if (south != null) {
                    return south;
                }
            }
            for (int chunkZ = -radius + 1; chunkZ <= radius - 1; chunkZ++) {
                int[] west = frozenBorderWaterColumnInChunk(generator, -radius, chunkZ);
                if (west != null) {
                    return west;
                }
                int[] east = frozenBorderWaterColumnInChunk(generator, radius, chunkZ);
                if (east != null) {
                    return east;
                }
            }
        }
        return null;
    }

    private static int[] frozenBorderWaterColumnInChunk(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ) {
        for (int localZ = 0; localZ < Chunk.DEPTH; localZ++) {
            int westX = chunkX * Chunk.WIDTH;
            int z = chunkZ * Chunk.DEPTH + localZ;
            if (waterColumnAt(generator, true, westX, z) != null) {
                return new int[] { westX, z, westX - 1, z };
            }
            int eastX = chunkX * Chunk.WIDTH + Chunk.WIDTH - 1;
            if (waterColumnAt(generator, true, eastX, z) != null) {
                return new int[] { eastX, z, eastX + 1, z };
            }
        }
        for (int localX = 0; localX < Chunk.WIDTH; localX++) {
            int x = chunkX * Chunk.WIDTH + localX;
            int northZ = chunkZ * Chunk.DEPTH;
            if (waterColumnAt(generator, true, x, northZ) != null) {
                return new int[] { x, northZ, x, northZ - 1 };
            }
            int southZ = chunkZ * Chunk.DEPTH + Chunk.DEPTH - 1;
            if (waterColumnAt(generator, true, x, southZ) != null) {
                return new int[] { x, southZ, x, southZ + 1 };
            }
        }
        return null;
    }

    private static int[] findInteriorFrozenColumn(ReleaseOneWorldGenerator generator) {
        for (int radius = 0; radius <= 128; radius++) {
            if (radius == 0) {
                int[] found = interiorFrozenColumnInChunk(generator, 0, 0);
                if (found != null) {
                    return found;
                }
                continue;
            }
            for (int chunkX = -radius; chunkX <= radius; chunkX++) {
                int[] north = interiorFrozenColumnInChunk(generator, chunkX, -radius);
                if (north != null) {
                    return north;
                }
                int[] south = interiorFrozenColumnInChunk(generator, chunkX, radius);
                if (south != null) {
                    return south;
                }
            }
            for (int chunkZ = -radius + 1; chunkZ <= radius - 1; chunkZ++) {
                int[] west = interiorFrozenColumnInChunk(generator, -radius, chunkZ);
                if (west != null) {
                    return west;
                }
                int[] east = interiorFrozenColumnInChunk(generator, radius, chunkZ);
                if (east != null) {
                    return east;
                }
            }
        }
        return null;
    }

    private static int[] interiorFrozenColumnInChunk(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ) {
        for (int localX = 1; localX < Chunk.WIDTH - 1; localX++) {
            for (int localZ = 1; localZ < Chunk.DEPTH - 1; localZ++) {
                int blockX = chunkX * Chunk.WIDTH + localX;
                int blockZ = chunkZ * Chunk.DEPTH + localZ;
                if (generator.getBiome(blockX, blockZ).isFrozen()) {
                    return new int[] { blockX, blockZ };
                }
            }
        }
        return null;
    }

    private static int[] findFrozenSurfaceChunk(ReleaseOneWorldGenerator generator) {
        for (int radius = 0; radius <= 2048; radius += 16) {
            int[] found = findFrozenSurfaceChunkOnRing(generator, radius);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] findFrozenSurfaceChunkOnRing(ReleaseOneWorldGenerator generator, int radius) {
        if (radius == 0) {
            return frozenSurfaceChunkAt(generator, 0, 0);
        }
        for (int x = -radius; x <= radius; x += 16) {
            int[] found = frozenSurfaceChunkAt(generator, x, -radius);
            if (found != null) {
                return found;
            }
            found = frozenSurfaceChunkAt(generator, x, radius);
            if (found != null) {
                return found;
            }
        }
        for (int z = -radius + 16; z <= radius - 16; z += 16) {
            int[] found = frozenSurfaceChunkAt(generator, -radius, z);
            if (found != null) {
                return found;
            }
            found = frozenSurfaceChunkAt(generator, radius, z);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] frozenSurfaceChunkAt(ReleaseOneWorldGenerator generator, int x, int z) {
        BiomeType biome = generator.getBiome(x, z);
        if (!biome.isFrozen()) {
            return null;
        }
        int top = generator.terrainTopY(x, z);
        if (top <= 0 || top >= Chunk.HEIGHT - 1
                || generator.baseBlockAt(x, top + 1, z) != BlockType.AIR) {
            return null;
        }
        BlockType support = generator.baseBlockAt(x, top, z);
        return support == BlockType.LEAVES || BlockShape.isOpaqueCube(support)
                ? new int[] { Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH) }
                : null;
    }

    private static int[] findWaterColumnOnRing(ReleaseOneWorldGenerator generator, boolean frozen, int radius) {
        if (radius == 0) {
            return waterColumnAt(generator, frozen, 0, 0);
        }
        for (int x = -radius; x <= radius; x += 16) {
            int[] found = waterColumnAt(generator, frozen, x, -radius);
            if (found != null) {
                return found;
            }
            found = waterColumnAt(generator, frozen, x, radius);
            if (found != null) {
                return found;
            }
        }
        for (int z = -radius + 16; z <= radius - 16; z += 16) {
            int[] found = waterColumnAt(generator, frozen, -radius, z);
            if (found != null) {
                return found;
            }
            found = waterColumnAt(generator, frozen, radius, z);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] waterColumnAt(ReleaseOneWorldGenerator generator, boolean frozen, int x, int z) {
        BiomeType biome = generator.getBiome(x, z);
        if (biome.canFreezeWater() != frozen || generator.terrainTopY(x, z) >= ReleaseOneWorldGenerator.SEA_LEVEL - 2) {
            return null;
        }
        BlockType seaBlock = generator.baseBlockAt(x, ReleaseOneWorldGenerator.SEA_LEVEL, z);
        if (frozen ? seaBlock == BlockType.ICE : seaBlock == BlockType.WATER) {
            return new int[] { x, z };
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
