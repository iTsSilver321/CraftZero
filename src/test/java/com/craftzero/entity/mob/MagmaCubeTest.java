package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagmaCubeTest {

    @Test
    @DisplayName("Small magma cubes should damage players unlike small slimes")
    void smallMagmaCubesDamagePlayers() throws Exception {
        World world = new World(9021L);
        try {
            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.getStats().restore(PlayerStats.MAX_HEALTH, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);
            clearAirColumn(world, 0, 70, 73, 0);

            MagmaCube magmaCube = new MagmaCube(1);
            magmaCube.setPosition(0.6f, 70.0f, 0.5f);
            magmaCube.setJumpDelay(1);
            setOnGround(magmaCube, true);
            world.replaceEntities(List.of(magmaCube));

            magmaCube.tick();

            assertEquals(PlayerStats.MAX_HEALTH - 3.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small magma cubes should not hit players outside 3D contact range")
    void smallMagmaCubesUseThreeDimensionalContactRange() {
        World world = new World(9026L);
        try {
            Player player = new Player(0.5f, 72.0f, 0.5f);
            player.getStats().restore(PlayerStats.MAX_HEALTH, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);

            MagmaCube magmaCube = new MagmaCube(1);
            magmaCube.setPosition(0.6f, 70.0f, 0.5f);
            magmaCube.setJumpDelay(100);
            world.replaceEntities(List.of(magmaCube));

            magmaCube.tick();

            assertEquals(PlayerStats.MAX_HEALTH, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Magma cubes should use Release jump height by size")
    void magmaCubesUseSizeScaledJumpHeight() throws Exception {
        World world = new World(9022L);
        try {
            Player player = new Player(20.0f, 70.0f, 20.0f);
            world.setPlayer(player);

            MagmaCube magmaCube = new MagmaCube(4);
            magmaCube.random = new FixedRandom(0, 0.25f);
            magmaCube.setPosition(0.5f, 70.0f, 0.5f);
            magmaCube.setJumpDelay(0);
            setOnGround(magmaCube, true);
            world.replaceEntities(List.of(magmaCube));

            magmaCube.tick();

            assertEquals(0.82f, magmaCube.getMotionY(), 0.001f);
            assertEquals(40, magmaCube.getJumpDelay());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.MAGMA_CUBE_JUMP, sounds.get(0).soundId());
            assertEquals(1.6f, sounds.get(0).volume(), 0.0001f);
            assertEquals(1.25f, sounds.get(0).pitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Magma cubes should inherit slime fall-damage immunity")
    void magmaCubesIgnoreFallDamageOnLanding() {
        World world = new World(9025L);
        try {
            MagmaCube magmaCube = new MagmaCube(4);
            magmaCube.random = new FixedRandom(0, 0.5f);
            magmaCube.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(magmaCube));

            magmaCube.onLanded(24.0f);

            assertEquals(magmaCube.getMaxHealth(), magmaCube.getHealth(), 0.001f);
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.MAGMA_CUBE_JUMP, sounds.get(0).soundId());
            assertEquals(1.6f, sounds.get(0).volume(), 0.0001f);
            assertEquals(1.25f, sounds.get(0).pitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Magma cubes should not drop magma cream in Java Release 1.0")
    void magmaCubesDoNotDropMagmaCreamInRelease10() {
        World world = new World(9023L);
        try {
            MagmaCube tiny = new MagmaCube(1);
            tiny.random = new FixedRandom(3, 0.5f);
            tiny.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(tiny));

            tiny.dropLoot();

            assertEquals(0, droppedCount(world, ItemType.MAGMA_CREAM));

            MagmaCube large = new MagmaCube(4);
            large.random = new FixedRandom(3, 0.5f);
            large.setPosition(1.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(large));

            large.dropLoot();

            assertEquals(0, droppedCount(world, ItemType.MAGMA_CREAM));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Large magma cubes split into smaller magma cubes")
    void largeMagmaCubesSplitIntoSmallerMagmaCubes() throws Exception {
        World world = new World(9024L);
        try {
            MagmaCube magmaCube = new MagmaCube(2);
            magmaCube.random = new FixedRandom(0, 0.75f);
            magmaCube.setPosition(4.0f, 65.0f, 8.0f);
            world.replaceEntities(List.of(magmaCube));

            magmaCube.damage(100.0f, DamageSource.generic());
            world.updateEntities(1.0f / 20.0f);

            List<MagmaCube> children = pendingEntities(world, MagmaCube.class);
            assertEquals(2, children.size());
            assertTrue(children.stream().allMatch(child -> child.getSize() == 1));
            assertTrue(children.stream().allMatch(child -> Math.abs(child.getY() - 65.5f) < 0.001f));
            assertTrue(children.stream().allMatch(child -> Math.abs(child.getYaw() - 270.0f) < 0.001f));
        } finally {
            world.cleanup();
        }
    }

    private static void setOnGround(Mob mob, boolean onGround) throws Exception {
        Field field = com.craftzero.entity.Entity.class.getDeclaredField("onGround");
        field.setAccessible(true);
        field.setBoolean(mob, onGround);
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static void clearAirColumn(World world, int x, int minY, int maxY, int z) {
        for (int y = minY; y <= maxY; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> List<T> pendingEntities(World world, Class<T> type) throws Exception {
        Field field = World.class.getDeclaredField("entitiesToAdd");
        field.setAccessible(true);
        return ((List<Entity>) field.get(world)).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private static final class FixedRandom extends Random {
        private final int intValue;
        private final float floatValue;

        private FixedRandom(int intValue, float floatValue) {
            this.intValue = intValue;
            this.floatValue = floatValue;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(intValue, bound - 1);
        }

        @Override
        public float nextFloat() {
            return floatValue;
        }
    }
}
