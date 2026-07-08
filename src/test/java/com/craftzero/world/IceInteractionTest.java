package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IceInteractionTest {
    @Test
    @DisplayName("Release 1.0 ice should melt into still water under high block light")
    void iceMeltsIntoWaterUnderHighBlockLight() {
        World world = new World(176L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.ICE, 0);
            world.setBlock(1, 100, 0, BlockType.TORCH, Block.FACE_WEST);

            assertTrue(world.getBlockLight(0, 100, 0) > 8);
            world.advanceBlockTicks(20);

            assertSame(BlockType.WATER, world.getBlock(0, 100, 0));
            assertEquals(0, world.getBlockMetadata(0, 100, 0));
            assertEquals(0, droppedCount(world, ItemType.ICE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 ice should stay frozen without block-light melt pressure")
    void iceStaysFrozenWithoutBlockLight() {
        World world = new World(177L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.ICE, 0);

            assertEquals(0, world.getBlockLight(0, 100, 0));
            world.advanceBlockTicks(40);

            assertSame(BlockType.ICE, world.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 mined ice over solid or liquid support should become flowing water")
    void minedIceOverSupportBecomesFlowingWater() {
        World world = new World(178L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.ICE, 0);

            assertTrue(world.breakBlock(0, 100, 0, true));

            assertSame(BlockType.FLOWING_WATER, world.getBlock(0, 100, 0));
            assertEquals(0, world.getBlockMetadata(0, 100, 0));
            assertEquals(0, droppedCount(world, ItemType.ICE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 Nether ice melt should not leave water behind")
    void netherIceMeltDoesNotCreateWater() {
        World world = new World(179L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            world.setBlock(0, 99, 0, BlockType.NETHERRACK);
            world.setBlock(0, 100, 0, BlockType.ICE, 0);
            world.setBlock(1, 100, 0, BlockType.TORCH, Block.FACE_WEST);

            assertTrue(world.getBlockLight(0, 100, 0) > 8);
            world.advanceBlockTicks(20);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }
}
