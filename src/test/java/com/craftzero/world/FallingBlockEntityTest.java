package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FallingBlockEntityTest {
    @Test
    @DisplayName("Falling sand should place itself when it lands on valid ground")
    void fallingSandPlacesOnValidGround() {
        World world = new World(41L);
        try {
            world.setBlock(0, 68, 0, BlockType.STONE);
            world.setBlock(0, 70, 0, BlockType.SAND);

            world.advanceBlockTicks(3);
            runEntities(world, 80);

            assertSame(BlockType.SAND, world.getBlock(0, 69, 0));
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling sand should break into an item when it lands on a torch")
    void fallingSandBreaksOnTorch() {
        World world = new World(42L);
        try {
            world.setBlock(0, 68, 0, BlockType.STONE);
            world.setBlock(0, 69, 0, BlockType.TORCH, 5);
            world.setBlock(0, 71, 0, BlockType.SAND);

            world.advanceBlockTicks(3);
            runEntities(world, 100);

            assertSame(BlockType.TORCH, world.getBlock(0, 69, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.SAND));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling sand should sink through water and settle submerged on solid ground")
    void fallingSandSettlesSubmergedInWater() {
        World world = new World(43L);
        try {
            world.setBlock(0, 68, 0, BlockType.STONE);
            world.setBlock(0, 69, 0, BlockType.WATER, 0);
            world.setBlock(0, 70, 0, BlockType.FLOWING_WATER, 8);
            world.setBlock(0, 72, 0, BlockType.SAND);

            world.advanceBlockTicks(3);
            runEntities(world, 120);

            assertSame(BlockType.SAND, world.getBlock(0, 69, 0));
            assertSame(BlockType.FLOWING_WATER, world.getBlock(0, 70, 0));
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling gravel should keep contextual flint drops for normal block breaking")
    void gravelDropResolverStillProducesFlint() {
        assertSame(ItemType.FLINT, BlockDropResolver.getDrop(BlockType.GRAVEL, new java.util.Random() {
            @Override
            public float nextFloat() {
                return 0.05f;
            }
        }).getType());
    }

    private static void runEntities(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }
}
