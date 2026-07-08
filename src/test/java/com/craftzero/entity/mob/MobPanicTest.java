package com.craftzero.entity.mob;

import com.craftzero.entity.ai.PanicGoal;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobPanicTest {
    @Test
    @DisplayName("Release 1.0 burning panic mobs should flee before delayed fire damage")
    void burningMobsPanicBeforeFireDamage() {
        World world = new World(6300L);
        try {
            prepareFlatPatch(world);

            InspectablePig pig = new InspectablePig();
            pig.random = new FixedRandom();
            pig.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(pig));

            float healthBefore = pig.getHealth();
            pig.setOnFire(80);

            world.updateEntities(1.0f / 20.0f);

            PanicGoal panicGoal = pig.getAI().getGoal(PanicGoal.class);
            assertNotNull(panicGoal);
            assertTrue(pig.getAI().isGoalActive(panicGoal));
            assertEquals(healthBefore, pig.getHealth(), 0.0001f);
            assertEquals(1.5f, pig.getForwardSpeedForTest(), 0.0001f);
            assertTrue(Math.abs(pig.getMotionX()) > 0.0f || Math.abs(pig.getMotionZ()) > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    private static void prepareFlatPatch(World world) {
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                world.setBlock(x, 69, z, BlockType.STONE, 0);
                for (int y = 70; y <= 73; y++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static final class InspectablePig extends Pig {
        private float getForwardSpeedForTest() {
            return forwardSpeed;
        }
    }

    private static final class FixedRandom extends java.util.Random {
        @Override
        public float nextFloat() {
            return 0.0f;
        }
    }
}
