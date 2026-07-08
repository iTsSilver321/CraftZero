package com.craftzero.entity.ai;

import com.craftzero.entity.mob.Mob;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobGoalRandomnessTest {

    @Test
    @DisplayName("Idle look-at-player chance should use the mob RNG")
    void lookAtPlayerChanceUsesMobRandom() {
        World world = new World(9141L);
        try {
            Player player = new Player(2.0f, 70.0f, 0.0f);
            world.setPlayer(player);

            TestMob mob = new TestMob();
            mob.setRandom(new SequenceRandom(0.11f));
            mob.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(mob));

            assertFalse(new LookAtPlayerGoal(mob, 8.0f).canUse());

            mob.setRandom(new SequenceRandom(0.10f));
            assertTrue(new LookAtPlayerGoal(mob, 8.0f).canUse());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Idle look-at-player should yield to movement goals")
    void lookAtPlayerOnlyStartsWhenMobIsIdle() {
        World world = new World(9145L);
        try {
            Player player = new Player(2.0f, 70.0f, 0.0f);
            world.setPlayer(player);

            TestMob mob = new TestMob();
            SequenceRandom random = new SequenceRandom(0.0f);
            mob.setRandom(random);
            mob.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(mob));

            mob.getAI().setMoveTarget(4.0f, 0.0f);

            assertFalse(new LookAtPlayerGoal(mob, 8.0f).canUse());
            assertEquals(0, random.floatCalls(), "movement should block before consuming idle-look randomness");

            mob.getAI().clearMoveTarget();
            LookAtPlayerGoal activeGoal = new LookAtPlayerGoal(mob, 8.0f);
            assertTrue(activeGoal.canUse());
            activeGoal.start();

            mob.getAI().setMoveTarget(4.0f, 0.0f);

            assertFalse(activeGoal.canContinue());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ranged attack strafing should use the mob RNG")
    void rangedAttackStrafingUsesMobRandom() {
        TestMob clockwise = new TestMob();
        clockwise.setRandom(new SequenceRandom(0.75f));
        RangedAttackGoal clockwiseGoal = new RangedAttackGoal(clockwise, clockwise.getAI(), 16.0f, 40);
        clockwiseGoal.start();
        assertTrue(clockwiseGoal.getState().strafingClockwise());

        TestMob counterClockwise = new TestMob();
        counterClockwise.setRandom(new SequenceRandom(0.25f));
        RangedAttackGoal counterGoal = new RangedAttackGoal(counterClockwise, counterClockwise.getAI(), 16.0f, 40);
        counterGoal.start();
        assertFalse(counterGoal.getState().strafingClockwise());
    }

    @Test
    @DisplayName("Ranged attack strafe flips should use the mob RNG")
    void rangedAttackStrafeFlipsUseMobRandom() {
        World world = new World(9142L);
        try {
            world.getChunkNow(0, 0);
            for (int x = 0; x <= 10; x++) {
                for (int y = 70; y <= 73; y++) {
                    world.setBlock(x, y, 0, BlockType.AIR, 0);
                }
            }
            Player player = new Player(8.5f, 70.0f, 0.5f);
            world.setPlayer(player);

            TestMob mob = new TestMob();
            mob.setRandom(new SequenceRandom(0.20f));
            mob.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(mob));

            RangedAttackGoal goal = new RangedAttackGoal(mob, mob.getAI(), 16.0f, 40);
            goal.restoreState(new RangedAttackGoal.State(5, 20, true, 0.5f), false);

            goal.tick();

            assertFalse(goal.getState().strafingClockwise());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wander target selection should use the mob RNG")
    void wanderTargetSelectionUsesMobRandom() {
        World world = new World(9143L);
        try {
            prepareFlatPad(world, -6, 6, -6, 6);

            TestMob mob = new TestMob();
            SequenceRandom random = new SequenceRandom(0.01f, 0.0f, 0.0f);
            mob.setRandom(random);
            mob.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(mob));

            assertTrue(new WanderGoal(mob, mob.getAI(), 8.0f, 0.6f).canUse());
            assertTrue(random.floatCalls() >= 3);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Panic flee selection should use the mob RNG")
    void panicFleeSelectionUsesMobRandom() {
        World world = new World(9144L);
        try {
            prepareFlatPad(world, -12, 12, -12, 12);

            TestMob mob = new TestMob();
            SequenceRandom random = new SequenceRandom(0.0f, 0.5f);
            mob.setRandom(random);
            mob.setHurtTime(10);
            mob.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(mob));

            assertTrue(new PanicGoal(mob, mob.getAI(), 1.5f).canUse());
            assertTrue(random.floatCalls() >= 2);
        } finally {
            world.cleanup();
        }
    }

    private static void prepareFlatPad(World world, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, 69, z, BlockType.STONE, 0);
                for (int y = 70; y <= 73; y++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static final class TestMob extends Mob {
        private TestMob() {
            super(0.6f, 1.8f, 20.0f);
        }

        private void setRandom(Random random) {
            this.random = random;
        }

        private void setHurtTime(int hurtTime) {
            this.hurtTime = hurtTime;
        }

        @Override
        public void dropLoot() {
        }

        @Override
        public String getTexturePath() {
            return "/textures/mob/test.png";
        }

        @Override
        public Mob.MobModelType getModelType() {
            return Mob.MobModelType.HUMANOID;
        }
    }

    private static final class SequenceRandom extends Random {
        private final float[] values;
        private int index;

        private SequenceRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            if (values.length == 0) {
                return 0.0f;
            }
            float value = values[index % values.length];
            index++;
            return value;
        }

        private int floatCalls() {
            return index;
        }
    }
}
