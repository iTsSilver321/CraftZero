package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BucketInteractionTest {
    @Test
    @DisplayName("Buckets should pick up only source fluids")
    void bucketsPickUpOnlySourceFluids() {
        World world = new World(61L);
        try {
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            assertSame(ItemType.WATER_BUCKET, world.pickupFluidSource(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));

            world.setBlock(10, 100, 0, BlockType.FLOWING_WATER, 3);
            assertNull(world.pickupFluidSource(10, 100, 0));
            assertSame(BlockType.FLOWING_WATER, world.getBlock(10, 100, 0));

            world.setBlock(20, 100, 0, BlockType.LAVA, 0);
            assertSame(ItemType.LAVA_BUCKET, world.pickupFluidSource(20, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Filled buckets should place source fluid blocks")
    void filledBucketsPlaceSourceFluids() {
        World world = new World(62L);
        try {
            assertTrue(world.placeFluidSource(0, 100, 0, true, null));
            assertSame(BlockType.WATER, world.getBlock(0, 100, 0));
            assertEquals(0, world.getBlockMetadata(0, 100, 0));

            assertTrue(world.placeFluidSource(10, 100, 0, false, null));
            assertSame(BlockType.LAVA, world.getBlock(10, 100, 0));
            assertEquals(0, world.getBlockMetadata(10, 100, 0));
        } finally {
            world.cleanup();
        }
    }
}
