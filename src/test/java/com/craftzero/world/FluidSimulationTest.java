package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FluidSimulationTest {
    @Test
    @DisplayName("Water should flow downward before spreading sideways")
    void waterFlowsDownward() {
        World world = new World(31L);
        try {
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            world.advanceBlockTicks(5);

            assertSame(BlockType.FLOWING_WATER, world.getBlock(0, 99, 0));
            assertEquals(8, world.getBlockMetadata(0, 99, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water should spread horizontally with one-step decay when blocked below")
    void waterSpreadsHorizontallyWithDecay() {
        World world = new World(32L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(-1, 99, 0, BlockType.STONE);
            world.setBlock(0, 99, 1, BlockType.STONE);
            world.setBlock(0, 99, -1, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.WATER, 0);

            world.advanceBlockTicks(5);

            assertSame(BlockType.FLOWING_WATER, world.getBlock(1, 100, 0));
            assertEquals(1, world.getBlockMetadata(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water should regenerate sources from two adjacent source blocks")
    void waterRegeneratesSource() {
        World world = new World(33L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(-1, 100, 0, BlockType.WATER, 0);
            world.setBlock(1, 100, 0, BlockType.WATER, 0);
            world.setBlock(0, 100, 0, BlockType.FLOWING_WATER, 2);

            world.advanceBlockTicks(5);

            assertSame(BlockType.WATER, world.getBlock(0, 100, 0));
            assertEquals(0, world.getBlockMetadata(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lava should use two-step Overworld horizontal decay")
    void lavaUsesTwoStepDecay() {
        World world = new World(34L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(1, 100, 0, BlockType.AIR);
            world.setBlock(-1, 100, 0, BlockType.STONE);
            world.setBlock(0, 100, 1, BlockType.STONE);
            world.setBlock(0, 100, -1, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);

            world.advanceBlockTicks(30);

            assertSame(BlockType.FLOWING_LAVA, world.getBlock(1, 100, 0));
            assertEquals(2, world.getBlockMetadata(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water and lava should harden into obsidian, cobblestone, or stone")
    void waterAndLavaMixingRules() {
        World world = new World(35L);
        try {
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);
            world.setBlock(1, 100, 0, BlockType.WATER, 0);
            assertSame(BlockType.OBSIDIAN, world.getBlock(0, 100, 0));

            world.setBlock(2, 100, 0, BlockType.FLOWING_LAVA, 4);
            world.setBlock(3, 100, 0, BlockType.WATER, 0);
            assertSame(BlockType.COBBLESTONE, world.getBlock(2, 100, 0));

            world.setBlock(4, 101, 0, BlockType.LAVA, 0);
            world.setBlock(4, 100, 0, BlockType.WATER, 0);
            world.advanceBlockTicks(30);
            assertSame(BlockType.STONE, world.getBlock(4, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Weaker water flow should not overwrite stronger or source water")
    void weakerWaterDoesNotOverwriteStrongerWater() {
        World world = new World(36L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            world.setBlock(1, 100, 0, BlockType.FLOWING_WATER, 6);

            world.advanceBlockTicks(5);

            assertSame(BlockType.WATER, world.getBlock(0, 100, 0));
            assertEquals(0, world.getBlockMetadata(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }
}
