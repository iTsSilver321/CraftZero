package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.FireballEntity;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhastTest {

    @Test
    @DisplayName("Ghasts should visibly charge before firing from in front of the body")
    void ghastChargesBeforeLaunchingFrontOffsetFireball() {
        World world = new World(9041L);
        try {
            clearCombatLane(world);
            Player player = new Player(12.5f, 80.5f, 0.5f);
            world.setPlayer(player);

            Ghast ghast = new Ghast();
            ghast.setPosition(0.5f, 79.0f, 0.5f);
            ghast.setFlightState(0, 0, 0, 0.0f, 80.0f, 0.0f);
            world.replaceEntities(List.of(ghast));

            runTicks(world, 10);

            assertEquals(10, ghast.getAttackCharge());
            assertEquals("/textures/mob/ghast.png", ghast.getTexturePath());
            assertEquals(0, countFireballs(world));
            assertTrue(hasSound(world, WorldSoundEvent.GHAST_CHARGE));

            runTicks(world, 9);

            assertEquals(19, ghast.getAttackCharge());
            assertEquals("/textures/mob/ghast_fire.png", ghast.getTexturePath());
            assertEquals(0, countFireballs(world));

            runTicks(world, 2);

            FireballEntity fireball = world.getEntities().stream()
                    .filter(FireballEntity.class::isInstance)
                    .map(FireballEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(fireball.isExplosive());
            assertEquals(0, ghast.getAttackCharge());
            assertTrue(hasSound(world, WorldSoundEvent.GHAST_FIREBALL));
            assertTrue(fireball.getX() > 4.9f && fireball.getX() < 5.2f,
                    "Fireball should start four blocks in front of the Ghast before its first movement tick");
            assertEquals(0.5f, fireball.getZ(), 0.001f);
            assertTrue(fireball.getMotionX() > 0.5f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ghasts should emit Release-style ambient, hurt, and death sounds")
    void ghastsEmitReleaseVocalizations() {
        World ambientWorld = new World(9042L);
        try {
            Ghast ghast = new Ghast();
            ghast.random = new FixedFloatRandom(0, 0.7f, 0.5f);
            ghast.ambientSoundTime = 1000;
            ghast.setPosition(2.5f, 80.0f, 2.5f);
            ambientWorld.spawnEntity(ghast);

            ghast.tickWithoutAi();

            List<WorldSoundEvent> sounds = ambientWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertGhastSound(sounds.get(0), WorldSoundEvent.GHAST_IDLE, 1.04f);
        } finally {
            ambientWorld.cleanup();
        }

        World hurtWorld = new World(9043L);
        try {
            Ghast ghast = new Ghast();
            ghast.random = new FixedFloatRandom(0.7f, 0.5f);
            ghast.setPosition(2.5f, 80.0f, 2.5f);
            hurtWorld.spawnEntity(ghast);

            assertTrue(ghast.damage(1.0f, DamageSource.generic()));

            List<WorldSoundEvent> sounds = hurtWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertGhastSound(sounds.get(0), WorldSoundEvent.GHAST_HURT, 1.04f);
        } finally {
            hurtWorld.cleanup();
        }

        World deathWorld = new World(9044L);
        try {
            Ghast ghast = new Ghast();
            ghast.random = new FixedFloatRandom(0.5f, 0.5f);
            ghast.setPosition(2.5f, 80.0f, 2.5f);
            deathWorld.spawnEntity(ghast);

            assertTrue(ghast.damage(ghast.getMaxHealth(), DamageSource.generic()));
            assertTrue(deathWorld.drainSoundEvents().isEmpty());

            ghast.tick();

            List<WorldSoundEvent> sounds = deathWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertGhastSound(sounds.get(0), WorldSoundEvent.GHAST_DEATH, 1.0f);
        } finally {
            deathWorld.cleanup();
        }
    }

    private static void clearCombatLane(World world) {
        for (int x = -2; x <= 16; x++) {
            for (int y = 78; y <= 84; y++) {
                for (int z = -2; z <= 2; z++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void runTicks(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }

    private static long countFireballs(World world) {
        return world.getEntities().stream()
                .filter(FireballEntity.class::isInstance)
                .count();
    }

    private static boolean hasSound(World world, String soundId) {
        return world.getSoundEvents().stream()
                .map(WorldSoundEvent::soundId)
                .anyMatch(soundId::equals);
    }

    private static void assertGhastSound(WorldSoundEvent sound, String expectedSoundId, float expectedPitch) {
        assertEquals(expectedSoundId, sound.soundId());
        assertEquals(2.5f, sound.x(), 0.0001f);
        assertEquals(82.0f, sound.y(), 0.0001f);
        assertEquals(2.5f, sound.z(), 0.0001f);
        assertEquals(10.0f, sound.volume(), 0.0001f);
        assertEquals(expectedPitch, sound.pitch(), 0.0001f);
    }

    private static final class FixedFloatRandom extends Random {
        private final int intValue;
        private final float[] floats;
        private int index;

        private FixedFloatRandom(float... floats) {
            this(0, floats);
        }

        private FixedFloatRandom(int intValue, float... floats) {
            this.intValue = intValue;
            this.floats = floats;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(intValue, bound);
        }

        @Override
        public float nextFloat() {
            if (floats.length == 0) {
                return 0.5f;
            }
            return floats[index++ % floats.length];
        }
    }
}
