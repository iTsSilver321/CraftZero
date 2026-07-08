package com.craftzero.world;

import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BedInteractionTest {
    @Test
    @DisplayName("Beds should normalize head clicks and only allow sleep at night")
    void bedUseNormalizesHeadAndRequiresNight() {
        World world = new World(80L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            world.setDayCycleManager(dayCycle);
            placeSupportedBed(world, 0, 100, 0, 0);

            dayCycle.setTime(6000);
            World.BedUseResult daytime = world.useBed(0, 100, -1);
            assertSame(World.BedUseOutcome.NOT_NIGHT, daytime.outcome());
            assertEquals(new BlockPos(0, 100, 0), daytime.footPos());
            assertEquals(new BlockPos(0, 100, -1), daytime.headPos());

            dayCycle.setTime(18000);
            World.BedUseResult night = world.useBed(0, 100, -1);
            assertTrue(night.sleepAllowed());
            assertEquals(new BlockPos(0, 100, 0), night.footPos());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Beds without a day cycle should not accidentally allow sleep")
    void bedUseWithoutDayCycleRejectsSleepAsNotNight() {
        World world = new World(801L);
        try {
            placeSupportedBed(world, 0, 100, 0, 0);

            World.BedUseResult result = world.useBed(0, 100, 0);

            assertSame(World.BedUseOutcome.NOT_NIGHT, result.outcome());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(0, world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Successful bed use should store occupied metadata on the head half only")
    void successfulBedUseStoresOccupiedMetadataOnHeadOnly() {
        World world = new World(802L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);

            World.BedUseResult sleeping = world.useBed(0, 100, -1);

            assertTrue(sleeping.sleepAllowed());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(World.BED_OCCUPIED_BIT,
                    world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
            assertSame(World.BedUseOutcome.OCCUPIED, world.useBed(0, 100, 0).outcome());

            assertTrue(world.setBedOccupied(0, 100, -1, false));

            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(0, world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Completed bed sleep should wake to morning, clear weather, and clear occupied metadata")
    void completingBedSleepAdvancesMorningAndClearsWeather() {
        World world = new World(803L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(18000);
            world.setDayCycleManager(dayCycle);
            world.setWeatherState("thunder");
            placeSupportedBed(world, 0, 100, 0, 0);

            World.BedUseResult sleeping = world.useBed(0, 100, -1);

            assertTrue(world.completeBedSleep(sleeping));
            assertEquals(0.0f, dayCycle.getTime(), 0.001f);
            assertEquals("clear", world.getWeatherState());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(0, world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed occupied metadata changes should rebuild both bed halves")
    void bedOccupiedMetadataChangesRebuildBothHalves() {
        RecordingWorld world = new RecordingWorld(8031L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);

            World.BedUseResult sleeping = world.useBed(0, 100, -1);

            assertTrue(sleeping.sleepAllowed());
            assertEquals(2, world.rebuildCount);
            assertTrue(world.rebuilt(0, 100, 0));
            assertTrue(world.rebuilt(0, 100, -1));

            assertTrue(world.completeBedSleep(sleeping));

            assertEquals(4, world.rebuildCount);
            assertTrue(world.rebuilt(0, 100, 0));
            assertTrue(world.rebuilt(0, 100, -1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rejected bed use should not complete the sleep transition")
    void rejectedBedUseDoesNotCompleteSleepTransition() {
        World world = new World(804L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000);
            world.setDayCycleManager(dayCycle);
            world.setWeatherState("rain");
            placeSupportedBed(world, 0, 100, 0, 0);

            World.BedUseResult rejected = world.useBed(0, 100, -1);

            assertFalse(world.completeBedSleep(rejected));
            assertEquals(6000.0f, dayCycle.getTime(), 0.001f);
            assertEquals("rain", world.getWeatherState());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(0, world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sleep-preventing monsters near a bed should block sleep")
    void monstersNearBedBlockSleep() {
        World world = new World(81L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);
            Mob zombie = MobFactory.create(MobDefinition.ZOMBIE);
            assertNotNull(zombie);
            zombie.setPosition(4.5f, 100.0f, 0.5f);
            world.spawnEntity(zombie);

            World.BedUseResult result = world.useBed(0, 100, 0);
            assertSame(World.BedUseOutcome.MONSTERS_NEARBY, result.outcome());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed monster checks should use the old head-centered 8x5x8 bounds")
    void monsterJustOutsideHeadCenteredSleepBoundsDoesNotBlockSleep() {
        World world = new World(810L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);
            Mob zombie = MobFactory.create(MobDefinition.ZOMBIE);
            assertNotNull(zombie);
            zombie.setPosition(8.5f, 100.0f, -1.0f);
            world.spawnEntity(zombie);

            World.BedUseResult result = world.useBed(0, 100, 0);

            assertTrue(result.sleepAllowed());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Passive mobs and excluded hostile mobs should not block sleep")
    void passiveAndExcludedMobsDoNotBlockSleep() {
        World world = new World(82L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);
            Mob pig = MobFactory.create(MobDefinition.PIG);
            Mob slime = MobFactory.create(MobDefinition.SLIME);
            assertNotNull(pig);
            assertNotNull(slime);
            pig.setPosition(2.5f, 100.0f, 0.5f);
            slime.setPosition(3.5f, 100.0f, 0.5f);
            world.spawnEntity(pig);
            world.spawnEntity(slime);

            assertTrue(world.useBed(0, 100, 0).sleepAllowed());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Occupied bed metadata should reject sleep")
    void occupiedBedsRejectSleep() {
        World world = new World(83L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);
            world.setBlock(0, 100, -1, BlockType.BED, 8 | World.BED_OCCUPIED_BIT);

            assertSame(World.BedUseOutcome.OCCUPIED, world.useBed(0, 100, 0).outcome());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stale foot-half occupied metadata should be ignored and cleared on successful use")
    void staleFootOccupiedBitDoesNotBlockSleep() {
        World world = new World(830L);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);
            world.setBlock(0, 100, 0, BlockType.BED, World.BED_OCCUPIED_BIT);

            World.BedUseResult result = world.useBed(0, 100, 0);

            assertTrue(result.sleepAllowed());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(World.BED_OCCUPIED_BIT,
                    world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Beds should explode when used outside the Overworld")
    void bedsExplodeOutsideOverworld() {
        World world = new World(84L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            setNight(world);
            placeSupportedBed(world, 0, 100, 0, 0);

            World.BedUseResult result = world.useBed(0, 100, 0);
            assertSame(World.BedUseOutcome.EXPLODED, result.outcome());
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 100, -1));
            WorldParticle explosionParticle = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.HUGE_EXPLOSION)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.5f, explosionParticle.getRenderX(0.0f), 0.0001f);
            assertEquals(100.5f, explosionParticle.getRenderY(0.0f), 0.0001f);
            assertEquals(-0.5f, explosionParticle.getRenderZ(0.0f), 0.0001f);
            assertEquals(2.5f, explosionParticle.getScale(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed explosion fire should come from the source-shaped affected explosion set")
    void bedExplosionFireUsesExplosionAffectedSet() {
        World actual = new World(8401L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            prepareBedExplosionFireFixture(actual);
            World.BedUseResult result = actual.useBed(0, 100, 0);

            assertSame(World.BedUseOutcome.EXPLODED, result.outcome());
            List<BlockPos> fires = collectFireBlocks(actual, -8, 8, 96, 106, -8, 8);
            assertFalse(fires.isEmpty(), "Fixture should leave surviving supports for flaming bed fire");
            for (int z = -2; z <= 2; z++) {
                assertNotSame(BlockType.FIRE, actual.getBlock(3, 100, z),
                        "Obsidian-shadowed shelf must not be lit by a fixed cube scatter");
            }
        } finally {
            actual.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion fire placement should only ignite affected ray positions")
    void explosionFirePlacementOnlyIgnitesAffectedPositions() {
        World world = new World(8402L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            prepareExplosionFireSupportFloor(world);
            Set<BlockPos> affected = new HashSet<>();
            for (int z = -6; z <= -2; z++) {
                for (int x = -6; x <= 6; x++) {
                    affected.add(new BlockPos(x, 100, z));
                }
            }

            world.igniteExplosionFires(affected);

            List<BlockPos> fires = collectFireBlocks(world, -8, 8, 96, 106, -8, 8);
            assertFalse(fires.isEmpty(), "Seeded fixture should ignite at least one affected support");
            for (BlockPos fire : fires) {
                assertTrue(affected.contains(fire), () -> "Fire outside affected explosion set at " + fire);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed placement and survival should require opaque support under both halves")
    void bedPlacementAndSurvivalRequireOpaqueSupportUnderBothHalves() {
        World world = new World(85L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 99, -1, BlockType.GLASS);

            assertNull(world.placeBed(0, 100, 0, 0, null));
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 100, -1));

            world.setBlock(0, 99, -1, BlockType.STONE);
            assertNotNull(world.placeBed(0, 100, 0, 0, null));

            world.setBlock(0, 99, -1, BlockType.GLASS);
            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 100, -1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Orphaned bed heads should not drop items or clear predicted foot blocks")
    void orphanedBedHeadBreakDoesNotDropOrDeletePredictedFoot() {
        World world = new World(8501L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE);
            world.setBlock(0, 100, -1, BlockType.BED, 8);

            assertTrue(world.breakBlock(0, 100, -1, true));

            assertSame(BlockType.STONE, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 100, -1));
            assertTrue(world.getDroppedItems().stream()
                    .noneMatch(item -> item.getItemType() == ItemType.BED));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Malformed bed feet should not delete unrelated bed halves at the predicted head")
    void malformedBedFootBreakDoesNotDeleteUnrelatedBedAtPredictedHead() {
        World world = new World(8502L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 99, -1, BlockType.STONE);
            world.setBlock(1, 99, -1, BlockType.STONE);
            assertNotNull(world.placeBed(0, 100, -1, 1, null));
            world.setBlock(0, 100, 0, BlockType.BED, 0);

            assertTrue(world.breakBlock(0, 100, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.BED, world.getBlock(0, 100, -1));
            assertSame(BlockType.BED, world.getBlock(1, 100, -1));
            assertEquals(1, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.BED)
                    .mapToInt(item -> item.getCount())
                    .sum());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed respawn should choose a clear adjacent standing position")
    void bedRespawnUsesAdjacentStandingPosition() {
        World world = new World(86L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);

            BlockPos respawn = world.findBedRespawnPosition(0, 100, -1);

            assertEquals(new BlockPos(-1, 100, -1), respawn);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed respawn should fail when adjacent standing spaces are blocked")
    void bedRespawnRejectsBlockedAdjacentSpaces() {
        World world = new World(87L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            for (int z = -2; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    if ((x == 0 && z == 0) || (x == 0 && z == -1)) {
                        continue;
                    }
                    world.setBlock(x, 100, z, BlockType.STONE);
                }
            }

            assertNull(world.findBedRespawnPosition(0, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    private static void setNight(World world) {
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(18000);
        world.setDayCycleManager(dayCycle);
    }

    private static void fillRespawnFloor(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                world.setBlock(x, y, z, BlockType.STONE);
            }
        }
    }

    private static void prepareBedExplosionFireFixture(World world) {
        prepareExplosionFireSupportFloor(world);
        for (int y = 99; y <= 103; y++) {
            for (int z = -4; z <= 4; z++) {
                world.setBlock(2, y, z, BlockType.OBSIDIAN, 0);
            }
        }
        assertNotNull(world.placeBed(0, 100, 0, 1, null));
    }

    private static void prepareExplosionFireSupportFloor(World world) {
        for (int y = 96; y <= 106; y++) {
            for (int z = -8; z <= 8; z++) {
                for (int x = -8; x <= 8; x++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
        for (int z = -8; z <= 8; z++) {
            for (int x = -8; x <= 8; x++) {
                world.setBlock(x, 99, z, BlockType.OBSIDIAN, 0);
            }
        }
    }

    private static List<BlockPos> collectFireBlocks(World world, int minX, int maxX, int minY, int maxY,
            int minZ, int maxZ) {
        List<BlockPos> fires = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (world.getBlock(x, y, z) == BlockType.FIRE) {
                        fires.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return fires;
    }

    private static void placeSupportedBed(World world, int x, int y, int z, int facing) {
        int[] dir = World.horizontalDirection(facing);
        world.setBlock(x, y - 1, z, BlockType.STONE);
        world.setBlock(x + dir[0], y - 1, z + dir[1], BlockType.STONE);
        world.setBlock(x, y, z, BlockType.AIR);
        world.setBlock(x + dir[0], y, z + dir[1], BlockType.AIR);
        assertNotNull(world.placeBed(x, y, z, facing, null));
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private final Set<String> rebuilds = new HashSet<>();

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            rebuilds.add(key(x, y, z));
        }

        private boolean rebuilt(int x, int y, int z) {
            return rebuilds.contains(key(x, y, z));
        }

        private static String key(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }
}
