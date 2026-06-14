package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacementSupportTest {
    @Test
    @DisplayName("Plants should break and drop when their support is removed")
    void plantsBreakWhenSupportIsRemoved() {
        World world = new World(51L);
        try {
            world.setBlock(0, 70, 0, BlockType.DIRT);
            world.setBlock(0, 71, 0, BlockType.YELLOW_FLOWER);

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.YELLOW_FLOWER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cactus should break when adjacent solid blocks invalidate placement")
    void cactusBreaksWhenAdjacentSolidAppears() {
        World world = new World(52L);
        try {
            world.setBlock(0, 70, 0, BlockType.SAND);
            world.setBlock(0, 71, 0, BlockType.CACTUS);
            assertSame(BlockType.CACTUS, world.getBlock(0, 71, 0));

            world.setBlock(1, 71, 0, BlockType.STONE);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.CACTUS));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small non-solid blocks should not act as top placement anchors")
    void smallBlocksDoNotAnchorTopPlacement() {
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.TORCH, Block.FACE_TOP));
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.YELLOW_FLOWER, Block.FACE_TOP));
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.FIRE, Block.FACE_TOP));
        assertFalse(BlockShape.blocksPlacementAgainst(BlockType.STONE, Block.FACE_TOP));
    }

    @Test
    @DisplayName("Redstone and rail devices should be selectable but not full collision cubes")
    void redstoneAndRailDevicesUseThinNonCollidingShapes() {
        BlockShape.BlockContext context = emptyContextWithStoneBelow();
        BlockType[] thinBlocks = {
                BlockType.REDSTONE_WIRE,
                BlockType.RAIL,
                BlockType.POWERED_RAIL,
                BlockType.DETECTOR_RAIL,
                BlockType.REDSTONE_REPEATER_OFF,
                BlockType.REDSTONE_REPEATER_ON,
                BlockType.STONE_PRESSURE_PLATE,
                BlockType.WOODEN_PRESSURE_PLATE,
                BlockType.LEVER,
                BlockType.STONE_BUTTON
        };

        for (BlockType type : thinBlocks) {
            assertTrue(BlockShape.getCollisionBoxes(type, 5, context).isEmpty(), type + " should not block movement");
            assertFalse(BlockShape.getSelectionBoxes(type, 5, context).isEmpty(), type + " should be selectable");
            assertFalse(BlockShape.isFullCube(type, 5), type + " should not render as a full cube");
        }
    }

    private static BlockShape.BlockContext emptyContextWithStoneBelow() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dy == -1 ? BlockType.STONE : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }
}
