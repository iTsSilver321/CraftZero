package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpecialBlockInteractionTest {
    private static final float EPSILON = 0.0001f;
    private static final BlockShape.BlockContext EMPTY_CONTEXT = new BlockShape.BlockContext() {
        @Override
        public BlockType getBlock(int dx, int dy, int dz) {
            return BlockType.AIR;
        }

        @Override
        public int getMetadata(int dx, int dy, int dz) {
            return 0;
        }
    };

    @Test
    @DisplayName("Cake slices should advance metadata and remove the final slice")
    void cakeSlicesAdvanceAndRemoveBlock() {
        World world = new World(70L);
        try {
            world.setBlock(0, 100, 0, BlockType.CAKE, 0);

            for (int expectedBites = 1; expectedBites <= World.CAKE_LAST_BITE_METADATA; expectedBites++) {
                assertTrue(world.eatCakeSlice(0, 100, 0));
                assertSame(BlockType.CAKE, world.getBlock(0, 100, 0));
                assertEquals(expectedBites, world.getCakeBites(0, 100, 0));
            }

            assertTrue(world.eatCakeSlice(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertFalse(world.eatCakeSlice(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cake should need support and should not drop when broken")
    void cakeRequiresSupportAndDoesNotDrop() {
        World world = new World(71L);
        try {
            assertFalse(world.canPlaceBlockAt(0, 100, 0, BlockType.CAKE, 0, null));

            world.setBlock(0, 99, 0, BlockType.STONE);
            assertTrue(world.canPlaceBlockAt(0, 100, 0, BlockType.CAKE, 0, null));
            world.setBlock(0, 100, 0, BlockType.CAKE, 0);

            assertTrue(world.breakBlock(0, 100, 0, true));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getDroppedItems().stream().noneMatch(item -> item.getItemType() == ItemType.CAKE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cake should break when its supporting block is removed")
    void cakeBreaksWhenSupportIsRemoved() {
        World world = new World(72L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.CAKE, 0);

            assertTrue(world.breakBlock(0, 99, 0, false));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cobweb block breaks should drop string only with sword or shears")
    void cobwebWorldDropsRequireSwordOrShears() {
        World world = new World(73L);
        try {
            world.setBlock(0, 100, 0, BlockType.COBWEB, 0);
            assertTrue(world.breakBlock(0, 100, 0, true));
            assertEquals(0, droppedCount(world, ItemType.STRING));
            assertEquals(0, droppedCount(world, ItemType.COBWEB));

            world.setBlock(1, 100, 0, BlockType.COBWEB, 0);
            assertTrue(world.breakBlock(1, 100, 0, true, ItemType.WOODEN_SWORD));
            assertEquals(1, droppedCount(world, ItemType.STRING));
            assertEquals(0, droppedCount(world, ItemType.COBWEB));

            world.setBlock(2, 100, 0, BlockType.COBWEB, 0);
            assertTrue(world.breakBlock(2, 100, 0, true, ItemType.SHEARS));
            assertEquals(2, droppedCount(world, ItemType.STRING));
            assertEquals(0, droppedCount(world, ItemType.COBWEB));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Foliage block breaks should respect Release 1.0 shears drops")
    void foliageWorldDropsRespectShears() {
        World world = new World(74L);
        try {
            world.setBlock(0, 100, 0, BlockType.LEAVES, 0);
            assertTrue(world.breakBlock(0, 100, 0, true, ItemType.SHEARS));
            assertEquals(1, droppedCount(world, ItemType.LEAVES));

            world.setBlock(1, 100, 0, BlockType.VINES, 0);
            assertTrue(world.breakBlock(1, 100, 0, true));
            assertEquals(0, droppedCount(world, ItemType.VINES));

            world.setBlock(2, 100, 0, BlockType.VINES, 0);
            assertTrue(world.breakBlock(2, 100, 0, true, ItemType.SHEARS));
            assertEquals(1, droppedCount(world, ItemType.VINES));

            world.setBlock(3, 100, 0, BlockType.DEAD_BUSH, 0);
            assertTrue(world.breakBlock(3, 100, 0, true));
            assertEquals(0, droppedCount(world, ItemType.DEAD_BUSH));

            world.setBlock(4, 100, 0, BlockType.DEAD_BUSH, 0);
            assertTrue(world.breakBlock(4, 100, 0, true, ItemType.SHEARS));
            assertEquals(0, droppedCount(world, ItemType.DEAD_BUSH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Marked leaves should decay only when disconnected from nearby logs")
    void naturalLeavesDecayWhenDisconnectedFromLogs() {
        World world = new World(75L);
        try {
            markGeneratedChunkShells(world, -1, 1, -1, 0);
            world.setBlock(0, 100, 0, BlockType.OAK_LOG, 0);
            for (int x = 1; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.LEAVES, 0);
            }
            world.setBlock(10, 100, 0, BlockType.LEAVES, 0);
            world.setBlock(12, 100, 0, BlockType.LEAVES, World.LEAF_PERSISTENT_BIT);

            world.advanceBlockTicks(25);

            assertSame(BlockType.LEAVES, world.getBlock(4, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(5, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(10, 100, 0));
            assertSame(BlockType.LEAVES, world.getBlock(12, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Unmarked generated leaves should ignore scheduled decay checks")
    void unmarkedGeneratedLeavesIgnoreScheduledDecayChecks() {
        World world = new World(751L, WorldGenerator.LEGACY_CRAFTZERO);
        try {
            Chunk chunk = world.getChunk(0, 0);
            chunk.setState(Chunk.ChunkState.GENERATED);
            chunk.setBlock(8, 64, 8, BlockType.LEAVES, 0);

            world.scheduleBlockTick(8, 64, 8, BlockType.LEAVES, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.LEAVES, world.getBlock(8, 64, 8));
            assertEquals(0, world.getBlockMetadata(8, 64, 8) & World.LEAF_CHECK_DECAY_BIT);
            assertFalse(world.hasScheduledBlockTick(8, 64, 8, BlockType.LEAVES));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Connected marked leaves should clear the check-decay bit")
    void connectedMarkedLeavesClearCheckDecayBit() {
        World world = new World(752L);
        try {
            markGeneratedChunkShells(world, -1, 1, -1, 1);
            world.setBlock(0, 100, 0, BlockType.OAK_LOG, 0);
            world.setBlock(1, 100, 0, BlockType.LEAVES, World.LEAF_CHECK_DECAY_BIT);

            world.advanceBlockTicks(25);

            assertSame(BlockType.LEAVES, world.getBlock(1, 100, 0));
            assertEquals(0, world.getBlockMetadata(1, 100, 0) & World.LEAF_CHECK_DECAY_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking logs should mark nearby generated leaves for decay")
    void breakingLogsMarksNearbyGeneratedLeavesForDecay() {
        World world = new World(753L);
        try {
            markGeneratedChunkShells(world, -1, 1, -1, 1);
            world.setBlock(0, 100, 0, BlockType.OAK_LOG, 0);
            world.getChunk(0, 0).setBlock(4, 100, 0, BlockType.LEAVES, 0);

            assertTrue(world.breakBlock(0, 100, 0, false));
            assertNotEquals(0, world.getBlockMetadata(4, 100, 0) & World.LEAF_CHECK_DECAY_BIT);

            world.advanceBlockTicks(25);

            assertSame(BlockType.AIR, world.getBlock(4, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Leaf decay should wait when the decay radius reaches unloaded chunks")
    void leafDecayWaitsForLoadedRadiusAtChunkEdges() {
        World world = new World(76L, WorldGenerator.LEGACY_CRAFTZERO);
        try {
            Chunk chunk = world.getChunk(0, 0);
            chunk.setState(Chunk.ChunkState.GENERATED);
            chunk.setBlock(15, 64, 0, BlockType.LEAVES, World.LEAF_CHECK_DECAY_BIT);

            world.scheduleBlockTick(15, 64, 0, BlockType.LEAVES, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.LEAVES, chunk.getBlock(15, 64, 0));
            assertTrue(world.hasScheduledBlockTick(15, 64, 0, BlockType.LEAVES));
            assertNull(world.getLoadedChunk(1, 0));
            assertNull(world.getLoadedChunk(0, -1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-style utility block drops should require pickaxe harvesting")
    void playerStyleUtilityBlockDropsRequirePickaxeHarvest() {
        World world = new World(77L);
        try {
            assertPlayerStyleDropRequiresPickaxe(world, 0, BlockType.CAULDRON, ItemType.CAULDRON);
            assertPlayerStyleDropRequiresPickaxe(world, 2, BlockType.BREWING_STAND, ItemType.BREWING_STAND);
            assertPlayerStyleDropRequiresPickaxe(world, 4, BlockType.ENCHANTING_TABLE, ItemType.ENCHANTING_TABLE);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-style netherrack drops should require pickaxe harvesting")
    void playerStyleNetherrackDropsRequirePickaxeHarvest() {
        World world = new World(79L);
        try {
            assertPlayerStyleDropRequiresPickaxe(world, 0, BlockType.NETHERRACK, ItemType.NETHERRACK);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-style stone control drops should require pickaxe harvesting")
    void playerStyleStoneControlDropsRequirePickaxeHarvest() {
        World world = new World(78L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.STONE_PRESSURE_PLATE, 0);
            assertTrue(world.breakBlock(0, 100, 0,
                    BlockHarvestRules.canHarvest(BlockType.STONE_PRESSURE_PLATE, null), null));
            assertEquals(0, droppedCount(world, ItemType.STONE_PRESSURE_PLATE));

            world.setBlock(1, 99, 0, BlockType.STONE, 0);
            world.setBlock(1, 100, 0, BlockType.STONE_PRESSURE_PLATE, 0);
            assertTrue(world.breakBlock(1, 100, 0,
                    BlockHarvestRules.canHarvest(BlockType.STONE_PRESSURE_PLATE, ItemType.WOODEN_PICKAXE),
                    ItemType.WOODEN_PICKAXE));
            assertEquals(1, droppedCount(world, ItemType.STONE_PRESSURE_PLATE));

            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            world.setBlock(2, 100, -1, BlockType.STONE_BUTTON, Block.FACE_NORTH);
            assertTrue(world.breakBlock(2, 100, -1,
                    BlockHarvestRules.canHarvest(BlockType.STONE_BUTTON, null), null));
            assertEquals(0, droppedCount(world, ItemType.STONE_BUTTON));

            world.setBlock(3, 100, 0, BlockType.STONE, 0);
            world.setBlock(3, 100, -1, BlockType.STONE_BUTTON, Block.FACE_NORTH);
            assertTrue(world.breakBlock(3, 100, -1,
                    BlockHarvestRules.canHarvest(BlockType.STONE_BUTTON, ItemType.WOODEN_PICKAXE),
                    ItemType.WOODEN_PICKAXE));
            assertEquals(1, droppedCount(world, ItemType.STONE_BUTTON));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cake shapes should split source collision height from selection height")
    void cakeShapeShrinksWithBites() {
        List<BlockShape.Cuboid> fullCake = BlockShape.getRenderBoxes(BlockType.CAKE, 0, EMPTY_CONTEXT);
        assertEquals(1, fullCake.size());
        assertCakeBox(fullCake.get(0), 1.0f / 16.0f, 15.0f / 16.0f);

        List<BlockShape.Cuboid> selectionCake = BlockShape.getSelectionBoxes(BlockType.CAKE, 0, EMPTY_CONTEXT);
        assertEquals(1, selectionCake.size());
        assertCakeBox(selectionCake.get(0), 1.0f / 16.0f, 15.0f / 16.0f);

        List<BlockShape.Cuboid> collisionCake = BlockShape.getCollisionBoxes(BlockType.CAKE, 0, EMPTY_CONTEXT);
        assertEquals(1, collisionCake.size());
        assertCakeCollisionBox(collisionCake.get(0), 1.0f / 16.0f, 15.0f / 16.0f);

        List<BlockShape.Cuboid> lastSlice = BlockShape.getRenderBoxes(BlockType.CAKE, World.CAKE_LAST_BITE_METADATA,
                EMPTY_CONTEXT);
        assertEquals(1, lastSlice.size());
        assertCakeBox(lastSlice.get(0), 11.0f / 16.0f, 15.0f / 16.0f);

        List<BlockShape.Cuboid> lastSliceCollision = BlockShape.getCollisionBoxes(BlockType.CAKE,
                World.CAKE_LAST_BITE_METADATA, EMPTY_CONTEXT);
        assertEquals(1, lastSliceCollision.size());
        assertCakeCollisionBox(lastSliceCollision.get(0), 11.0f / 16.0f, 15.0f / 16.0f);
        assertFalse(BlockShape.isFullCube(BlockType.CAKE, 0));
    }

    private static void assertCakeBox(BlockShape.Cuboid box, float minX, float maxX) {
        assertEquals(minX, box.minX(), EPSILON);
        assertEquals(0.0f, box.minY(), EPSILON);
        assertEquals(1.0f / 16.0f, box.minZ(), EPSILON);
        assertEquals(maxX, box.maxX(), EPSILON);
        assertEquals(8.0f / 16.0f, box.maxY(), EPSILON);
        assertEquals(15.0f / 16.0f, box.maxZ(), EPSILON);
    }

    private static void assertCakeCollisionBox(BlockShape.Cuboid box, float minX, float maxX) {
        assertEquals(minX, box.minX(), EPSILON);
        assertEquals(0.0f, box.minY(), EPSILON);
        assertEquals(1.0f / 16.0f, box.minZ(), EPSILON);
        assertEquals(maxX, box.maxX(), EPSILON);
        assertEquals(7.0f / 16.0f, box.maxY(), EPSILON);
        assertEquals(15.0f / 16.0f, box.maxZ(), EPSILON);
    }

    private static void assertPlayerStyleDropRequiresPickaxe(World world, int x, BlockType blockType,
            ItemType expectedDrop) {
        world.setBlock(x, 100, 0, blockType, 0);
        assertTrue(world.breakBlock(x, 100, 0, BlockHarvestRules.canHarvest(blockType, null), null));
        assertEquals(0, droppedCount(world, expectedDrop), blockType.name());

        world.setBlock(x + 1, 100, 0, blockType, 0);
        assertTrue(world.breakBlock(x + 1, 100, 0,
                BlockHarvestRules.canHarvest(blockType, ItemType.WOODEN_PICKAXE), ItemType.WOODEN_PICKAXE));
        assertEquals(1, droppedCount(world, expectedDrop), blockType.name());
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static void markGeneratedChunkShells(World world, int minChunkX, int maxChunkX,
            int minChunkZ, int maxChunkZ) {
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                world.getChunk(chunkX, chunkZ).setState(Chunk.ChunkState.GENERATED);
            }
        }
    }
}
