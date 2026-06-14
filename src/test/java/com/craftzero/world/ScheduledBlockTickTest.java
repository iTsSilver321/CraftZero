package com.craftzero.world;

import com.craftzero.entity.FallingBlockEntity;
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
}
