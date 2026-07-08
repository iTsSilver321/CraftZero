package com.craftzero.world;

import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.graphics.Camera;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledBlockTickTest {
    @Test
    @DisplayName("Scheduled block ticks should be duplicate-safe and invalidate on block changes")
    void scheduledTicksAreDuplicateSafeAndInvalidate() {
        World world = new World(21L);
        try {
            world.setBlock(0, 100, 0, BlockType.WATER, 0);
            int scheduled = world.getScheduledBlockTickCount();
            world.scheduleBlockTick(0, 100, 0, BlockType.WATER, 5);
            assertEquals(scheduled, world.getScheduledBlockTickCount());

            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            assertFalse(world.hasScheduledBlockTick(0, 100, 0, BlockType.WATER));
            world.advanceBlockTicks(10);
            assertSame(BlockType.STONE, world.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling blocks should respect their 3 tick schedule")
    void fallingBlocksUseThreeTickSchedule() {
        World world = new World(22L);
        try {
            world.setBlock(0, 100, 0, BlockType.SAND);
            assertTrue(world.hasScheduledBlockTick(0, 100, 0, BlockType.SAND));

            world.advanceBlockTicks(2);
            assertSame(BlockType.SAND, world.getBlock(0, 100, 0));

            world.advanceBlockTicks(1);
            world.updateEntities(1.0f / 20.0f);
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof FallingBlockEntity));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Generated chunks should not wake dormant unsupported falling blocks")
    void generatedChunksDoNotScheduleDormantFallingBlocks() {
        World world = new World(23L, WorldGenerator.LEGACY_CRAFTZERO);
        try {
            Chunk chunk = world.getChunk(0, 0);
            chunk.setBlock(1, 20, 1, BlockType.SAND);
            chunk.setState(Chunk.ChunkState.GENERATED);

            world.update(new Camera(new Vector3f(8.0f, 70.0f, 8.0f)));

            assertFalse(world.hasScheduledBlockTick(1, 20, 1, BlockType.SAND));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Due scheduled ticks should not synchronously generate unloaded chunks")
    void scheduledTicksDoNotGenerateUnloadedChunks() {
        World world = new World(24L, WorldGenerator.LEGACY_CRAFTZERO);
        try {
            int x = 4096;
            int z = 4096;
            int chunkX = Math.floorDiv(x, Chunk.WIDTH);
            int chunkZ = Math.floorDiv(z, Chunk.DEPTH);

            world.scheduleBlockTick(x, 64, z, BlockType.WATER, 0);
            world.advanceBlockTicks(1);

            assertNull(world.getLoadedChunk(chunkX, chunkZ));
            assertFalse(world.hasScheduledBlockTick(x, 64, z, BlockType.WATER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block-change fluid probes should not synchronously generate unloaded neighbor chunks")
    void blockChangeFluidProbesDoNotGenerateNeighborChunks() {
        World world = new World(25L, WorldGenerator.LEGACY_CRAFTZERO);
        try {
            Chunk chunk = world.getChunk(0, 0);
            chunk.setState(Chunk.ChunkState.GENERATED);

            world.setBlock(15, 64, 0, BlockType.STONE, 0);

            assertSame(BlockType.STONE, chunk.getBlock(15, 64, 0));
            assertNull(world.getLoadedChunk(1, 0));
            assertNull(world.getLoadedChunk(0, -1));
        } finally {
            world.cleanup();
        }
    }
}
