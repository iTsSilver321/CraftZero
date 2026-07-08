package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LilyPadInteractionTest {
    @Test
    @DisplayName("Lily pads should use the source 1/64-block walkable shape")
    void lilyPadUsesThinWalkableShape() {
        VoxelShape collision = BlockShape.collisionShape(BlockState.of(BlockType.LILY_PAD), waterBelowContext());
        VoxelShape selection = BlockShape.selectionShape(BlockState.of(BlockType.LILY_PAD), waterBelowContext());

        assertFalse(collision.isEmpty());
        assertFalse(collision.isFullCube());
        assertEquals(1, collision.boxes().size());
        assertEquals(1.0f / 64.0f, collision.boxes().get(0).maxY(), 0.0001f);
        assertEquals(collision.boxes(), selection.boxes());
        assertFalse(BlockShape.canFallThrough(BlockType.LILY_PAD));
    }

    @Test
    @DisplayName("Lily pads should require level-0 water directly below")
    void lilyPadRequiresLevelZeroWaterBelow() {
        assertTrue(BlockShape.canPlaceAt(BlockType.LILY_PAD, 0, waterBelowContext()));
        assertFalse(BlockShape.canPlaceAt(BlockType.LILY_PAD, 0, blockBelowContext(BlockType.WATER, 3)));
        assertFalse(BlockShape.canPlaceAt(BlockType.LILY_PAD, 0, blockBelowContext(BlockType.DIRT)));
        assertFalse(BlockShape.canPlaceAt(BlockType.LILY_PAD, 0, blockBelowContext(BlockType.LAVA)));
    }

    @Test
    @DisplayName("World placement should allow lily pads above level-0 water only")
    void worldPlacesLilyPadAboveLevelZeroWaterOnly() {
        World world = new World(9501L);
        try {
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            assertTrue(world.canPlaceBlockAt(0, 101, 0, BlockType.LILY_PAD, 0, null));

            world.setBlock(1, 100, 0, BlockType.WATER, 3);
            assertFalse(world.canPlaceBlockAt(1, 101, 0, BlockType.LILY_PAD, 0, null));

            world.setBlock(2, 100, 0, BlockType.DIRT, 0);
            assertFalse(world.canPlaceBlockAt(2, 101, 0, BlockType.LILY_PAD, 0, null));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lily pads should break when the supporting water is removed")
    void lilyPadBreaksWhenWaterSupportIsRemoved() {
        World world = new World(9502L);
        try {
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            world.setBlock(0, 101, 0, BlockType.LILY_PAD, 0);

            assertTrue(world.breakBlock(0, 100, 0, false));

            assertSame(BlockType.AIR, world.getBlock(0, 101, 0));
            assertEquals(1, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.LILY_PAD)
                    .mapToInt(item -> item.getCount())
                    .sum());
        } finally {
            world.cleanup();
        }
    }

    private static BlockShape.BlockContext waterBelowContext() {
        return blockBelowContext(BlockType.WATER);
    }

    private static BlockShape.BlockContext blockBelowContext(BlockType below) {
        return blockBelowContext(below, 0);
    }

    private static BlockShape.BlockContext blockBelowContext(BlockType below, int metadata) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == 0 && dy == -1 && dz == 0 ? below : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return dx == 0 && dy == -1 && dz == 0 ? metadata : 0;
            }
        };
    }
}
