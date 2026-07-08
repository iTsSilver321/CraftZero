package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlimeTest {

    @Test
    @DisplayName("Large slimes split into Release-style half-size children")
    void largeSlimesSplitIntoHalfSizeChildren() throws Exception {
        World world = new World(9131L);
        try {
            Slime slime = new Slime(4);
            slime.random = new FixedRandom(2, 0.25f);
            slime.setPosition(10.0f, 70.0f, 20.0f);
            world.replaceEntities(List.of(slime));

            slime.damage(100.0f, DamageSource.generic());
            world.updateEntities(1.0f / 20.0f);

            List<Slime> children = pendingEntities(world, Slime.class);
            assertEquals(4, children.size());
            assertTrue(children.stream().allMatch(child -> child.getClass() == Slime.class));
            assertTrue(children.stream().allMatch(child -> child.getSize() == 2));
            assertTrue(children.stream().allMatch(child -> Math.abs(child.getY() - 70.5f) < 0.001f));

            Set<String> offsets = children.stream()
                    .map(child -> Math.round((child.getX() - 10.0f) * 10.0f) + ","
                            + Math.round((child.getZ() - 20.0f) * 10.0f))
                    .collect(Collectors.toSet());
            assertEquals(Set.of("-5,-5", "5,-5", "-5,5", "5,5"), offsets);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slimes should emit sized squish sounds when jumping")
    void slimesEmitSizedJumpSounds() throws Exception {
        World world = new World(9132L);
        try {
            Slime slime = new Slime(2);
            slime.random = new FixedRandom(0, 0.5f);
            slime.setPosition(0.5f, 70.0f, 0.5f);
            slime.setJumpDelay(0);
            setOnGround(slime, true);
            world.replaceEntities(List.of(slime));

            slime.tick();

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertSlimeSound(sounds.get(0), WorldSoundEvent.SLIME, 0.8f, 1.25f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slimes should drive Release-style jump and landing squish state")
    void slimesDriveJumpAndLandingSquishState() throws Exception {
        Slime slime = new Slime(2);
        slime.random = new FixedRandom(0, 0.5f);
        slime.setPosition(0.5f, 70.0f, 0.5f);
        slime.setJumpDelay(0);
        setOnGround(slime, true);

        slime.tick();

        assertEquals(0.0f, slime.getRenderSquishAmount(0.0f), 0.0001f);
        assertEquals(1.0f, slime.getRenderSquishAmount(1.0f), 0.0001f);

        setOnGround(slime, false);
        slime.tick();

        assertEquals(1.0f, slime.getRenderSquishAmount(0.0f), 0.0001f);
        assertEquals(0.6f, slime.getRenderSquishAmount(1.0f), 0.0001f);

        slime.onLanded(0.5f);

        assertEquals(-0.5f, slime.getRenderSquishAmount(1.0f), 0.0001f);
    }

    @Test
    @DisplayName("Slimes should ignore fall damage while keeping landing feedback")
    void slimesIgnoreFallDamageOnLanding() throws Exception {
        World world = new World(9135L);
        try {
            Slime slime = new Slime(4);
            slime.random = new FixedRandom(0, 0.5f);
            slime.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(slime));

            slime.onLanded(24.0f);

            assertEquals(slime.getMaxHealth(), slime.getHealth(), 0.001f);
            assertEquals(-0.5f, slime.getRenderSquishAmount(1.0f), 0.0001f);
            assertEquals(32, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SLIME)
                    .count());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertSlimeSound(sounds.get(0), WorldSoundEvent.SLIME, 1.6f, 1.25f);
            assertTrue(pendingEntities(world, Slime.class).isEmpty(),
                    "Fall landings should not kill or split large slimes");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slimes should emit an attack squish only when contact damage lands")
    void slimesEmitAttackSoundOnAcceptedContactDamage() {
        World world = new World(9133L);
        try {
            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.getStats().restore(PlayerStats.MAX_HEALTH, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);
            clearAirColumn(world, 0, 70, 73, 0);

            Slime slime = new Slime(2);
            slime.random = new FixedRandom(0, 0.5f);
            slime.setPosition(0.6f, 70.0f, 0.5f);
            slime.setJumpDelay(100);
            world.replaceEntities(List.of(slime));

            slime.tick();

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertSlimeSound(sounds.get(0), WorldSoundEvent.SLIME_ATTACK, 0.8f, 1.25f);
            assertEquals(PlayerStats.MAX_HEALTH - 2.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slime contact damage should use source-style 3D distance")
    void slimeContactDamageUsesThreeDimensionalDistance() {
        World world = new World(9136L);
        try {
            Player player = new Player(0.5f, 74.0f, 0.5f);
            player.getStats().restore(PlayerStats.MAX_HEALTH, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);

            Slime slime = new Slime(4);
            slime.random = new FixedRandom(0, 0.5f);
            slime.setPosition(0.6f, 70.0f, 0.5f);
            slime.setJumpDelay(100);
            world.replaceEntities(List.of(slime));

            slime.tick();

            assertEquals(PlayerStats.MAX_HEALTH, player.getStats().getHealth(), 0.001f);
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slime contact damage should require line of sight")
    void slimeContactDamageRequiresLineOfSight() {
        World world = new World(9137L);
        try {
            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.getStats().restore(PlayerStats.MAX_HEALTH, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);
            world.setBlock(1, 71, 0, BlockType.STONE, 0);
            world.setBlock(1, 72, 0, BlockType.STONE, 0);

            Slime slime = new Slime(4);
            slime.random = new FixedRandom(0, 0.5f);
            slime.setPosition(2.5f, 70.0f, 0.5f);
            slime.setJumpDelay(100);
            world.replaceEntities(List.of(slime));

            slime.tick();

            assertEquals(PlayerStats.MAX_HEALTH, player.getStats().getHealth(), 0.001f);
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slime deaths should emit one squish after lethal damage")
    void slimesEmitDeathSquishWithoutDuplicateHurtSound() {
        World world = new World(9134L);
        try {
            Slime slime = new Slime(1);
            slime.random = new FixedRandom(0, 0.5f);
            slime.setPosition(1.5f, 70.0f, 1.5f);
            world.replaceEntities(List.of(slime));

            assertTrue(slime.damage(slime.getMaxHealth(), DamageSource.generic()));
            assertTrue(world.drainSoundEvents().isEmpty());

            slime.tick();

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertSlimeSound(sounds.get(0), WorldSoundEvent.SLIME, 0.4f, 1.25f);
        } finally {
            world.cleanup();
        }
    }

    private static void setOnGround(Mob mob, boolean onGround) throws Exception {
        Field field = com.craftzero.entity.Entity.class.getDeclaredField("onGround");
        field.setAccessible(true);
        field.setBoolean(mob, onGround);
    }

    private static void assertSlimeSound(WorldSoundEvent sound, String soundId, float volume, float pitch) {
        assertEquals(soundId, sound.soundId());
        assertEquals(volume, sound.volume(), 0.0001f);
        assertEquals(pitch, sound.pitch(), 0.0001f);
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
