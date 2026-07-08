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

    @Test
    @DisplayName("Water buckets should evaporate instead of placing water in the Nether")
    void waterBucketsEvaporateInTheNether() {
        World world = new World(65L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            world.setBlock(0, 100, 0, BlockType.AIR, 0);
            assertTrue(world.placeFluidSource(0, 100, 0, true, null));

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertEquals(1, world.getSoundEvents().size());
            assertEquals(WorldSoundEvent.FIZZ, world.getSoundEvents().get(0).soundId());
            assertEquals(8, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.LARGE_SMOKE)
                    .count());
            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .count());

            world.setBlock(10, 100, 0, BlockType.AIR, 0);
            assertTrue(world.placeFluidSource(10, 100, 0, false, null));
            assertSame(BlockType.LAVA, world.getBlock(10, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cauldrons should fill from water buckets and drain one level per bottle")
    void cauldronWaterLevelInteractions() {
        World world = new World(63L);
        try {
            world.setBlock(0, 100, 0, BlockType.CAULDRON, 0);

            assertEquals(0, world.getCauldronLevel(0, 100, 0));
            assertTrue(world.fillCauldronFromWaterBucket(0, 100, 0));
            assertEquals(World.CAULDRON_MAX_LEVEL, world.getCauldronLevel(0, 100, 0));
            assertEquals(World.CAULDRON_MAX_LEVEL, world.getBlockMetadata(0, 100, 0));
            assertFalse(world.fillCauldronFromWaterBucket(0, 100, 0));

            assertNull(world.pickupFluidSource(0, 100, 0));
            assertEquals(World.CAULDRON_MAX_LEVEL, world.getCauldronLevel(0, 100, 0));

            assertTrue(world.drainCauldronIntoBottle(0, 100, 0));
            assertEquals(2, world.getCauldronLevel(0, 100, 0));
            assertTrue(world.drainCauldronIntoBottle(0, 100, 0));
            assertEquals(1, world.getCauldronLevel(0, 100, 0));
            assertTrue(world.drainCauldronIntoBottle(0, 100, 0));
            assertEquals(0, world.getCauldronLevel(0, 100, 0));
            assertFalse(world.drainCauldronIntoBottle(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cauldron helpers should ignore non-cauldron blocks")
    void cauldronInteractionsRejectOtherBlocks() {
        World world = new World(64L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 2);

            assertEquals(0, world.getCauldronLevel(0, 100, 0));
            assertFalse(world.fillCauldronFromWaterBucket(0, 100, 0));
            assertFalse(world.drainCauldronIntoBottle(0, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(0, 100, 0));
            assertEquals(2, world.getBlockMetadata(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }
}
