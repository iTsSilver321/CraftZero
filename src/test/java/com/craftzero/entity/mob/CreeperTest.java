package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreeperTest {
    @Test
    @DisplayName("Creepers should emit the Release-style fuse sound on first ignition")
    void creeperFirstFuseTickEmitsSound() {
        World world = new World(6273L);
        try {
            Creeper creeper = new Creeper();
            creeper.setPosition(0.5f, 70.0f, 0.5f);
            world.spawnEntity(creeper);

            assertEquals(1, creeper.advanceFuse());

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.CREEPER_FUSE, sounds.get(0).soundId());
            assertEquals(1.0f, sounds.get(0).volume(), 0.0001f);
            assertEquals(0.5f, sounds.get(0).pitch(), 0.0001f);

            assertEquals(2, creeper.advanceFuse());
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Skeleton-killed creepers should drop old Release 1.0 records")
    void skeletonKilledCreepersDropOldRecords() {
        World world = new World(6274L);
        try {
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 70.0f, 0.5f);
            Creeper creeper = new Creeper();
            creeper.random = new SequenceRandom(0, 1);
            creeper.setPosition(1.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(skeleton, creeper));

            ArrowEntity arrow = new ArrowEntity(0.5f, 71.0f, 0.5f,
                    1.0f, 0.0f, 0.0f, skeleton, false, 20.0f);
            creeper.damage(20.0f, DamageSource.entity(DamageSource.Type.ARROW, arrow));
            creeper.tick();

            assertEquals(1, droppedCount(world, ItemType.RECORD_CAT));
            assertEquals(0, droppedCount(world, ItemType.RECORD_11));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Non-skeleton-killed creepers should not drop records")
    void nonSkeletonKilledCreepersDoNotDropRecords() {
        World world = new World(6275L);
        try {
            Creeper creeper = new Creeper();
            creeper.random = new SequenceRandom(0, 1);
            creeper.setPosition(1.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(creeper));

            creeper.damage(20.0f, DamageSource.generic());
            creeper.tick();

            assertEquals(0, droppedCount(world, ItemType.RECORD_13));
            assertEquals(0, droppedCount(world, ItemType.RECORD_CAT));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered creepers should use the old doubled explosion strength")
    void poweredCreepersUseDoubleExplosionStrength() {
        Creeper creeper = new Creeper();

        assertFalse(creeper.isPowered());
        assertEquals(3.0f, creeper.getExplosionPower(), 0.0001f);

        creeper.setPowered(true);

        assertTrue(creeper.isPowered());
        assertEquals(6.0f, creeper.getExplosionPower(), 0.0001f);
    }

    @Test
    @DisplayName("Creeper fuse should honor assigned living targets before the local player")
    void creeperFuseUsesAssignedLivingTargetBeforePlayer() {
        World world = new World(6276L);
        try {
            makeFloor(world, -2, 34, -2, 2, 69);
            world.setPlayer(new Player(32.5f, 70.0f, 0.5f));

            Creeper creeper = new Creeper();
            creeper.setPosition(0.5f, 70.0f, 0.5f);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(creeper, sheep));

            creeper.getAI().setTarget(sheep);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(creeper.isIgnited());
            assertEquals(1, creeper.getFuseTime());

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.CREEPER_FUSE, sounds.get(0).soundId());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Creeper fuse should cool and clear stale assigned targets")
    void creeperDropsInvalidAssignedTargetAndCoolsFuse() {
        World world = new World(6277L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Creeper creeper = new Creeper();
            creeper.setPosition(0.5f, 70.0f, 0.5f);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(creeper, sheep));

            creeper.setFuseState(2, true);
            creeper.getAI().setTarget(sheep);
            creeper.getAI().setMoveTarget(sheep.getX(), sheep.getZ());
            sheep.remove();
            world.updateEntities(1.0f / 20.0f);

            assertEquals(1, creeper.getFuseTime());
            assertNull(creeper.getAI().getTarget());
            assertFalse(creeper.getAI().hasMoveTarget());
        } finally {
            world.cleanup();
        }
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static void makeFloor(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, y, z, BlockType.STONE, 0);
            }
        }
    }

    private static final class SequenceRandom extends Random {
        private final int[] ints;
        private int index;

        private SequenceRandom(int... ints) {
            this.ints = ints;
        }

        @Override
        public int nextInt(int bound) {
            int value = index < ints.length ? ints[index++] : 0;
            return Math.floorMod(value, bound);
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }
}
