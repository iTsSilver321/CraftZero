package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSoundTest {

    @Test
    @DisplayName("Common Release 1.0 hostile mobs emit hurt sounds on non-lethal damage")
    void commonHostileMobsEmitHurtSounds() {
        assertHurtSound(new Zombie(), WorldSoundEvent.ZOMBIE_HURT);
        assertHurtSound(new Skeleton(), WorldSoundEvent.SKELETON_HURT);
        assertHurtSound(new Creeper(), WorldSoundEvent.CREEPER_HURT);
        assertHurtSound(new Spider(), WorldSoundEvent.SPIDER_HURT);
        assertHurtSound(new CaveSpider(), WorldSoundEvent.SPIDER_HURT);
    }

    @Test
    @DisplayName("Common Release 1.0 hostile mobs emit death sounds without duplicate lethal hurt sounds")
    void commonHostileMobsEmitDeathSounds() {
        assertDeathSound(new Zombie(), WorldSoundEvent.ZOMBIE_DEATH);
        assertDeathSound(new Skeleton(), WorldSoundEvent.SKELETON_DEATH);
        assertDeathSound(new Creeper(), WorldSoundEvent.CREEPER_DEATH);
        assertDeathSound(new Spider(), WorldSoundEvent.SPIDER_DEATH);
        assertDeathSound(new CaveSpider(), WorldSoundEvent.SPIDER_DEATH);
    }

    @Test
    @DisplayName("Common mob deaths should emit bounded explode poof particles")
    void commonMobDeathsEmitExplodeParticles() {
        World world = new World(9077L);
        try {
            Zombie zombie = new Zombie();
            zombie.setPosition(6.5f, 70.0f, 6.5f);
            world.spawnEntity(zombie);

            assertTrue(zombie.damage(zombie.getMaxHealth(), DamageSource.generic()));
            zombie.tick();

            long poofCount = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.EXPLODE)
                    .count();
            assertTrue(poofCount >= 8 && poofCount <= 24);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Common Release 1.0 passive animals emit hurt sounds on non-lethal damage")
    void commonPassiveAnimalsEmitHurtSounds() {
        assertHurtSound(new Cow(), WorldSoundEvent.COW_HURT);
        assertHurtSound(new Mooshroom(), WorldSoundEvent.COW_HURT);
        assertHurtSound(new Pig(), WorldSoundEvent.PIG_HURT);
        assertHurtSound(new Sheep(), WorldSoundEvent.SHEEP_HURT);
        assertHurtSound(new Chicken(), WorldSoundEvent.CHICKEN_HURT);
    }

    @Test
    @DisplayName("Common Release 1.0 passive animals emit death sounds without duplicate lethal hurt sounds")
    void commonPassiveAnimalsEmitDeathSounds() {
        assertDeathSound(new Cow(), WorldSoundEvent.COW_DEATH);
        assertDeathSound(new Mooshroom(), WorldSoundEvent.COW_DEATH);
        assertDeathSound(new Pig(), WorldSoundEvent.PIG_DEATH);
        assertDeathSound(new Sheep(), WorldSoundEvent.SHEEP_DEATH);
        assertDeathSound(new Chicken(), WorldSoundEvent.CHICKEN_DEATH);
    }

    @Test
    @DisplayName("Common Release 1.0 mobs emit timed ambient idle sounds")
    void commonMobsEmitAmbientIdleSounds() {
        assertAmbientSound(new Zombie(), WorldSoundEvent.ZOMBIE_IDLE);
        assertAmbientSound(new Skeleton(), WorldSoundEvent.SKELETON_IDLE);
        assertAmbientSound(new Spider(), WorldSoundEvent.SPIDER_IDLE);
        assertAmbientSound(new CaveSpider(), WorldSoundEvent.SPIDER_IDLE);
        assertAmbientSound(new Cow(), WorldSoundEvent.COW_IDLE);
        assertAmbientSound(new Mooshroom(), WorldSoundEvent.COW_IDLE);
        assertAmbientSound(new Pig(), WorldSoundEvent.PIG_IDLE);
        assertAmbientSound(new Sheep(), WorldSoundEvent.SHEEP_IDLE);
        assertAmbientSound(new Chicken(), WorldSoundEvent.CHICKEN_IDLE);
    }

    @Test
    @DisplayName("Creepers stay silent between fuse, hurt, and death events")
    void creepersDoNotEmitAmbientIdleSounds() {
        World world = new World(9074L);
        try {
            Creeper creeper = new Creeper();
            creeper.random = new FixedFloatRandom(0, 0.7f, 0.5f);
            creeper.ambientSoundTime = 1000;
            creeper.setPosition(8.5f, 70.0f, 8.5f);
            world.spawnEntity(creeper);

            creeper.tickWithoutAi();

            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 wolves emit state-based ambient vocalizations")
    void wolvesEmitStateBasedAmbientSounds() {
        Wolf calm = new Wolf();
        assertWolfAmbientSound(calm, WorldSoundEvent.WOLF_BARK, 1);

        Wolf angry = new Wolf();
        angry.setAngry(true);
        assertWolfAmbientSound(angry, WorldSoundEvent.WOLF_GROWL, 0);

        Wolf healthyTamed = new Wolf();
        healthyTamed.setTamed(true);
        healthyTamed.setHealth(20.0f);
        assertWolfAmbientSound(healthyTamed, WorldSoundEvent.WOLF_PANTING, 0);

        Wolf hurtTamed = new Wolf();
        hurtTamed.setTamed(true);
        hurtTamed.setHealth(6.0f);
        assertWolfAmbientSound(hurtTamed, WorldSoundEvent.WOLF_WHINE, 0);
    }

    @Test
    @DisplayName("Release 1.0 wolves emit hurt and death vocalizations")
    void wolvesEmitHurtAndDeathSounds() {
        assertHurtSound(new Wolf(), WorldSoundEvent.WOLF_HURT);
        assertDeathSound(new Wolf(), WorldSoundEvent.WOLF_DEATH);
    }

    @Test
    @DisplayName("Release 1.0 silverfish emit ambient, hurt, and death sounds")
    void silverfishEmitReleaseSounds() {
        assertAmbientSound(new Silverfish(), WorldSoundEvent.SILVERFISH_IDLE);
        assertHurtSound(new Silverfish(), WorldSoundEvent.SILVERFISH_HURT);
        assertDeathSound(new Silverfish(), WorldSoundEvent.SILVERFISH_DEATH);
    }

    @Test
    @DisplayName("Release 1.0 Zombie Pigmen emit ambient, hurt, and death sounds")
    void zombiePigmenEmitReleaseSounds() {
        assertAmbientSound(new ZombiePigman(), WorldSoundEvent.ZOMBIE_PIGMAN_IDLE);
        assertHurtSound(new ZombiePigman(), WorldSoundEvent.ZOMBIE_PIGMAN_HURT);
        assertDeathSound(new ZombiePigman(), WorldSoundEvent.ZOMBIE_PIGMAN_DEATH);
    }

    @Test
    @DisplayName("Release 1.0 squid remain silent for ambient, hurt, and death events")
    void squidRemainSilentForReleaseOne() {
        World world = new World(9076L);
        try {
            Squid squid = new Squid();
            squid.random = new FixedFloatRandom(0, 0.7f, 0.5f);
            squid.ambientSoundTime = 1000;
            squid.setPosition(10.5f, 70.0f, 10.5f);
            world.spawnEntity(squid);

            squid.tickWithoutAi();
            assertTrue(world.drainSoundEvents().isEmpty());

            assertTrue(squid.damage(1.0f, DamageSource.generic()));
            assertTrue(world.drainSoundEvents().isEmpty());

            assertTrue(squid.damage(squid.getMaxHealth(), DamageSource.generic()));
            squid.tick();
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    private static void assertWolfAmbientSound(Wolf wolf, String expectedSoundId, int vocalRoll) {
        World world = new World(9075L);
        try {
            wolf.random = new FixedFloatRandom(new int[] { 0, vocalRoll }, 0.7f, 0.5f);
            wolf.ambientSoundTime = 1000;
            wolf.setPosition(9.5f, 70.0f, 9.5f);
            world.spawnEntity(wolf);

            wolf.tickWithoutAi();

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertMobSound(sounds.get(0), expectedSoundId, 9.5f, 70.0f + wolf.getHeight() * 0.5f, 9.5f, 1.04f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertAmbientSound(Mob mob, String expectedSoundId) {
        World world = new World(9073L);
        try {
            mob.random = new FixedFloatRandom(0, 0.7f, 0.5f);
            mob.ambientSoundTime = 1000;
            mob.setPosition(7.5f, 70.0f, 7.5f);
            world.spawnEntity(mob);

            mob.tickWithoutAi();

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertMobSound(sounds.get(0), expectedSoundId, 7.5f, 70.0f + mob.getHeight() * 0.5f, 7.5f, 1.04f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertHurtSound(Mob mob, String expectedSoundId) {
        World world = new World(9071L);
        try {
            mob.random = new FixedFloatRandom(0.7f, 0.5f);
            mob.setPosition(4.5f, 70.0f, 4.5f);
            world.spawnEntity(mob);

            assertTrue(mob.damage(1.0f, DamageSource.generic()));

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertMobSound(sounds.get(0), expectedSoundId, 4.5f, 70.0f + mob.getHeight() * 0.5f, 4.5f, 1.04f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertDeathSound(Mob mob, String expectedSoundId) {
        World world = new World(9072L);
        try {
            mob.random = new FixedFloatRandom(0.5f, 0.5f);
            mob.setPosition(6.5f, 70.0f, 6.5f);
            world.spawnEntity(mob);

            assertTrue(mob.damage(mob.getMaxHealth(), DamageSource.generic()));
            assertTrue(world.drainSoundEvents().isEmpty());

            mob.tick();

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertMobSound(sounds.get(0), expectedSoundId, 6.5f, 70.0f + mob.getHeight() * 0.5f, 6.5f, 1.0f);
            assertTrue(mob.isDead());
        } finally {
            world.cleanup();
        }
    }

    private static void assertMobSound(WorldSoundEvent sound, String expectedSoundId,
            float expectedX, float expectedY, float expectedZ, float expectedPitch) {
        assertEquals(expectedSoundId, sound.soundId());
        assertEquals(expectedX, sound.x(), 0.0001f);
        assertEquals(expectedY, sound.y(), 0.0001f);
        assertEquals(expectedZ, sound.z(), 0.0001f);
        assertEquals(1.0f, sound.volume(), 0.0001f);
        assertEquals(expectedPitch, sound.pitch(), 0.0001f);
    }

    private static final class FixedFloatRandom extends Random {
        private final int[] ints;
        private final float[] floats;
        private int intIndex;
        private int index;

        private FixedFloatRandom(float... floats) {
            this(0, floats);
        }

        private FixedFloatRandom(int intValue, float... floats) {
            this(new int[] { intValue }, floats);
        }

        private FixedFloatRandom(int[] ints, float... floats) {
            this.ints = ints;
            this.floats = floats;
        }

        @Override
        public int nextInt(int bound) {
            if (ints.length == 0) {
                return 0;
            }
            return Math.floorMod(ints[intIndex++ % ints.length], bound);
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
