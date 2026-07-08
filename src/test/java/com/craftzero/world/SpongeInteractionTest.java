package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpongeInteractionTest {

    @Test
    @DisplayName("Sponge should be a full opaque Release 1.0 block")
    void spongeUsesFullOpaqueBlockShape() {
        VoxelShape shape = BlockShape.collisionShape(BlockState.of(BlockType.SPONGE), emptyContext());

        assertFalse(shape.isEmpty());
        assertTrue(shape.isFullCube());
        assertTrue(BlockShape.isOpaqueCube(BlockType.SPONGE));
        assertSame(ItemType.SPONGE, BlockType.SPONGE.getDroppedItem());
    }

    @Test
    @DisplayName("Release 1.0 sponge should not absorb adjacent water")
    void spongeDoesNotAbsorbAdjacentWater() {
        World world = new World(180L);
        try {
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            world.setBlock(1, 100, 0, BlockType.SPONGE, 0);

            world.advanceBlockTicks(5);

            assertSame(BlockType.WATER, world.getBlock(0, 100, 0));
            assertSame(BlockType.SPONGE, world.getBlock(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sponge should drop itself when broken")
    void spongeDropsItself() {
        World world = new World(181L);
        try {
            world.setBlock(0, 100, 0, BlockType.SPONGE, 0);

            assertTrue(world.breakBlock(0, 100, 0, true));
            assertEquals(1, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.SPONGE)
                    .mapToInt(item -> item.getCount())
                    .sum());
        } finally {
            world.cleanup();
        }
    }

    private static BlockShape.BlockContext emptyContext() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }
}
