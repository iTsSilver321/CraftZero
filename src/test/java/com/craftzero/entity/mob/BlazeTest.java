package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlazeTest {

    @Test
    @DisplayName("Blazes should take water contact damage")
    void blazesTakeWaterContactDamage() {
        World world = new World(9011L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);
            Blaze blaze = new Blaze();
            blaze.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(blaze));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(blaze.getMaxHealth() - 1.0f, blaze.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blazes should take rain damage only in exposed rain columns")
    void blazesTakeRainDamageWhenExposed() {
        RainColumnWorld world = new RainColumnWorld(9020L);
        try {
            Blaze exposed = new Blaze();
            exposed.setPosition(0.5f, 70.0f, 0.5f);
            Blaze covered = new Blaze();
            covered.setPosition(2.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(exposed, covered));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(exposed.getMaxHealth() - 1.0f, exposed.getHealth(), 0.001f);
            assertEquals(covered.getMaxHealth(), covered.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blazes should charge and space their three-fireball volley")
    void blazeVolleyUsesReleaseChargeAndSpacing() {
        CountingWorld world = new CountingWorld(9015L);
        try {
            clearCombatLane(world);
            Player player = new Player(12.5f, 80.0f, 0.5f);
            world.setPlayer(player);

            Blaze blaze = new Blaze();
            blaze.setPosition(0.5f, 79.0f, 0.5f);
            blaze.setAttackState(0, 0, 0);
            world.replaceEntities(List.of(blaze));

            runTicks(world, 11);

            assertTrue(blaze.isCharged());
            assertEquals(3, blaze.getBurstShots());
            assertEquals(0, world.spawnedFireballs);
            assertEquals(8, particleCount(world, WorldParticle.Type.FLAME));
            assertEquals(4, particleCount(world, WorldParticle.Type.SMOKE));

            runTicks(world, 11);

            assertEquals(1, world.spawnedFireballs);
            assertEquals(2, blaze.getBurstShots());
            assertTrue(blaze.getBurstCooldown() > 0);
            assertEquals(14, particleCount(world, WorldParticle.Type.FLAME));
            assertTrue(particleCount(world, WorldParticle.Type.SMOKE) >= 4);

            runTicks(world, 21);

            assertEquals(2, world.spawnedFireballs);
            assertEquals(1, blaze.getBurstShots());

            runTicks(world, 21);

            assertEquals(3, world.spawnedFireballs);
            assertEquals(0, blaze.getBurstShots());
            assertTrue(blaze.getAttackCooldown() >= 90);
            assertFalse(blaze.isCharged());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blazes should emit Release-style ambient, hurt, and death sounds")
    void blazesEmitReleaseFeedbackSounds() {
        World ambientWorld = new World(9017L);
        try {
            Blaze blaze = new Blaze();
            blaze.random = fixedNextInts(0);
            blaze.ambientSoundTime = 1000;
            blaze.setPosition(2.5f, 70.0f, 2.5f);
            ambientWorld.spawnEntity(blaze);

            blaze.tickWithoutAi();

            List<WorldSoundEvent> sounds = ambientWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.BLAZE_BREATHE, sounds.get(0).soundId());
        } finally {
            ambientWorld.cleanup();
        }

        World hurtWorld = new World(9018L);
        try {
            Blaze blaze = new Blaze();
            blaze.random = fixedNextInts();
            blaze.setPosition(3.5f, 70.0f, 3.5f);
            hurtWorld.spawnEntity(blaze);

            assertTrue(blaze.damage(1.0f, DamageSource.generic()));

            List<WorldSoundEvent> sounds = hurtWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.BLAZE_HURT, sounds.get(0).soundId());
        } finally {
            hurtWorld.cleanup();
        }

        World deathWorld = new World(9019L);
        try {
            Blaze blaze = new Blaze();
            blaze.random = fixedNextInts();
            blaze.setPosition(4.5f, 70.0f, 4.5f);
            deathWorld.spawnEntity(blaze);

            assertTrue(blaze.damage(blaze.getMaxHealth(), DamageSource.generic()));
            assertTrue(deathWorld.drainSoundEvents().isEmpty());

            blaze.tick();

            List<WorldSoundEvent> sounds = deathWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.BLAZE_DEATH, sounds.get(0).soundId());
        } finally {
            deathWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Blazes should emit Release-style ambient large-smoke particles")
    void blazesEmitAmbientLargeSmokeParticles() {
        World world = new World(9021L);
        try {
            Blaze blaze = new Blaze();
            blaze.random = fixedNextInts(999);
            blaze.setPosition(2.5f, 70.0f, 2.5f);
            world.replaceEntities(List.of(blaze));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(2, particleCount(world, WorldParticle.Type.LARGE_SMOKE));
            for (WorldParticle particle : world.getParticles()) {
                if (particle.getType() != WorldParticle.Type.LARGE_SMOKE) {
                    continue;
                }
                assertEquals(2.5f, particle.getRenderX(0.0f), 0.0001f);
                assertEquals(70.0f + blaze.getHeight() * 0.5f, particle.getRenderY(0.0f), 0.0001f);
                assertEquals(2.5f, particle.getRenderZ(0.0f), 0.0001f);
                assertEquals(0.30f, particle.getScale(1.0f), 0.0001f);
                assertEquals(22.0f, particle.getLifetimeTicks(), 0.0001f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blazes should use close-range melee instead of starting a ranged burst")
    void blazeUsesCloseRangeMelee() {
        World world = new World(9016L);
        try {
            clearCombatLane(world);
            Player player = new Player(0.9f, 79.0f, 0.5f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);

            Blaze blaze = new Blaze();
            blaze.setPosition(0.5f, 79.0f, 0.5f);
            blaze.setAttackState(0, 0, 0);
            world.replaceEntities(List.of(blaze));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(16.0f, player.getStats().getHealth(), 0.001f);
            assertEquals(0, fireballCount(world));
            assertEquals(20, blaze.getAttackCooldown());
            assertEquals(0, blaze.getBurstShots());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blaze rods should require recent player-credit damage")
    void blazeRodsRequireRecentPlayerCreditDamage() {
        World world = new World(9012L);
        try {
            Blaze environmental = new Blaze();
            environmental.random = new AlwaysDropRandom();
            environmental.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(environmental));

            environmental.dropLoot();

            assertEquals(0, droppedCount(world, ItemType.BLAZE_ROD));

            Blaze playerDamaged = new Blaze();
            playerDamaged.random = new AlwaysDropRandom();
            playerDamaged.setPosition(1.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(playerDamaged));
            playerDamaged.damage(1.0f, DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    0.0f, 70.0f, 0.0f, 0.0f, 0.0f));

            playerDamaged.dropLoot();

            assertEquals(1, droppedCount(world, ItemType.BLAZE_ROD));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Tamed wolf melee should count as player credit for Blaze rods")
    void tamedWolfMeleeCreditsBlazeRodDrops() {
        World world = new World(9013L);
        try {
            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Blaze blaze = new Blaze();
            blaze.random = new AlwaysDropRandom();
            blaze.setPosition(1.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf, blaze));

            blaze.damage(1.0f, DamageSource.entity(DamageSource.Type.MOB_MELEE, wolf));
            blaze.dropLoot();

            assertEquals(1, droppedCount(world, ItemType.BLAZE_ROD));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blaze rod Looting should use the Release 1.0 single widened roll")
    void blazeRodLootingUsesSingleWidenedRoll() {
        World world = new World(9014L);
        try {
            Blaze blaze = new Blaze();
            blaze.random = fixedNextInts(0, 3);
            blaze.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(blaze));
            blaze.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 3));

            blaze.dropLoot();

            assertEquals(0, droppedCount(world, ItemType.BLAZE_ROD));
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

    private static long fireballCount(World world) {
        return world.getEntities().stream()
                .filter(FireballEntity.class::isInstance)
                .count();
    }

    private static long particleCount(World world, WorldParticle.Type type) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == type)
                .count();
    }

    private static void runTicks(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
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

    private static final class CountingWorld extends World {
        private int spawnedFireballs;

        private CountingWorld(long seed) {
            super(seed);
        }

        @Override
        public FireballEntity spawnFireball(float x, float y, float z,
                float motionX, float motionY, float motionZ, Entity shooter, boolean explosive) {
            spawnedFireballs++;
            return super.spawnFireball(x, y, z, motionX, motionY, motionZ, shooter, explosive);
        }
    }

    private static final class RainColumnWorld extends World {
        private RainColumnWorld(long seed) {
            super(seed);
        }

        @Override
        public boolean isRainingAt(int x, int y, int z) {
            return x == 0;
        }
    }

    private static Random fixedNextInts(int... values) {
        return new Random() {
            private int index;

            @Override
            public int nextInt(int bound) {
                if (index >= values.length) {
                    return 0;
                }
                return Math.floorMod(values[index++], bound);
            }

            @Override
            public float nextFloat() {
                return 0.5f;
            }
        };
    }

    private static final class AlwaysDropRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }
}
