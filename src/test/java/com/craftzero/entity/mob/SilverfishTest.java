package com.craftzero.entity.mob;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SilverfishTest {
    @Test
    @DisplayName("Idle silverfish should re-enter compatible stone blocks as monster eggs")
    void idleSilverfishHidesInCompatibleStoneBlock() {
        World world = new World(6200L);
        try {
            world.setBlock(1, 100, 0, BlockType.COBBLESTONE, 0);
            Silverfish silverfish = new Silverfish();
            silverfish.setPosition(0.5f, 100.0f, 0.5f);
            silverfish.random = new SequenceRandom(new int[] { 999, 4 }, new float[] { 0.5f });
            world.spawnEntity(silverfish);

            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.INFESTED_STONE, world.getBlock(1, 100, 0));
            assertEquals(1, world.getBlockMetadata(1, 100, 0),
                    "Cobblestone hosts should become cobblestone monster eggs");
            assertTrue(silverfish.isRemoved());
            assertEquals(0L, world.getEntities().stream().filter(Silverfish.class::isInstance).count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Silverfish should not hide while pursuing an active move target")
    void movingSilverfishDoesNotHide() {
        World world = new World(6201L);
        try {
            world.setBlock(1, 100, 0, BlockType.STONE_BRICK, 0);
            Silverfish silverfish = new Silverfish();
            silverfish.setPosition(0.5f, 100.0f, 0.5f);
            silverfish.getAI().setMoveTarget(4.0f, 0.5f);
            silverfish.random = new SequenceRandom(new int[] { 999, 4 }, new float[] { 0.5f });
            world.spawnEntity(silverfish);

            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.STONE_BRICK, world.getBlock(1, 100, 0));
            assertFalse(silverfish.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    private static final class SequenceRandom extends Random {
        private final int[] ints;
        private final float[] floats;
        private int intIndex;
        private int floatIndex;

        private SequenceRandom(int[] ints, float[] floats) {
            this.ints = ints;
            this.floats = floats;
        }

        @Override
        public int nextInt(int bound) {
            if (intIndex >= ints.length) {
                return bound - 1;
            }
            int value = ints[intIndex++];
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
            }
            return value;
        }

        @Override
        public float nextFloat() {
            if (floatIndex >= floats.length) {
                return 0.5f;
            }
            return floats[floatIndex++];
        }
    }
}
