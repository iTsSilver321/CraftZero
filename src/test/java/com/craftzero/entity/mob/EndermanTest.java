package com.craftzero.entity.mob;

import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EndermanTest {
    @Test
    @DisplayName("Exposed daytime Endermen should teleport away and stop aggression")
    void exposedDaytimeEndermanTeleportsAndClearsAnger() {
        World world = new World(7301L);
        try {
            setTime(world, 6000.0f);
            prepareColumn(world, 0, 0);
            prepareColumn(world, 8, 0);

            Enderman enderman = new Enderman();
            enderman.random = new SequenceRandom(
                    new int[] { 32 },
                    new float[] { 0.0f, 0.625f, 0.5f });
            enderman.setPosition(0.5f, 100.0f, 0.5f);
            enderman.setAngry(true);
            world.replaceEntities(List.of(enderman));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(8.5f, enderman.getX(), 0.001f);
            assertEquals(100.0f, enderman.getY(), 0.001f);
            assertEquals(0.5f, enderman.getZ(), 0.001f);
            assertFalse(enderman.isAngry());
            assertEquals(130, portalParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Covered daytime Endermen should not use the daylight escape path")
    void coveredDaytimeEndermanDoesNotTeleport() {
        World world = new World(7302L);
        try {
            setTime(world, 6000.0f);
            prepareColumn(world, 0, 0);
            world.setBlock(0, 104, 0, BlockType.STONE, 0);

            Enderman enderman = new Enderman();
            enderman.random = new SequenceRandom(new int[] {}, new float[] { 0.0f });
            enderman.setPosition(0.5f, 100.0f, 0.5f);
            enderman.setAngry(true);
            world.replaceEntities(List.of(enderman));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.5f, enderman.getX(), 0.001f);
            assertTrue(enderman.isAngry());
            assertEquals(2, portalParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nighttime Endermen should not run the daylight escape roll")
    void nighttimeEndermanDoesNotTeleportFromDaylightRule() {
        World world = new World(7303L);
        try {
            setTime(world, 18000.0f);
            prepareColumn(world, 0, 0);
            prepareColumn(world, 8, 0);

            Enderman enderman = new Enderman();
            enderman.random = new SequenceRandom(
                    new int[] { 32 },
                    new float[] { 0.0f, 0.625f, 0.5f });
            enderman.setPosition(0.5f, 100.0f, 0.5f);
            enderman.setAngry(true);
            world.replaceEntities(List.of(enderman));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.5f, enderman.getX(), 0.001f);
            assertTrue(enderman.isAngry());
            assertEquals(2, portalParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Endermen should emit the old per-tick ambient portal shimmer")
    void endermanEmitsAmbientPortalParticlesWhileIdle() {
        World world = new World(7306L);
        try {
            setTime(world, 18000.0f);

            Enderman enderman = new Enderman();
            enderman.random = new SequenceRandom(new int[] { 1 }, new float[] {});
            enderman.setPosition(4.5f, 70.0f, 4.5f);
            world.replaceEntities(List.of(enderman));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(2, portalParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stared-at close Endermen should teleport instead of landing melee hits")
    void staredAtCloseEndermanTeleportsInsteadOfMeleeing() {
        World world = new World(7307L);
        try {
            setTime(world, 18000.0f);
            prepareStandingSpot(world, 0, 70, 0);
            prepareStandingSpot(world, 0, 70, -1);
            prepareStandingSpot(world, 8, 70, -1);

            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.setWorld(world);
            player.getStats().update(5.1f, false, false, player.getDifficulty());
            world.setPlayer(player);

            Enderman enderman = new Enderman();
            enderman.random = new SequenceRandom(new int[] { 32 }, new float[] { 0.625f, 0.5f });
            enderman.setPosition(0.5f, 70.0f, -0.5f);
            enderman.setAngry(true);
            player.getCamera().setLookTarget(enderman.getX(), enderman.getY() + enderman.getHeight() * 0.5f,
                    enderman.getZ());
            world.replaceEntities(List.of(enderman));

            MeleeAttackGoal melee = enderman.getAI().getGoal(MeleeAttackGoal.class);
            assertNotNull(melee);
            enderman.getAI().setMoveTarget(player.getPosition().x, player.getPosition().z);
            melee.start();
            melee.tick();

            assertEquals(PlayerStats.MAX_HEALTH, player.getStats().getHealth(), 0.001f);
            assertEquals(8.5f, enderman.getX(), 0.001f);
            assertEquals(70.0f, enderman.getY(), 0.001f);
            assertEquals(-0.5f, enderman.getZ(), 0.001f);
            assertEquals(0, enderman.getTeleportCooldown());
            assertEquals(128, portalParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Close Endermen should still melee players who look away")
    void closeEndermanMeleesWhenPlayerIsNotStaring() {
        World world = new World(7308L);
        try {
            setTime(world, 18000.0f);
            prepareStandingSpot(world, 0, 70, 0);
            prepareStandingSpot(world, 0, 70, -1);

            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.setWorld(world);
            player.getCamera().setYaw(180.0f);
            player.getStats().update(5.1f, false, false, player.getDifficulty());
            world.setPlayer(player);

            Enderman enderman = new Enderman();
            enderman.setPosition(0.5f, 70.0f, -0.5f);
            enderman.setAngry(true);
            world.replaceEntities(List.of(enderman));

            MeleeAttackGoal melee = enderman.getAI().getGoal(MeleeAttackGoal.class);
            assertNotNull(melee);
            enderman.getAI().setMoveTarget(player.getPosition().x, player.getPosition().z);
            melee.start();
            melee.tick();

            assertEquals(PlayerStats.MAX_HEALTH - CombatRules.EASY_ENDERMAN_DAMAGE,
                    player.getStats().getHealth(), 0.001f);
            assertEquals(0.5f, enderman.getX(), 0.001f);
            assertEquals(-0.5f, enderman.getZ(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Endermen should dodge thrown snowballs without accepting player-credit hits")
    void endermanDodgesThrownSnowballWithoutPlayerCreditHit() {
        World world = new World(7309L);
        try {
            setTime(world, 18000.0f);
            prepareProjectileLane(world, 0, 12, 70, 0);
            prepareStandingSpot(world, 3, 70, 0);
            prepareStandingSpot(world, 11, 70, 0);

            Enderman enderman = new Enderman();
            enderman.random = new SequenceRandom(new int[] { 32 }, new float[] { 0.625f, 0.5f });
            enderman.setPosition(3.5f, 70.0f, 0.5f);
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 70.9f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, true);
            world.replaceEntities(List.of(snowball, enderman));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(snowball.isRemoved());
            assertEquals(11.5f, enderman.getX(), 0.001f);
            assertEquals(70.0f, enderman.getY(), 0.001f);
            assertEquals(0.5f, enderman.getZ(), 0.001f);
            assertEquals(enderman.getMaxHealth(), enderman.getHealth(), 0.001f);
            assertEquals(0, enderman.getHurtTime());
            assertFalse(enderman.hasRecentPlayerDamage());
            assertNull(enderman.getLastDamageSource());
            assertTrue(portalParticleCount(world) >= 128);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Endermen should skip wet, cramped, and cactus-supported teleport spots")
    void endermanTeleportSkipsUnsafeDestinations() {
        ColumnRainWorld world = new ColumnRainWorld(7310L, 11);
        try {
            setTime(world, 18000.0f);
            prepareProjectileLane(world, 0, 24, 70, 0);
            world.setBlock(15, 69, 0, BlockType.CACTUS, 0);
            world.setBlock(19, 72, 0, BlockType.STONE, 0);

            Enderman enderman = new Enderman();
            SequenceRandom random = new SequenceRandom(
                    new int[] { 32, 32, 32, 32 },
                    new float[] {
                            0.625f, 0.5f,
                            0.6875f, 0.5f,
                            0.75f, 0.5f,
                            0.8125f, 0.5f
                    });
            enderman.random = random;
            enderman.setPosition(3.5f, 70.0f, 0.5f);
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 70.9f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, true);
            world.replaceEntities(List.of(snowball, enderman));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(snowball.isRemoved());
            assertEquals(23.5f, enderman.getX(), 0.001f);
            assertEquals(70.0f, enderman.getY(), 0.001f);
            assertEquals(0.5f, enderman.getZ(), 0.001f);
            assertTrue(random.bounds().size() >= 4);
            assertEquals(enderman.getMaxHealth(), enderman.getHealth(), 0.001f);
            assertEquals(0, enderman.getHurtTime());
            assertFalse(enderman.hasRecentPlayerDamage());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Endermen should pick up carryable blocks with the Release-era 20-tick roll")
    void endermanPicksUpCarryableBlockAtSourceCadence() {
        World world = new World(7304L);
        try {
            setTime(world, 18000.0f);
            world.setBlock(10, 70, 10, BlockType.DIRT, 3);

            Enderman enderman = new Enderman();
            SequenceRandom random = new SequenceRandom(
                    new int[] { 0 },
                    new float[] { 0.5f, 0.0f, 0.5f });
            enderman.random = random;
            enderman.setPosition(10.5f, 70.0f, 10.5f);
            world.replaceEntities(List.of(enderman));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(20, random.bounds().get(0));
            assertSame(BlockType.DIRT, enderman.getCarriedBlock());
            assertEquals(3, enderman.getCarriedMetadata());
            assertSame(BlockType.AIR, world.getBlock(10, 70, 10));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Endermen should place carried blocks into valid empty supported cells")
    void endermanPlacesCarriedBlockWhenTargetCanSupportIt() {
        World world = new World(7305L);
        try {
            setTime(world, 18000.0f);
            world.setBlock(10, 70, 10, BlockType.STONE, 0);
            world.setBlock(10, 71, 10, BlockType.AIR, 0);

            Enderman enderman = new Enderman();
            SequenceRandom random = new SequenceRandom(
                    new int[] { 0 },
                    new float[] { 0.25f, 0.5f, 0.25f });
            enderman.random = random;
            enderman.setPosition(10.5f, 70.0f, 10.5f);
            enderman.setCarriedBlock(BlockType.DIRT, 2);
            world.replaceEntities(List.of(enderman));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(2000, random.bounds().get(0));
            assertSame(BlockType.DIRT, world.getBlock(10, 71, 10));
            assertEquals(2, world.getBlockMetadata(10, 71, 10));
            assertSame(BlockType.AIR, enderman.getCarriedBlock());
            assertEquals(0, enderman.getCarriedMetadata());
        } finally {
            world.cleanup();
        }
    }

    private static void setTime(World world, float time) {
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(time);
        world.setDayCycleManager(dayCycle);
    }

    private static void prepareColumn(World world, int x, int z) {
        world.getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        world.setBlock(x, 99, z, BlockType.STONE, 0);
        for (int y = 100; y < 128; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    private static void prepareProjectileLane(World world, int minX, int maxX, int y, int z) {
        for (int x = minX; x <= maxX; x++) {
            prepareStandingSpot(world, x, y, z);
        }
    }

    private static void prepareStandingSpot(World world, int x, int y, int z) {
        world.getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        world.setBlock(x, y - 1, z, BlockType.STONE, 0);
        for (int airY = y; airY <= y + 3; airY++) {
            world.setBlock(x, airY, z, BlockType.AIR, 0);
        }
    }

    private static long portalParticleCount(World world) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == WorldParticle.Type.PORTAL)
                .count();
    }

    private static final class ColumnRainWorld extends World {
        private final int rainyX;

        private ColumnRainWorld(long seed, int rainyX) {
            super(seed);
            this.rainyX = rainyX;
        }

        @Override
        public boolean isRainingAt(int x, int y, int z) {
            return x == rainyX;
        }
    }

    private static final class SequenceRandom extends Random {
        private final int[] ints;
        private final float[] floats;
        private final List<Integer> bounds = new ArrayList<>();
        private int intIndex;
        private int floatIndex;

        private SequenceRandom(int[] ints, float[] floats) {
            this.ints = ints;
            this.floats = floats;
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            if (ints.length == 0 || intIndex >= ints.length) {
                return Math.min(1, bound - 1);
            }
            int value = ints[intIndex++];
            return Math.max(0, Math.min(value, bound - 1));
        }

        @Override
        public float nextFloat() {
            if (floats.length == 0 || floatIndex >= floats.length) {
                return 0.5f;
            }
            return floats[floatIndex++];
        }

        private List<Integer> bounds() {
            return bounds;
        }
    }
}
