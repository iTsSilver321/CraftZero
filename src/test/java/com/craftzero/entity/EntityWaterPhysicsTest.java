package com.craftzero.entity;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityWaterPhysicsTest {
    @Test
    @DisplayName("Generic entities should use Release-style water drag and gravity without surface lift")
    void genericEntityWaterPhysicsUsesReleaseDragAndGravity() {
        World world = new World(505L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);

            TestEntity entity = new TestEntity(0.5f, 90.0f, 0.5f);
            entity.setWorld(world);

            entity.updatePhysics(1.0f / 20.0f);

            assertEquals(-0.02f, entity.getMotionY(), 0.0001f);
            assertEquals(89.98f, entity.getY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Generic entity water drag should damp all axes before water gravity")
    void genericEntityWaterDragDampsMotionBeforeGravity() {
        World world = new World(506L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);

            TestEntity entity = new TestEntity(0.5f, 90.0f, 0.5f);
            entity.setWorld(world);
            entity.setMotion(0.5f, -0.1f, -0.25f);

            entity.updatePhysics(1.0f / 20.0f);

            assertEquals(0.4f, entity.getMotionX(), 0.0001f);
            assertEquals(-0.1f, entity.getMotionY(), 0.0001f);
            assertEquals(-0.2f, entity.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flowing water should carry generic entities along decay gradients")
    void flowingWaterCurrentCarriesGenericEntities() {
        World world = new World(509L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(1, 90, 0, BlockType.FLOWING_WATER, 1);

            TestEntity entity = new TestEntity(1.5f, 90.0f, 0.5f);
            entity.setWorld(world);

            entity.updatePhysics(1.0f / 20.0f);

            assertTrue(entity.getMotionX() > 0.01f, () -> "motionX=" + entity.getMotionX());
            assertTrue(entity.getX() > 1.51f, () -> "x=" + entity.getX());
            assertEquals(0.0f, entity.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Generic entities should emit source-style water entry bursts after the first dry tick")
    void genericEntitiesEmitWaterEntryParticlesAfterDryToWetTransition() {
        World world = new World(514L);
        try {
            clearWaterFixture(world);
            TestEntity entity = new TestEntity(0.5f, 90.0f, 0.5f);
            entity.setWorld(world);

            entity.updatePhysics(1.0f / 20.0f);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            entity.updatePhysics(1.0f / 20.0f);

            assertEquals(13, particleCount(world, WorldParticle.Type.BUBBLE));
            assertEquals(13, particleCount(world, WorldParticle.Type.SPLASH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Generic entities spawned already in water should not emit entry bursts on first physics update")
    void genericEntitiesSkipWaterEntryParticlesOnFirstWetUpdate() {
        World world = new World(515L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            TestEntity entity = new TestEntity(0.5f, 90.0f, 0.5f);
            entity.setWorld(world);

            entity.updatePhysics(1.0f / 20.0f);

            assertEquals(0, particleCount(world, WorldParticle.Type.BUBBLE));
            assertEquals(0, particleCount(world, WorldParticle.Type.SPLASH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Falling water sheets should add the old downward current pull")
    void fallingWaterCurrentPullsEntitiesDownward() {
        World world = new World(510L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.FLOWING_WATER, 8);
            world.setBlock(1, 90, 0, BlockType.STONE, 0);

            TestEntity entity = new TestEntity(0.5f, 90.0f, 0.5f);
            entity.setWorld(world);

            entity.updatePhysics(1.0f / 20.0f);

            assertTrue(entity.getMotionY() < -0.03f, () -> "motionY=" + entity.getMotionY());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Generic entities should use Release-style heavy lava drag and gravity")
    void genericEntityLavaPhysicsUsesReleaseDragAndGravity() {
        World world = new World(511L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.LAVA, 0);

            TestEntity entity = new TestEntity(0.5f, 90.0f, 0.5f);
            entity.setWorld(world);
            entity.setMotion(0.5f, -0.1f, -0.25f);

            entity.updatePhysics(1.0f / 20.0f);

            assertTrue(entity.isInLava());
            assertEquals(0.25f, entity.getMotionX(), 0.0001f);
            assertEquals(-0.07f, entity.getMotionY(), 0.0001f);
            assertEquals(-0.125f, entity.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flowing lava should carry generic entities along decay gradients")
    void flowingLavaCurrentCarriesGenericEntities() {
        World world = new World(512L);
        try {
            clearWaterFixture(world);
            world.setBlock(0, 90, 0, BlockType.LAVA, 0);
            world.setBlock(1, 90, 0, BlockType.FLOWING_LAVA, 2);

            TestEntity entity = new TestEntity(1.5f, 90.0f, 0.5f);
            entity.setWorld(world);

            entity.updatePhysics(1.0f / 20.0f);

            assertTrue(entity.isInLava());
            assertTrue(entity.getMotionX() > 0.01f, () -> "motionX=" + entity.getMotionX());
            assertTrue(entity.getX() > 1.51f, () -> "x=" + entity.getX());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living entities should drown after the Release-era air counter expires")
    void livingEntitiesUseReleaseAirCounterForDrowning() {
        World world = new World(507L);
        try {
            fillWaterColumn(world);
            TestLivingEntity entity = new TestLivingEntity(0.5f, 90.0f, 0.5f);
            world.replaceEntities(List.of(entity));

            runTicks(world, LivingEntity.MAX_AIR_TICKS);

            assertEquals(entity.getMaxHealth(), entity.getHealth(), 0.0001f);
            assertEquals(0, entity.getAirTicks());

            runTicks(world, -LivingEntity.DROWN_DAMAGE_AIR_TICKS);

            assertEquals(entity.getMaxHealth() - LivingEntity.DROWN_DAMAGE, entity.getHealth(), 0.0001f);
            assertEquals(0, entity.getAirTicks());
            assertEquals(8, bubbleParticles(world));

            runTicks(world, -LivingEntity.DROWN_DAMAGE_AIR_TICKS);

            assertEquals(entity.getMaxHealth() - LivingEntity.DROWN_DAMAGE * 2.0f, entity.getHealth(), 0.0001f);
            assertEquals(16, bubbleParticles(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Underwater breathers should opt out of generic living-entity drowning")
    void underwaterBreathersDoNotUseGenericDrowning() {
        World world = new World(508L);
        try {
            fillWaterColumn(world);
            TestWaterBreather entity = new TestWaterBreather(0.5f, 90.0f, 0.5f);
            world.replaceEntities(List.of(entity));

            runTicks(world, LivingEntity.MAX_AIR_TICKS - LivingEntity.DROWN_DAMAGE_AIR_TICKS);

            assertEquals(entity.getMaxHealth(), entity.getHealth(), 0.0001f);
            assertEquals(LivingEntity.MAX_AIR_TICKS, entity.getAirTicks());
            assertEquals(0, bubbleParticles(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water Breathing should opt living entities out of generic drowning")
    void waterBreathingEffectPreventsGenericLivingEntityDrowning() {
        World world = new World(513L);
        try {
            fillWaterColumn(world);
            TestLivingEntity entity = new TestLivingEntity(0.5f, 90.0f, 0.5f);
            entity.addEffect(new StatusEffectInstance(StatusEffectType.WATER_BREATHING,
                    LivingEntity.MAX_AIR_TICKS * 2, 0));
            world.replaceEntities(List.of(entity));

            runTicks(world, LivingEntity.MAX_AIR_TICKS - LivingEntity.DROWN_DAMAGE_AIR_TICKS);

            assertEquals(entity.getMaxHealth(), entity.getHealth(), 0.0001f);
            assertEquals(LivingEntity.MAX_AIR_TICKS, entity.getAirTicks());
            assertEquals(0, bubbleParticles(world));
        } finally {
            world.cleanup();
        }
    }

    private static long bubbleParticles(World world) {
        return particleCount(world, WorldParticle.Type.BUBBLE);
    }

    private static long particleCount(World world, WorldParticle.Type type) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == type)
                .count();
    }

    private static void clearWaterFixture(World world) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 88; y <= 93; y++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void fillWaterColumn(World world) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 50; y <= 95; y++) {
                    world.setBlock(x, y, z, BlockType.WATER, 0);
                }
            }
        }
    }

    private static void runTicks(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }

    private static final class TestEntity extends Entity {
        private TestEntity(float x, float y, float z) {
            super(0.6f, 1.8f);
            setPosition(x, y, z);
        }
    }

    private static class TestLivingEntity extends LivingEntity {
        private TestLivingEntity(float x, float y, float z) {
            super(0.6f, 1.8f, 20.0f);
            setPosition(x, y, z);
        }
    }

    private static final class TestWaterBreather extends TestLivingEntity {
        private TestWaterBreather(float x, float y, float z) {
            super(x, y, z);
        }

        @Override
        protected boolean canBreatheUnderwater() {
            return true;
        }
    }
}
