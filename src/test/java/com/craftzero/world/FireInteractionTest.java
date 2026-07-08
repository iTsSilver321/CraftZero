package com.craftzero.world;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.Ghast;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FireInteractionTest {
    @Test
    @DisplayName("Rain should extinguish exposed ordinary fire but not covered fire")
    void rainExtinguishesOnlyExposedOrdinaryFire() {
        World world = new World(9007L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenFireColumn(world, pos[0], pos[1], BlockType.STONE);
            world.setWeatherState("rain");

            world.advanceBlockTicks(30);

            assertSame(BlockType.AIR, world.getBlock(pos[0], 100, pos[1]));

            int coveredX = pos[0] + 8;
            int coveredZ = pos[1];
            prepareOpenFireColumn(world, coveredX, coveredZ, BlockType.NETHERRACK);
            world.setBlock(coveredX, 104, coveredZ, BlockType.STONE, 0);
            world.setBlock(coveredX - 1, 104, coveredZ, BlockType.STONE, 0);
            world.setBlock(coveredX + 1, 104, coveredZ, BlockType.STONE, 0);
            world.setBlock(coveredX, 104, coveredZ - 1, BlockType.STONE, 0);
            world.setBlock(coveredX, 104, coveredZ + 1, BlockType.STONE, 0);

            world.advanceBlockTicks(30);

            assertSame(BlockType.FIRE, world.getBlock(coveredX, 100, coveredZ));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rain should not extinguish netherrack-supported fire")
    void rainDoesNotExtinguishNetherrackFire() {
        World world = new World(9012L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenFireColumn(world, pos[0], pos[1], BlockType.NETHERRACK);
            world.setWeatherState("rain");

            world.advanceBlockTicks(30);

            assertSame(BlockType.FIRE, world.getBlock(pos[0], 100, pos[1]));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire on ordinary solid support should burn out after the source age threshold")
    void fireOnOrdinarySolidSupportBurnsOutAfterAgeThreshold() {
        World world = new World(9013L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 4);

            world.advanceBlockTicks(30);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire should expose Release 1.0 source burn rates")
    void fireUsesReleaseOneBurnRates() {
        assertFireRate(BlockType.OAK_PLANKS, 5, 20);
        assertFireRate(BlockType.FENCE, 5, 20);
        assertFireRate(BlockType.OAK_STAIRS, 5, 20);
        assertFireRate(BlockType.OAK_LOG, 5, 5);
        assertFireRate(BlockType.LEAVES, 30, 60);
        assertFireRate(BlockType.BOOKSHELF, 30, 20);
        assertFireRate(BlockType.TNT, 15, 100);
        assertFireRate(BlockType.TALL_GRASS, 60, 100);
        assertFireRate(BlockType.WHITE_WOOL, 30, 60);
        assertFireRate(BlockType.VINES, 15, 100);

        assertFireRate(BlockType.CHEST, 0, 0);
        assertFireRate(BlockType.TRAPDOOR, 0, 0);
        assertFireRate(BlockType.WOODEN_DOOR, 0, 0);
        assertFireRate(BlockType.SAPLING, 0, 0);
    }

    @Test
    @DisplayName("Immediate fire spread should use source flammability thresholds")
    void immediateFireSpreadUsesSourceFlammabilityThresholds() {
        World leavesWorld = new RandomOverrideWorld(9014L, new SequenceRandom(1, 59, 0, 0));
        try {
            prepareFireNextTo(leavesWorld, BlockType.LEAVES);

            leavesWorld.advanceBlockTicks(30);

            assertSame(BlockType.FIRE, leavesWorld.getBlock(1, 100, 0));
        } finally {
            leavesWorld.cleanup();
        }

        World logWorld = new RandomOverrideWorld(9015L, new SequenceRandom(1, 59));
        try {
            prepareFireNextTo(logWorld, BlockType.OAK_LOG);

            logWorld.advanceBlockTicks(30);

            assertSame(BlockType.OAK_LOG, logWorld.getBlock(1, 100, 0));
        } finally {
            logWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Air fire spread should use the source neighbor-encouragement scan")
    void airFireSpreadUsesSourceNeighborEncouragementScan() {
        World leavesWorld = new RandomOverrideWorld(9017L, new SequenceRandom(1, 2, 0));
        try {
            prepareDiagonalAirSpreadProbe(leavesWorld, BlockType.WHITE_WOOL);

            leavesWorld.advanceBlockTicks(30);

            assertSame(BlockType.FIRE, leavesWorld.getBlock(-1, 100, -1));
        } finally {
            leavesWorld.cleanup();
        }

        World logWorld = new RandomOverrideWorld(9018L, new SequenceRandom(1, 2));
        try {
            prepareDiagonalAirSpreadProbe(logWorld, BlockType.OAK_LOG);

            logWorld.advanceBlockTicks(30);

            assertSame(BlockType.AIR, logWorld.getBlock(-1, 100, -1));
        } finally {
            logWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Fire catching TNT should use the source burn table before priming")
    void fireCatchingTntUsesSourceBurnTable() {
        World world = new RandomOverrideWorld(9016L, new SequenceRandom(1, 99));
        try {
            prepareFireNextTo(world, BlockType.TNT);

            world.advanceBlockTicks(30);
            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof PrimedTntEntity));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire on netherrack should not burn out")
    void fireOnNetherrackDoesNotBurnOut() {
        World world = new World(9001L);
        try {
            world.setBlock(0, 99, 0, BlockType.NETHERRACK, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 15);

            for (int i = 0; i < 120; i++) {
                world.advanceBlockTicks(30);
            }

            assertSame(BlockType.FIRE, world.getBlock(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire catching TNT should prime it instead of replacing it with fire")
    void fireCatchingTntPrimesIt() {
        World world = new World(9002L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 0);
            world.setBlock(1, 100, 0, BlockType.TNT, 0);

            for (int i = 0; i < 120 && world.getBlock(1, 100, 0) == BlockType.TNT; i++) {
                world.advanceBlockTicks(30);
            }
            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof PrimedTntEntity));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living mobs touching fire should be set on fire")
    void mobTouchingFireIgnites() {
        World world = new World(9003L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Living mobs touching lava should burn and take fire damage")
    void mobTouchingLavaBurnsAndTakesDamage() {
        World world = new World(9004L);
        try {
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));
            float beforeHealth = zombie.getHealth();

            world.updateEntities(1.0f / 20.0f);

            assertTrue(zombie.isOnFire());
            assertEquals(beforeHealth - 4.0f, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire Resistance should prevent living mob lava ignition and damage")
    void fireResistancePreventsMobLavaIgnitionAndDamage() {
        World world = new World(9022L);
        try {
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            zombie.addEffect(new StatusEffectInstance(StatusEffectType.FIRE_RESISTANCE, 200, 0));
            world.replaceEntities(java.util.List.of(zombie));
            float beforeHealth = zombie.getHealth();

            world.updateEntities(1.0f / 20.0f);

            assertFalse(zombie.isOnFire());
            assertEquals(beforeHealth, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items touching fire should take item-entity fire damage")
    void droppedItemTouchingFireTakesHealthDamage() {
        World world = new World(9008L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 0);
            DroppedItem item = new DroppedItem(0.5f, 100.5f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(1, world.getDroppedItems().size());
            assertEquals(4, item.getHealth());

            for (int i = 0; i < 4; i++) {
                world.updateDroppedItems(1.0f / 20.0f);
            }

            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items touching lava should take fire damage and fizz upward")
    void droppedItemTouchingLavaFizzesAndBurns() {
        World world = new World(9009L);
        try {
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);
            DroppedItem item = new DroppedItem(0.5f, 100.5f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(1, world.getDroppedItems().size());
            assertEquals(1, item.getHealth());
            assertEquals(4.0f, item.getVelocityY(), 0.001f);
            WorldSoundEvent fizz = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.FIZZ, fizz.soundId());
            assertEquals(0.4f, fizz.volume(), 0.0001f);
            assertTrue(fizz.pitch() >= 2.0f);
            assertTrue(fizz.pitch() < 2.4f);

            world.updateDroppedItems(1.0f / 20.0f);

            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped item lava fizz and bounce should not repeat every tick")
    void droppedItemLavaFizzAndBounceUseSourceCadence() {
        World world = new World(9010L);
        try {
            world.setBlock(0, 100, 0, BlockType.LAVA, 0);
            DroppedItem item = new DroppedItem(0.5f, 100.5f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f);
            item.setHealth(40);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(36, item.getHealth());
            assertEquals(4.0f, item.getVelocityY(), 0.001f);
            List<WorldSoundEvent> firstTickSounds = world.drainSoundEvents();
            assertEquals(1, firstTickSounds.size());
            assertEquals(WorldSoundEvent.FIZZ, firstTickSounds.get(0).soundId());

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(32, item.getHealth());
            assertTrue(item.getVelocityY() < 4.0f);
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped item lava feedback cadence reopens every 25 source ticks")
    void droppedItemLavaFeedbackCadenceReopensEveryTwentyFiveTicks() {
        World world = new World(9011L);
        try {
            world.setBlock(0, 119, 0, BlockType.STONE, 0);
            DroppedItem item = new DroppedItem(0.5f, 120.1f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f);
            item.setOnGround(true);

            assertFalse(item.update(1.0f / 20.0f, world));
            assertTrue(item.shouldApplyLavaFeedback());

            for (int tick = 2; tick < 25; tick++) {
                assertFalse(item.update(1.0f / 20.0f, world));
                assertFalse(item.shouldApplyLavaFeedback(), "tick " + tick);
            }

            assertFalse(item.update(1.0f / 20.0f, world));
            assertTrue(item.shouldApplyLavaFeedback());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether fire mobs should ignore fire damage sources")
    void netherFireMobsIgnoreFireDamage() {
        List<Mob> mobs = List.of(new Blaze(), new Ghast(), new MagmaCube(), new ZombiePigman());

        for (Mob mob : mobs) {
            float beforeHealth = mob.getHealth();

            assertFalse(mob.damage(1.0f, DamageSource.point(DamageSource.Type.FIRE,
                    mob.getX(), mob.getY(), mob.getZ(), 0.0f, 0.0f)), mob.getClass().getSimpleName());
            mob.setOnFire(80);

            assertEquals(beforeHealth, mob.getHealth(), 0.001f, mob.getClass().getSimpleName());
            assertFalse(mob.isOnFire(), mob.getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("Nether fire mobs touching lava should not burn or take fire damage")
    void netherFireMobsIgnoreLavaContactDamage() {
        World world = new World(9006L);
        try {
            Blaze blaze = new Blaze();
            blaze.setPosition(0.5f, 100.0f, 0.5f);
            MagmaCube magmaCube = new MagmaCube();
            magmaCube.setPosition(1.5f, 100.0f, 0.5f);
            Ghast ghast = new Ghast();
            ghast.setPosition(4.5f, 100.0f, 0.5f);
            ZombiePigman pigman = new ZombiePigman();
            pigman.setPosition(8.5f, 100.0f, 0.5f);
            List<Mob> mobs = List.of(blaze, magmaCube, ghast, pigman);
            for (int x = 0; x < mobs.size(); x++) {
                Mob mob = mobs.get(x);
                world.setBlock((int) Math.floor(mob.getX()), 100, 0, BlockType.LAVA, 0);
            }
            world.replaceEntities(mobs);

            world.updateEntities(1.0f / 20.0f);

            for (Mob mob : mobs) {
                assertFalse(mob.isOnFire(), mob.getClass().getSimpleName());
                assertEquals(mob.getMaxHealth(), mob.getHealth(), 0.001f, mob.getClass().getSimpleName());
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players touching fire should take fire contact damage")
    void playerTouchingFireTakesDamage() {
        World world = new World(9005L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 0);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

            player.update(1.0f / 20.0f, world);

            assertEquals(19.0f, player.getStats().getHealth(), 0.001f);
            assertTrue(player.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire Resistance should prevent player fire contact ignition and damage")
    void fireResistancePreventsPlayerFireContactIgnitionAndDamage() {
        World world = new World(9023L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 0, BlockType.FIRE, 0);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.FIRE_RESISTANCE, 200, 0));

            player.update(1.0f / 20.0f, world);

            assertEquals(20.0f, player.getStats().getHealth(), 0.001f);
            assertFalse(player.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players should take periodic damage while burning")
    void playerBurningTicksDealPeriodicDamage() {
        World world = new World(9006L);
        try {
            world.setBlock(2, 99, 0, BlockType.STONE, 0);
            Player player = new Player(2.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            player.setOnFire(40);

            for (int i = 0; i < 20; i++) {
                player.update(1.0f / 20.0f, world);
            }

            assertEquals(19.0f, player.getStats().getHealth(), 0.001f);
            assertEquals(20, player.getFireTicks());
            assertTrue(player.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rain should extinguish exposed burning players")
    void rainExtinguishesExposedBurningPlayers() {
        World world = new World(9014L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            world.getChunkNow(Math.floorDiv(pos[0], Chunk.WIDTH), Math.floorDiv(pos[1], Chunk.DEPTH));
            world.setBlock(pos[0], 99, pos[1], BlockType.STONE, 0);
            for (int y = 100; y < Chunk.HEIGHT; y++) {
                world.setBlock(pos[0], y, pos[1], BlockType.AIR, 0);
            }
            world.setWeatherState("rain");
            Player player = new Player(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            player.setOnFire(40);

            player.update(1.0f / 20.0f, world);

            assertFalse(player.isOnFire());
            assertEquals(20.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    private static int[] findRainBiome(World world) {
        for (int x = -64; x <= 64; x += 8) {
            for (int z = -64; z <= 64; z += 8) {
                BiomeType biome = world.getReleaseBiome(x, z);
                if (!biome.canFreezeWater() && biome.getTemperature() < 1.0f) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No non-frozen rain biome found near origin");
    }

    private static void prepareOpenFireColumn(World world, int x, int z, BlockType support) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, support, 0);
        world.setBlock(x, 100, z, BlockType.FIRE, 0);
        for (int y = 101; y < Chunk.HEIGHT; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    private static void prepareFireNextTo(World world, BlockType target) {
        world.setBlock(0, 99, 0, BlockType.NETHERRACK, 0);
        world.setBlock(0, 100, 0, BlockType.FIRE, 0);
        world.setBlock(1, 100, 0, target, 0);
    }

    private static void prepareDiagonalAirSpreadProbe(World world, BlockType fuel) {
        world.setBlock(0, 99, 0, BlockType.NETHERRACK, 0);
        world.setBlock(0, 100, 0, BlockType.FIRE, 0);
        world.setBlock(-1, 100, -1, BlockType.AIR, 0);
        world.setBlock(-2, 100, -1, fuel, 0);
    }

    private static void assertFireRate(BlockType type, int encouragement, int flammability) {
        assertEquals(encouragement, type.getFireEncouragement(), type.name() + " encouragement");
        assertEquals(flammability, type.getFireFlammability(), type.name() + " flammability");
    }

    private static final class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        private SequenceRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (index >= values.length) {
                return bound - 1;
            }
            int value = values[index++];
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Random value " + value + " outside bound " + bound);
            }
            return value;
        }
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }
}
