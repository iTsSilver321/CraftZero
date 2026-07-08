package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnowLayerInteractionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    @DisplayName("Snow layer should use Release 1.0 metadata bounds and collision")
    void snowLayerUsesMetadataBoundsAndCollision() {
        VoxelShape thinRender = BlockShape.renderShape(new BlockState(BlockType.SNOW_LAYER, 0), emptyContext());
        VoxelShape fullRender = BlockShape.renderShape(new BlockState(BlockType.SNOW_LAYER, 7), emptyContext());
        VoxelShape noCollision = BlockShape.collisionShape(new BlockState(BlockType.SNOW_LAYER, 0), emptyContext());
        VoxelShape thinCollision = BlockShape.collisionShape(new BlockState(BlockType.SNOW_LAYER, 2), emptyContext());
        VoxelShape thickCollision = BlockShape.collisionShape(new BlockState(BlockType.SNOW_LAYER, 3), emptyContext());
        VoxelShape topCollision = BlockShape.collisionShape(new BlockState(BlockType.SNOW_LAYER, 7), emptyContext());

        assertFalse(thinRender.isFullCube());
        assertEquals(2.0f / 16.0f, thinRender.boxes().get(0).maxY(), EPSILON);
        assertEquals(1.0f, fullRender.boxes().get(0).maxY(), EPSILON);
        assertTrue(noCollision.isEmpty());
        assertFalse(thinCollision.isEmpty());
        assertEquals(0.25f, thinCollision.boxes().get(0).maxY(), EPSILON);
        assertFalse(thickCollision.isEmpty());
        assertEquals(0.375f, thickCollision.boxes().get(0).maxY(), EPSILON);
        assertEquals(0.875f, topCollision.boxes().get(0).maxY(), EPSILON);
    }

    @Test
    @DisplayName("Snow layer should require vanilla opaque or leaf support")
    void snowLayerRequiresVanillaSupport() {
        assertTrue(BlockShape.canPlaceAt(BlockType.SNOW_LAYER, 0, contextWithBelow(BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.SNOW_LAYER, 0, contextWithBelow(BlockType.SNOW)));
        assertTrue(BlockShape.canPlaceAt(BlockType.SNOW_LAYER, 0, contextWithBelow(BlockType.LEAVES)));
        assertFalse(BlockShape.canPlaceAt(BlockType.SNOW_LAYER, 0, contextWithBelow(BlockType.AIR)));
        assertFalse(BlockShape.canPlaceAt(BlockType.SNOW_LAYER, 0, contextWithBelow(BlockType.GLASS)));
    }

    @Test
    @DisplayName("Snow layer should drop one snowball when directly broken")
    void snowLayerDropsOneSnowballWhenBroken() {
        World world = new World(170L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.SNOW_LAYER, 0);

            assertTrue(world.breakBlock(0, 100, 0, true));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertEquals(1, droppedCount(world, ItemType.SNOWBALL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-style snow layer drops should require shovel harvesting")
    void playerStyleSnowLayerDropsRequireShovelHarvest() {
        World world = new World(173L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.SNOW_LAYER, 0);

            assertTrue(world.breakBlock(0, 100, 0,
                    BlockHarvestRules.canHarvest(BlockType.SNOW_LAYER, null), null));
            assertEquals(0, droppedCount(world, ItemType.SNOWBALL));

            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(1, 100, 0, BlockType.SNOW_LAYER, 0);

            assertTrue(world.breakBlock(1, 100, 0,
                    BlockHarvestRules.canHarvest(BlockType.SNOW_LAYER, ItemType.WOODEN_SHOVEL),
                    ItemType.WOODEN_SHOVEL));
            assertEquals(1, droppedCount(world, ItemType.SNOWBALL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow layer should disappear without drops when support is removed")
    void snowLayerVanishesWhenUnsupported() {
        World world = new World(171L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.SNOW_LAYER, 0);

            assertTrue(world.breakBlock(0, 99, 0, false));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertEquals(0, droppedCount(world, ItemType.SNOWBALL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow layer should melt under block light above 11")
    void snowLayerMeltsUnderHighBlockLight() {
        World world = new World(172L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.SNOW_LAYER, 0);
            world.setBlock(1, 100, 0, BlockType.TORCH, Block.FACE_WEST);

            assertTrue(world.getBlockLight(0, 100, 0) > 11);
            world.advanceBlockTicks(20);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertEquals(0, droppedCount(world, ItemType.SNOWBALL));
        } finally {
            world.cleanup();
        }
    }

    private static BlockShape.BlockContext emptyContext() {
        return contextWithBelow(BlockType.AIR);
    }

    private static BlockShape.BlockContext contextWithBelow(BlockType below) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == 0 && dy == -1 && dz == 0 ? below : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }
}
