package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockedChestInteractionTest {

    @Test
    @DisplayName("Locked chest should use Release 1.0 legacy block semantics")
    void lockedChestUsesLegacyBlockSemantics() {
        VoxelShape shape = BlockShape.collisionShape(BlockState.of(BlockType.LOCKED_CHEST), emptyContext());

        assertTrue(shape.isFullCube());
        assertTrue(BlockShape.isOpaqueCube(BlockType.LOCKED_CHEST));
        assertEquals(15, BlockType.LOCKED_CHEST.getLightEmission());
        assertFalse(BlockType.LOCKED_CHEST.hasTileEntity());
        assertFalse(BlockType.LOCKED_CHEST.isContainerBlock());
        assertSame(BlockBehavior.SPECIAL, BlockBehavior.of(BlockType.LOCKED_CHEST));
        assertSame(ItemType.LOCKED_CHEST, BlockType.LOCKED_CHEST.getDroppedItem());
    }

    @Test
    @DisplayName("Locked chest should vanish on its scheduled update without drops")
    void lockedChestVanishesOnScheduledUpdateWithoutDrops() {
        World world = new World(180L);
        try {
            world.setBlock(0, 100, 0, BlockType.LOCKED_CHEST, 0);

            assertTrue(world.hasScheduledBlockTick(0, 100, 0, BlockType.LOCKED_CHEST));
            world.advanceBlockTicks(1);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getDroppedItems().isEmpty());
            assertFalse(world.hasScheduledBlockTick(0, 100, 0, BlockType.LOCKED_CHEST));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Locked chest should drop itself when broken before decay")
    void lockedChestDropsItselfWhenBrokenBeforeDecay() {
        World world = new World(181L);
        try {
            world.setBlock(0, 100, 0, BlockType.LOCKED_CHEST, 0);

            assertTrue(world.breakBlock(0, 100, 0, true));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertEquals(1, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.LOCKED_CHEST)
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
