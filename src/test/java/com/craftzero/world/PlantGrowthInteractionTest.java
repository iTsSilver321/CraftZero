package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PlantGrowthInteractionTest {
    @Test
    @DisplayName("Sugar cane requires water-side soil or another cane below")
    void sugarCaneUsesReleaseOneSupportRules() {
        assertFalse(BlockShape.canPlaceAt(BlockType.SUGAR_CANE, 0, supportContext(BlockType.SAND, false)));
        assertTrue(BlockShape.canPlaceAt(BlockType.SUGAR_CANE, 0, supportContext(BlockType.SAND, true)));
        assertTrue(BlockShape.canPlaceAt(BlockType.SUGAR_CANE, 0, supportContext(BlockType.SUGAR_CANE, false)));
        assertTrue(BlockShape.getCollisionBoxes(BlockType.SUGAR_CANE, 0, supportContext(BlockType.SAND, true)).isEmpty());
        assertFalse(BlockShape.getSelectionBoxes(BlockType.SUGAR_CANE, 0, supportContext(BlockType.SAND, true)).isEmpty());
    }

    @Test
    @DisplayName("Sugar cane grows upward from age 15 and breaks when water support is lost")
    void sugarCaneGrowsAndBreaksWithoutWater() {
        World world = new World(5800L);
        try {
            world.setBlock(0, 98, 0, BlockType.STONE, 0);
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(1, 99, 0, BlockType.WATER, 0);
            world.setBlock(0, 100, 0, BlockType.SUGAR_CANE, World.COLUMN_PLANT_MAX_AGE);

            world.advanceBlockTicks(20);

            assertSame(BlockType.SUGAR_CANE, world.getBlock(0, 101, 0));

            world.setBlock(1, 99, 0, BlockType.AIR, 0);
            world.advanceBlockTicks(20);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.SUGAR_CANE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cactus uses narrow collision and grows only up to three blocks")
    void cactusShapeAndGrowthLimit() {
        BlockShape.Cuboid box = BlockShape.getCollisionBoxes(BlockType.CACTUS, 0, supportContext(BlockType.SAND, false))
                .get(0);
        assertEquals(1.0f / 16.0f, box.minX(), 0.0001f);
        assertEquals(15.0f / 16.0f, box.maxX(), 0.0001f);
        assertEquals(15.0f / 16.0f, box.maxY(), 0.0001f);

        World world = new World(5801L);
        try {
            world.setBlock(0, 98, 0, BlockType.STONE, 0);
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(0, 100, 0, BlockType.CACTUS, World.COLUMN_PLANT_MAX_AGE);

            world.advanceBlockTicks(20);

            assertSame(BlockType.CACTUS, world.getBlock(0, 101, 0));

            world.setBlock(0, 101, 0, BlockType.CACTUS, 0);
            world.setBlock(0, 102, 0, BlockType.CACTUS, World.COLUMN_PLANT_MAX_AGE);
            world.advanceBlockTicks(20);

            assertSame(BlockType.AIR, world.getBlock(0, 103, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether wart requires soul sand, grows in the Nether, and mature drops multiply")
    void netherWartSupportGrowthAndDrops() {
        assertTrue(BlockShape.canPlaceAt(BlockType.NETHER_WART, 0, supportContext(BlockType.SOUL_SAND, false)));
        assertFalse(BlockShape.canPlaceAt(BlockType.NETHER_WART, 0, supportContext(BlockType.SAND, false)));
        assertTrue(BlockShape.getCollisionBoxes(BlockType.NETHER_WART, 0, supportContext(BlockType.SOUL_SAND, false)).isEmpty());

        World world = new World(5802L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            world.setBlock(0, 99, 0, BlockType.SOUL_SAND, 0);
            world.setBlock(0, 100, 0, BlockType.NETHER_WART, 2);

            for (int i = 0; i < 200 && world.getBlockMetadata(0, 100, 0) < World.NETHER_WART_MAX_AGE; i++) {
                world.advanceBlockTicks(20);
            }

            assertEquals(World.NETHER_WART_MAX_AGE, world.getBlockMetadata(0, 100, 0));

            List<ItemStack> immature = BlockDropResolver.getDrops(BlockType.NETHER_WART, 1, fixedIntRandom(0));
            List<ItemStack> mature = BlockDropResolver.getDrops(BlockType.NETHER_WART, World.NETHER_WART_MAX_AGE,
                    fixedIntRandom(2));
            assertEquals(1, immature.get(0).getCount());
            assertSame(ItemType.NETHER_WART, mature.get(0).getType());
            assertEquals(4, mature.get(0).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether wart planted outside the Nether should stay dormant")
    void netherWartDoesNotGrowOutsideNether() {
        World world = new World(5806L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.setBlock(0, 99, 0, BlockType.SOUL_SAND, 0);
            world.setBlock(0, 100, 0, BlockType.NETHER_WART, 0);

            for (int i = 0; i < 200; i++) {
                world.advanceBlockTicks(20);
            }

            assertSame(BlockType.NETHER_WART, world.getBlock(0, 100, 0));
            assertEquals(0, world.getBlockMetadata(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether wart breaks when soul sand support is removed")
    void netherWartBreaksWithoutSoulSand() {
        World world = new World(5803L);
        try {
            world.setBlock(0, 99, 0, BlockType.SOUL_SAND, 0);
            world.setBlock(0, 100, 0, BlockType.NETHER_WART, 0);

            world.breakBlock(0, 99, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.NETHER_WART));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bone meal should grow small mushrooms into matching huge mushrooms")
    void boneMealGrowsHugeMushroom() {
        World world = new World(5804L);
        try {
            clearVolume(world, -4, 100, -4, 4, 110, 4);
            world.setBlock(0, 99, 0, BlockType.MYCELIUM, 0);
            world.setBlock(0, 100, 0, BlockType.RED_MUSHROOM, 0);

            assertTrue(world.applyBoneMealToPlant(0, 100, 0));

            assertSame(BlockType.DIRT, world.getBlock(0, 99, 0));
            assertSame(BlockType.RED_MUSHROOM_BLOCK, world.getBlock(0, 100, 0));
            assertEquals(10, world.getBlockMetadata(0, 100, 0));
            assertTrue(countBlocks(world, BlockType.RED_MUSHROOM_BLOCK, -4, 100, -4, 4, 110, 4) > 8);
            assertEquals(0, countBlocks(world, BlockType.BROWN_MUSHROOM_BLOCK, -4, 100, -4, 4, 110, 4));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Failed huge mushroom bone meal should restore the small mushroom")
    void failedHugeMushroomBoneMealRestoresSmallMushroom() {
        World world = new World(5805L);
        try {
            clearVolume(world, 4, 100, 4, 12, 110, 12);
            world.setBlock(8, 99, 8, BlockType.MYCELIUM, 0);
            world.setBlock(8, 100, 8, BlockType.BROWN_MUSHROOM, 0);
            world.setBlock(11, 101, 8, BlockType.STONE, 0);

            assertFalse(world.applyBoneMealToPlant(8, 100, 8));

            assertSame(BlockType.BROWN_MUSHROOM, world.getBlock(8, 100, 8));
            assertSame(BlockType.STONE, world.getBlock(11, 101, 8));
            assertEquals(0, countBlocks(world, BlockType.BROWN_MUSHROOM_BLOCK, 4, 100, 4, 12, 110, 12));
        } finally {
            world.cleanup();
        }
    }

    private static void clearVolume(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static int countBlocks(World world, BlockType type, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
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

    private static BlockShape.BlockContext supportContext(BlockType below, boolean waterBesideBelow) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                if (dy == -1 && dx == 0 && dz == 0) {
                    return below;
                }
                if (waterBesideBelow && dy == -1 && dx == 1 && dz == 0) {
                    return BlockType.WATER;
                }
                return BlockType.AIR;
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
