package com.craftzero.world;

import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

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
    @DisplayName("Standing sand should not start falling through a torch block")
    void standingSandDoesNotStartFallingThroughTorch() {
        World world = new World(47L);
        try {
            world.setBlock(0, 68, 0, BlockType.STONE);
            world.setBlock(0, 69, 0, BlockType.TORCH, 5);
            world.setBlock(0, 70, 0, BlockType.SAND);

            world.advanceBlockTicks(3);
            runEntities(world, 20);

            assertSame(BlockType.SAND, world.getBlock(0, 70, 0));
            assertSame(BlockType.TORCH, world.getBlock(0, 69, 0));
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling sand should replace fire and ground cover when it lands")
    void fallingSandReplacesFireAndGroundCoverOnLanding() {
        assertFallingSandReplacesLandingTarget(BlockType.FIRE, 0);
        assertFallingSandReplacesLandingTarget(BlockType.TALL_GRASS, 1);
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

    private static void assertFallingSandReplacesLandingTarget(BlockType target, int metadata) {
        World world = new World(48L + target.ordinal());
        try {
            world.setBlock(0, 68, 0, BlockType.STONE);
            world.setBlock(0, 69, 0, target, metadata);
            world.setBlock(0, 71, 0, BlockType.SAND);

            world.advanceBlockTicks(3);
            runEntities(world, 100);

            assertSame(BlockType.SAND, world.getBlock(0, 69, 0));
            assertTrue(world.getDroppedItems().isEmpty(), target + " left drops: "
                    + world.getDroppedItems().stream()
                            .map(item -> item.getItemType() + ":" + item.getCount())
                            .toList());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling blocks outside vertical bounds wait before dropping")
    void fallingBlockBelowWorldWaitsBeforeDropping() {
        World world = new World(44L);
        try {
            FallingBlockEntity falling = new FallingBlockEntity(BlockType.SAND, 0);
            falling.setPosition(0.5f, -2.0f, 0.5f);
            falling.setMotion(0.0f, 0.0f, 0.0f);
            world.replaceEntities(List.of(falling));

            runEntities(world, 100);
            assertFalse(falling.isRemoved());
            assertTrue(world.getDroppedItems().isEmpty());

            world.updateEntities(1.0f / 20.0f);
            assertTrue(falling.isRemoved());
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.SAND));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling blocks at the top world boundary fall back in before timing out")
    void fallingBlockAtTopBoundaryDoesNotDropImmediately() {
        World world = new World(45L);
        try {
            FallingBlockEntity falling = new FallingBlockEntity(BlockType.SAND, 0);
            falling.setPosition(0.5f, Chunk.HEIGHT, 0.5f);
            falling.setMotion(0.0f, 0.0f, 0.0f);
            falling.setTicksExisted(100);
            world.replaceEntities(List.of(falling));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(falling.isRemoved());
            assertTrue(world.getDroppedItems().isEmpty());
            assertTrue(falling.getY() < Chunk.HEIGHT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling blocks still drop after the hard lifetime timeout")
    void fallingBlockDropsAfterHardTimeout() {
        World world = new World(46L);
        try {
            FallingBlockEntity falling = new FallingBlockEntity(BlockType.GRAVEL, 0);
            falling.setPosition(0.5f, 64.0f, 0.5f);
            falling.setMotion(0.0f, 0.0f, 0.0f);
            falling.setTicksExisted(600);
            world.replaceEntities(List.of(falling));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(falling.isRemoved());
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.GRAVEL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling gravel should drop the block item instead of contextual flint")
    void fallingGravelDropsBlockItemInsteadOfContextualFlint() {
        World world = new RandomOverrideWorld(49L, fixedIntRandom(0));
        try {
            FallingBlockEntity falling = new FallingBlockEntity(BlockType.GRAVEL, 0);
            falling.setPosition(0.5f, 64.0f, 0.5f);
            falling.setMotion(0.0f, 0.0f, 0.0f);
            falling.setTicksExisted(600);
            world.replaceEntities(List.of(falling));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(falling.isRemoved());
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.GRAVEL));
            assertTrue(world.getDroppedItems().stream().noneMatch(item -> item.getItemType() == ItemType.FLINT));
        } finally {
            world.cleanup();
        }
    }

    private static void runEntities(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }

    private static Random fixedIntRandom(int value) {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return Math.max(0, Math.min(value, bound - 1));
            }
        };
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }
}
