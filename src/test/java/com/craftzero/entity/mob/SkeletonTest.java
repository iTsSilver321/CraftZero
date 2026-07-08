package com.craftzero.entity.mob;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.Entity;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SkeletonTest {
    @Test
    @DisplayName("Skeletons should shoot close targets with the Release 1.0 arrow arc")
    void skeletonShootsCloseTargetsWithReleaseArrowArc() throws Exception {
        World world = new World(6280L);
        try {
            prepareOpenCombatLane(world, 0, 3);
            Player player = new Player(2.5f, 90.0f, 0.5f);
            world.setPlayer(player);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 90.0f, 0.5f);
            world.replaceEntities(List.of(skeleton));

            world.updateEntities(1.0f / 20.0f);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.stream().filter(sound -> WorldSoundEvent.BOW.equals(sound.soundId())).count());
            ArrowEntity arrow = pendingArrow(world);
            assertSame(skeleton, arrow.getShooter());
            assertEquals(CombatRules.EASY_SKELETON_ARROW_DAMAGE, arrow.getDamage(), 0.0001f);
            assertEquals(0, arrow.getFireTicksOnHit());
            assertEquals(0.58719f, arrow.getMotionX(), 0.0001f);
            assertEquals(0.12331f, arrow.getMotionY(), 0.0001f);
            assertEquals(0.0f, arrow.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Burning skeletons should shoot burning arrows")
    void burningSkeletonShootsBurningArrow() throws Exception {
        World world = new World(6282L);
        try {
            prepareOpenCombatLane(world, 0, 3);
            Player player = new Player(2.5f, 90.0f, 0.5f);
            world.setPlayer(player);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 90.0f, 0.5f);
            skeleton.setOnFire(80);
            world.replaceEntities(List.of(skeleton));

            world.updateEntities(1.0f / 20.0f);

            ArrowEntity arrow = pendingArrow(world);
            assertSame(skeleton, arrow.getShooter());
            assertEquals(100, arrow.getFireTicksOnHit());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Skeletons should use the old 30-tick bow cadence")
    void skeletonUsesReleaseBowCadence() {
        World world = new World(6281L);
        try {
            prepareOpenCombatLane(world, 0, 9);
            Player player = new Player(8.5f, 90.0f, 0.5f);
            world.setPlayer(player);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 90.0f, 0.5f);
            world.replaceEntities(List.of(skeleton));

            world.updateEntities(1.0f / 20.0f);
            assertEquals(1, bowSounds(world.drainSoundEvents()));

            for (int i = 0; i < 29; i++) {
                world.updateEntities(1.0f / 20.0f);
            }
            assertEquals(0, bowSounds(world.drainSoundEvents()));

            world.updateEntities(1.0f / 20.0f);
            assertEquals(1, bowSounds(world.drainSoundEvents()));
        } finally {
            world.cleanup();
        }
    }

    private static void prepareOpenCombatLane(World world, int minX, int maxX) {
        for (int x = minX; x <= maxX; x++) {
            world.setBlock(x, 89, 0, BlockType.STONE, 0);
            for (int y = 90; y <= 93; y++) {
                world.setBlock(x, y, 0, BlockType.AIR, 0);
            }
        }
    }

    private static int bowSounds(List<WorldSoundEvent> sounds) {
        return (int) sounds.stream()
                .filter(sound -> WorldSoundEvent.BOW.equals(sound.soundId()))
                .count();
    }

    @SuppressWarnings("unchecked")
    private static ArrowEntity pendingArrow(World world) throws Exception {
        Field field = World.class.getDeclaredField("entitiesToAdd");
        field.setAccessible(true);
        return ((List<Entity>) field.get(world)).stream()
                .filter(ArrowEntity.class::isInstance)
                .map(ArrowEntity.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
