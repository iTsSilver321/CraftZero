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
            loadFluidTickNeighborhood(world);
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
            loadFluidTickNeighborhood(world);
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
            loadFluidTickNeighborhood(world);
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
            loadFluidTickNeighborhood(world);
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
    @DisplayName("Lava should use one-step Nether horizontal decay")
    void lavaUsesOneStepDecayInNether() {
        World world = new World(37L, WorldGenerators.generatorIdFor(Dimension.NETHER), Dimension.NETHER);
        try {
            loadFluidTickNeighborhood(world);
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(1, 100, 0, BlockType.AIR);
            world.setBlock(-1, 100, 0, BlockType.STONE);
            world.setBlock(0, 100, 1, BlockType.STONE);
            world.setBlock(0, 100, -1, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);

            world.advanceBlockTicks(30);

            assertSame(BlockType.FLOWING_LAVA, world.getBlock(1, 100, 0));
            assertEquals(1, world.getBlockMetadata(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water and lava should harden into obsidian, cobblestone, or stone")
    void waterAndLavaMixingRules() {
        World world = new World(35L);
        try {
            loadFluidTickNeighborhood(world);
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
    @DisplayName("Lava mixing should emit the old fizz and large-smoke feedback")
    void lavaMixingEmitsFizzAndLargeSmokeFeedback() {
        World world = new World(353L);
        try {
            loadFluidTickNeighborhood(world);
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);
            world.setBlock(1, 100, 0, BlockType.WATER, 0);

            var sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            WorldSoundEvent fizz = sounds.get(0);
            assertEquals(WorldSoundEvent.FIZZ, fizz.soundId());
            assertEquals(0.5f, fizz.x(), 0.0001f);
            assertEquals(100.5f, fizz.y(), 0.0001f);
            assertEquals(0.5f, fizz.z(), 0.0001f);
            assertEquals(0.5f, fizz.volume(), 0.0001f);
            assertTrue(fizz.pitch() >= 1.8f);
            assertTrue(fizz.pitch() <= 3.4f);

            assertEquals(8, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.LARGE_SMOKE, particle.getType());
                assertTrue(particle.getRenderX(0.0f) >= 0.0f && particle.getRenderX(0.0f) < 1.0f);
                assertEquals(101.2f, particle.getRenderY(0.0f), 0.0001f);
                assertTrue(particle.getRenderZ(0.0f) >= 0.0f && particle.getRenderZ(0.0f) < 1.0f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water should not harden shallow lava levels above the Release threshold")
    void waterDoesNotHardenShallowLavaLevels() {
        World world = new World(352L);
        try {
            loadFluidTickNeighborhood(world);
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(1, 100, 0, BlockType.FLOWING_LAVA, 5);

            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            world.advanceBlockTicks(5);

            assertSame(BlockType.FLOWING_LAVA, world.getBlock(1, 100, 0));
            assertEquals(5, world.getBlockMetadata(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water and lava should still harden across loaded chunk borders")
    void waterAndLavaMixAcrossLoadedChunkBorders() {
        World world = new World(351L, WorldGenerator.LEGACY_CRAFTZERO);
        try {
            Chunk west = world.getChunk(0, 0);
            Chunk east = world.getChunk(1, 0);
            west.setState(Chunk.ChunkState.GENERATED);
            east.setState(Chunk.ChunkState.GENERATED);
            east.setBlock(0, 100, 0, BlockType.WATER, 0);

            world.setBlock(15, 100, 0, BlockType.LAVA, 0);

            assertSame(BlockType.OBSIDIAN, west.getBlock(15, 100, 0));
            assertSame(BlockType.WATER, east.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Weaker water flow should not overwrite stronger or source water")
    void weakerWaterDoesNotOverwriteStrongerWater() {
        World world = new World(36L);
        try {
            loadFluidTickNeighborhood(world);
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

    private static void loadFluidTickNeighborhood(World world) {
        int[][] chunks = {
                { 0, 0 },
                { 1, 0 },
                { -1, 0 },
                { 0, 1 },
                { 0, -1 }
        };
        for (int[] chunk : chunks) {
            world.getChunk(chunk[0], chunk[1]).setState(Chunk.ChunkState.GENERATED);
        }
    }
}
