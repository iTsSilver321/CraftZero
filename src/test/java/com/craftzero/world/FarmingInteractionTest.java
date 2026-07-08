package com.craftzero.world;

import com.craftzero.entity.DroppedItem;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FarmingInteractionTest {
    @Test
    @DisplayName("Farmland uses Release 1.0 shape and drops dirt")
    void farmlandShapeAndDrop() {
        List<BlockShape.Cuboid> boxes = BlockShape.getCollisionBoxes(BlockType.FARMLAND, 0, contextWithBelow(BlockType.DIRT));

        assertEquals(1, boxes.size());
        assertEquals(15.0f / 16.0f, boxes.get(0).maxY(), 0.0001f);
        assertFalse(BlockShape.isFullCube(BlockType.FARMLAND, 0));
        assertSame(ItemType.DIRT, BlockType.FARMLAND.getDroppedItem());
    }

    @Test
    @DisplayName("Wheat crops require farmland and break when support is removed")
    void cropsRequireFarmlandSupport() {
        assertTrue(BlockShape.canPlaceAt(BlockType.CROPS, 0, contextWithBelow(BlockType.FARMLAND)));
        assertFalse(BlockShape.canPlaceAt(BlockType.CROPS, 0, contextWithBelow(BlockType.DIRT)));
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.CROPS, Block.FACE_TOP));

        World world = new World(5700L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 7);
            world.setBlock(0, 71, 0, BlockType.CROPS, 0);

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(hasDrop(world, ItemType.SEEDS));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Farmland hydrates near water and decays back to dirt when dry and unplanted")
    void farmlandHydratesAndDecays() {
        World world = new World(5701L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 0);
            world.setBlock(4, 70, 0, BlockType.WATER, 0);

            world.advanceBlockTicks(20);

            assertSame(BlockType.FARMLAND, world.getBlock(0, 70, 0));
            assertEquals(World.FARMLAND_MAX_MOISTURE, world.getBlockMetadata(0, 70, 0));

            world.setBlock(4, 70, 0, BlockType.AIR, 0);
            world.advanceBlockTicks(20 * 8);

            assertSame(BlockType.DIRT, world.getBlock(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rain hydrates exposed farmland without nearby water")
    void rainHydratesExposedFarmlandWithoutNearbyWater() {
        World world = new World(5714L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            int x = pos[0];
            int z = pos[1];
            int y = 100;
            prepareOpenColumn(world, x, z);
            world.setWeatherState("rain", 1000, 1000);
            world.setBlock(x, y, z, BlockType.FARMLAND, 0);

            assertTrue(world.isRainingAt(x, y + 1, z));

            world.scheduleBlockTick(x, y, z, BlockType.FARMLAND, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.FARMLAND, world.getBlock(x, y, z));
            assertEquals(World.FARMLAND_MAX_MOISTURE, world.getBlockMetadata(x, y, z));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Farmland should trample to dirt only after a meaningful fall")
    void farmlandTramplesFromFallImpact() {
        World world = new World(5710L);
        try {
            AABB landingBox = new AABB(0.2f, 70.9375f, 0.2f, 0.8f, 72.7f, 0.8f);
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 7);
            world.setBlock(0, 71, 0, BlockType.CROPS, 0);

            assertFalse(world.trampleFarmlandBelow(landingBox, 0.5f));
            assertSame(BlockType.FARMLAND, world.getBlock(0, 70, 0));
            assertSame(BlockType.CROPS, world.getBlock(0, 71, 0));

            assertTrue(world.trampleFarmlandBelow(landingBox, 2.0f));
            assertSame(BlockType.DIRT, world.getBlock(0, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(hasDrop(world, ItemType.SEEDS));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dry farmland remains farmland while crops are planted above it")
    void plantedDryFarmlandDoesNotDecay() {
        World world = new World(5702L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 0);
            world.setBlock(0, 71, 0, BlockType.CROPS, 0);

            world.advanceBlockTicks(20);

            assertSame(BlockType.FARMLAND, world.getBlock(0, 70, 0));
            assertEquals(0, world.getBlockMetadata(0, 70, 0));
            assertSame(BlockType.CROPS, world.getBlock(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pumpkin and melon stems should use farmland support and bone meal growth")
    void stemsUseFarmlandSupportAndBoneMeal() {
        assertTrue(BlockShape.canPlaceAt(BlockType.PUMPKIN_STEM, 0, contextWithBelow(BlockType.FARMLAND)));
        assertTrue(BlockShape.canPlaceAt(BlockType.MELON_STEM, 0, contextWithBelow(BlockType.FARMLAND)));
        assertFalse(BlockShape.canPlaceAt(BlockType.PUMPKIN_STEM, 0, contextWithBelow(BlockType.DIRT)));

        World world = new World(5708L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 7);
            world.setBlock(0, 71, 0, BlockType.PUMPKIN_STEM, 0);

            assertTrue(world.applyBoneMealToPlant(0, 71, 0));
            int age = world.getBlockMetadata(0, 71, 0);
            assertTrue(age >= 2 && age <= 5);
            while (world.getBlockMetadata(0, 71, 0) < World.MAX_CROP_AGE) {
                assertTrue(world.applyBoneMealToPlant(0, 71, 0));
            }
            assertFalse(world.applyBoneMealToPlant(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mature pumpkin and melon stems should grow adjacent fruit on valid support")
    void matureStemsGrowAdjacentFruit() throws Exception {
        World world = new World(5709L);
        try {
            prepareStemPatch(world);
            world.setBlock(0, 71, 0, BlockType.PUMPKIN_STEM, World.MAX_CROP_AGE);

            invokeStemFruitGrowth(world, 0, 71, 0, BlockType.PUMPKIN_STEM);

            assertTrue(hasAdjacentFruit(world, 0, 71, 0, BlockType.PUMPKIN));

            prepareStemPatch(world);
            world.setBlock(0, 71, 0, BlockType.MELON_STEM, World.MAX_CROP_AGE);

            invokeStemFruitGrowth(world, 0, 71, 0, BlockType.MELON_STEM);

            assertTrue(hasAdjacentFruit(world, 0, 71, 0, BlockType.MELON));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bone meal matures wheat crops and refuses already mature crops")
    void boneMealMaturesWheat() {
        World world = new World(5703L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 7);
            world.setBlock(0, 71, 0, BlockType.CROPS, 2);

            assertTrue(world.applyBoneMealToCrop(0, 71, 0));
            assertEquals(World.MAX_CROP_AGE, world.getCropAge(0, 71, 0));
            assertFalse(world.applyBoneMealToCrop(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bone meal should grow saplings into oak trees when space is available")
    void boneMealGrowsSaplingIntoTree() {
        World world = new World(5706L);
        try {
            world.setBlock(8, 70, 8, BlockType.GRASS, 0);
            world.setBlock(8, 71, 8, BlockType.SAPLING, 0);

            assertTrue(world.applyBoneMealToPlant(8, 71, 8));

            assertSame(BlockType.OAK_LOG, world.getBlock(8, 71, 8));
            assertTrue(countBlocks(world, BlockType.OAK_LOG, 8, 71, 8, 8, 80, 8) >= 4);
            assertTrue(countBlocks(world, BlockType.LEAVES, 6, 73, 6, 10, 80, 10) > 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bone meal on obstructed saplings should be consumed without growing a tree")
    void boneMealOnBlockedSaplingConsumesWithoutGrowth() {
        World world = new World(5707L);
        try {
            world.setBlock(8, 70, 8, BlockType.GRASS, 0);
            world.setBlock(8, 71, 8, BlockType.SAPLING, 0);
            world.setBlock(8, 72, 8, BlockType.STONE, 0);

            assertTrue(world.applyBoneMealToPlant(8, 71, 8));

            assertSame(BlockType.SAPLING, world.getBlock(8, 71, 8));
            assertSame(BlockType.STONE, world.getBlock(8, 72, 8));
            assertEquals(0, countBlocks(world, BlockType.OAK_LOG, 8, 71, 8, 8, 80, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bone meal on grass should scatter Release 1.0 tall grass and flowers")
    void boneMealOnGrassScattersGroundCover() {
        World world = new World(5711L);
        try {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dx = -8; dx <= 8; dx++) {
                    world.setBlock(dx, 70, dz, BlockType.GRASS, 0);
                    world.setBlock(dx, 71, dz, BlockType.AIR, 0);
                }
            }

            assertTrue(world.applyBoneMealToPlant(0, 70, 0));

            int tallGrass = countBlocks(world, BlockType.TALL_GRASS, -8, 71, -8, 8, 74, 8);
            int flowers = countBlocks(world, BlockType.YELLOW_FLOWER, -8, 71, -8, 8, 74, 8)
                    + countBlocks(world, BlockType.RED_ROSE, -8, 71, -8, 8, 74, 8);
            assertTrue(tallGrass > 0, "grass bone meal should generate tall grass");
            assertTrue(tallGrass + flowers > 0, "grass bone meal should generate ground cover");
            assertTallGrassMetadata(world, -8, 71, -8, 8, 74, 8, 1);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Grass and mycelium should decay to dirt under opaque low-light cover")
    void grassAndMyceliumDecayUnderOpaqueLowLightCover() {
        World world = new World(5712L);
        try {
            world.setBlock(0, 70, 0, BlockType.DIRT, 0);
            world.setBlock(0, 70, 0, BlockType.GRASS, 0);
            world.setBlock(0, 71, 0, BlockType.STONE, 0);
            world.setBlock(2, 70, 0, BlockType.DIRT, 0);
            world.setBlock(2, 70, 0, BlockType.MYCELIUM, 0);
            world.setBlock(2, 71, 0, BlockType.STONE, 0);

            world.scheduleBlockTick(0, 70, 0, BlockType.GRASS, 0);
            world.scheduleBlockTick(2, 70, 0, BlockType.MYCELIUM, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.DIRT, world.getBlock(0, 70, 0));
            assertSame(BlockType.DIRT, world.getBlock(2, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Grass and mycelium should spread to nearby dirt under bright open light")
    void grassAndMyceliumSpreadToNearbyDirtUnderBrightOpenLight() {
        World world = new World(5713L);
        try {
            prepareGrassLikeSpreadPatch(world, 0, 70, 0, BlockType.GRASS);
            prepareGrassLikeSpreadPatch(world, 20, 70, 0, BlockType.MYCELIUM);

            for (int i = 0; i < 160
                    && (countBlocks(world, BlockType.GRASS, -2, 70, -2, 2, 70, 2) <= 1
                            || countBlocks(world, BlockType.MYCELIUM, 18, 70, -2, 22, 70, 2) <= 1); i++) {
                world.scheduleBlockTick(0, 70, 0, BlockType.GRASS, 0);
                world.scheduleBlockTick(20, 70, 0, BlockType.MYCELIUM, 0);
                world.advanceBlockTicks(1);
            }

            assertTrue(countBlocks(world, BlockType.GRASS, -2, 70, -2, 2, 70, 2) > 1,
                    "grass should spread onto nearby dirt");
            assertTrue(countBlocks(world, BlockType.MYCELIUM, 18, 70, -2, 22, 70, 2) > 1,
                    "mycelium should spread onto nearby dirt");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hydrated wheat crops advance age on scheduled ticks")
    void hydratedWheatGrowsOnTicks() {
        World world = new World(5704L);
        try {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    world.setBlock(dx, 70, dz, BlockType.FARMLAND, World.FARMLAND_MAX_MOISTURE);
                }
            }
            world.setBlock(0, 71, 0, BlockType.CROPS, 0);

            for (int i = 0; i < 200 && world.getCropAge(0, 71, 0) == 0; i++) {
                world.advanceBlockTicks(20);
            }

            assertTrue(world.getCropAge(0, 71, 0) > 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mature wheat drops wheat plus seed rolls while immature wheat drops seeds")
    void wheatDropsUseCropAge() {
        List<ItemStack> immatureDrops = BlockDropResolver.getDrops(BlockType.CROPS, 3, fixedIntRandom(0));
        assertEquals(1, immatureDrops.size());
        assertSame(ItemType.SEEDS, immatureDrops.get(0).getType());

        List<ItemStack> matureDrops = BlockDropResolver.getDrops(BlockType.CROPS, World.MAX_CROP_AGE, fixedIntRandom(0));
        assertTrue(matureDrops.stream().anyMatch(stack -> stack.getType() == ItemType.WHEAT && stack.getCount() == 1));
        assertTrue(matureDrops.stream().anyMatch(stack -> stack.getType() == ItemType.SEEDS && stack.getCount() == 3));
    }

    @Test
    @DisplayName("Stem drops should return matching seed rolls")
    void stemDropsUseMatchingSeeds() {
        List<ItemStack> pumpkinDrops = BlockDropResolver.getDrops(BlockType.PUMPKIN_STEM, World.MAX_CROP_AGE,
                fixedIntRandom(0));
        List<ItemStack> melonDrops = BlockDropResolver.getDrops(BlockType.MELON_STEM, World.MAX_CROP_AGE,
                fixedIntRandom(0));

        assertEquals(1, pumpkinDrops.size());
        assertSame(ItemType.PUMPKIN_SEEDS, pumpkinDrops.get(0).getType());
        assertEquals(3, pumpkinDrops.get(0).getCount());
        assertEquals(1, melonDrops.size());
        assertSame(ItemType.MELON_SEEDS, melonDrops.get(0).getType());
        assertEquals(3, melonDrops.get(0).getCount());
    }

    @Test
    @DisplayName("Breaking mature crop blocks uses metadata-aware wheat drops")
    void breakingMatureCropDropsWheat() {
        World world = new World(5705L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 7);
            world.setBlock(0, 71, 0, BlockType.CROPS, World.MAX_CROP_AGE);

            assertTrue(world.breakBlock(0, 71, 0, true));

            assertTrue(hasDrop(world, ItemType.WHEAT));
        } finally {
            world.cleanup();
        }
    }

    private static boolean hasDrop(World world, ItemType type) {
        for (DroppedItem item : world.getDroppedItems()) {
            if (item.getItemType() == type) {
                return true;
            }
        }
        return false;
    }

    private static int[] findRainBiome(World world) {
        for (int x = -256; x <= 256; x += 8) {
            for (int z = -256; z <= 256; z += 8) {
                if (isRainBiome(world.getReleaseBiome(x, z))) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No non-frozen rain biome found near spawn search area");
    }

    private static boolean isRainBiome(BiomeType biome) {
        return biome.hasPrecipitation() && !biome.canFreezeWater() && biome.getTemperature() < 1.0f;
    }

    private static void prepareOpenColumn(World world, int x, int z) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, BlockType.STONE, 0);
        for (int y = 100; y < 128; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    private static void prepareStemPatch(World world) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                world.setBlock(dx, 70, dz, BlockType.FARMLAND, World.FARMLAND_MAX_MOISTURE);
                world.setBlock(dx, 71, dz, BlockType.AIR, 0);
            }
        }
    }

    private static void prepareGrassLikeSpreadPatch(World world, int centerX, int y, int centerZ, BlockType source) {
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                world.setBlock(x, y - 1, z, BlockType.STONE, 0);
                world.setBlock(x, y, z, BlockType.DIRT, 0);
                for (int clearY = y + 1; clearY <= y + 4; clearY++) {
                    world.setBlock(x, clearY, z, BlockType.AIR, 0);
                }
            }
        }
        world.setBlock(centerX, y, centerZ, source, 0);
    }

    private static boolean hasAdjacentFruit(World world, int x, int y, int z, BlockType fruit) {
        return world.getBlock(x + 1, y, z) == fruit
                || world.getBlock(x - 1, y, z) == fruit
                || world.getBlock(x, y, z + 1) == fruit
                || world.getBlock(x, y, z - 1) == fruit;
    }

    private static void invokeStemFruitGrowth(World world, int x, int y, int z, BlockType stem) throws Exception {
        Method method = World.class.getDeclaredMethod("tryGrowStemFruit",
                int.class, int.class, int.class, BlockType.class);
        method.setAccessible(true);
        method.invoke(world, x, y, z, stem);
    }

    private static int countBlocks(World world, BlockType type, int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (world.getBlock(x, y, z) == type) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void assertTallGrassMetadata(World world, int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ, int expectedMetadata) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (world.getBlock(x, y, z) == BlockType.TALL_GRASS) {
                        assertEquals(expectedMetadata, world.getBlockMetadata(x, y, z));
                    }
                }
            }
        }
    }

    private static BlockShape.BlockContext contextWithBelow(BlockType below) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dy == -1 ? below : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static Random fixedIntRandom(int value) {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return Math.min(value, bound - 1);
            }
        };
    }
}
