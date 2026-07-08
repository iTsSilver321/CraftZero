package com.craftzero.main;

import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainBedSleepTest {
    @Test
    @DisplayName("Bed sleep overlay should fade to the old dark screen")
    void bedSleepOverlayAlphaFadesToDarkScreen() {
        assertEquals(0.0f, Main.bedSleepOverlayAlpha(false, 0.5f, 1.0f), 0.0001f);
        assertEquals(0.0f, Main.bedSleepOverlayAlpha(true, -1.0f, 1.0f), 0.0001f);
        assertEquals(0.43f, Main.bedSleepOverlayAlpha(true, 0.5f, 1.0f), 0.0001f);
        assertEquals(0.86f, Main.bedSleepOverlayAlpha(true, 1.0f, 1.0f), 0.0001f);
        assertEquals(0.86f, Main.bedSleepOverlayAlpha(true, 3.0f, 1.0f), 0.0001f);
    }

    @Test
    @DisplayName("Bed sleep should preserve old spawn when no safe adjacent respawn exists")
    void bedSleepPreservesSpawnWhenAdjacentRespawnIsBlocked() {
        World world = new World(628600L);
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
            setNight(world);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            player.setSpawnPosition(20.5f, 90.0f, 20.5f);

            World.BedUseResult bedUse = world.useBed(0, 100, 0);

            assertTrue(bedUse.sleepAllowed());
            assertFalse(Main.applyBedSpawnIfSafe(player, world, bedUse));
            assertFalse(player.hasBedSpawn());
            assertEquals(20.5f, player.getSpawnX(), 0.0001f);
            assertEquals(90.0f, player.getSpawnY(), 0.0001f);
            assertEquals(20.5f, player.getSpawnZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bed sleep should save the resolved adjacent standing spawn")
    void bedSleepSavesResolvedAdjacentRespawn() {
        World world = new World(628601L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            setNight(world);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            player.setSpawnPosition(20.5f, 90.0f, 20.5f);

            World.BedUseResult bedUse = world.useBed(0, 100, 0);

            assertTrue(bedUse.sleepAllowed());
            assertTrue(Main.applyBedSpawnIfSafe(player, world, bedUse));
            assertTrue(player.hasBedSpawn());
            assertEquals(new BlockPos(0, 100, 0), player.getBedSpawnPos());
            assertEquals(-0.5f, player.getSpawnX(), 0.0001f);
            assertEquals(100.0f, player.getSpawnY(), 0.0001f);
            assertEquals(-0.5f, player.getSpawnZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Accepted bed sleep should enter bed pose and wake beside the bed")
    void acceptedBedSleepStartsPoseAndWakesBesideBed() {
        World world = new World(628605L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            setNight(world);
            Player player = new Player(4.0f, 100.0f, 4.0f);
            player.getCamera().setYaw(135.0f);
            player.getCamera().setPitch(20.0f);

            World.BedUseResult bedUse = world.useBed(0, 100, 0);

            assertTrue(Main.beginAcceptedBedSleep(player, world, bedUse));
            assertTrue(player.isSleeping());
            assertEquals(new BlockPos(0, 100, 0), player.getSleepingBedFootPos());
            assertEquals(new BlockPos(0, 100, -1), player.getSleepingBedHeadPos());
            assertEquals(0, player.getSleepingBedFacing());
            assertEquals(0.0f, player.getSleepingRenderYaw(), 0.0001f);
            assertEquals(0.5f, player.getPosition().x, 0.0001f);
            assertEquals(100.5625f, player.getPosition().y, 0.0001f);
            assertEquals(0.0f, player.getPosition().z, 0.0001f);

            assertTrue(Main.finishAcceptedBedSleep(player, world, bedUse));

            assertFalse(player.isSleeping());
            assertEquals(-0.5f, player.getPosition().x, 0.0001f);
            assertEquals(100.0f, player.getPosition().y, 0.0001f);
            assertEquals(-0.5f, player.getPosition().z, 0.0001f);
            assertEquals(135.0f, player.getCamera().getYaw(), 0.0001f);
            assertEquals(20.0f, player.getCamera().getPitch(), 0.0001f);
            assertTrue(player.hasBedSpawn());
            assertEquals(new BlockPos(0, 100, 0), player.getBedSpawnPos());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Accepted bed sleep should preserve position and spawn when wake spaces are blocked")
    void acceptedBedSleepFallsBackWhenWakeSpacesAreBlocked() {
        World world = new World(628606L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            blockAdjacentRespawnSpaces(world);
            setNight(world);
            Player player = new Player(4.0f, 100.0f, 4.0f);
            player.setSpawnPosition(20.5f, 90.0f, 20.5f);

            World.BedUseResult bedUse = world.useBed(0, 100, 0);

            assertTrue(Main.beginAcceptedBedSleep(player, world, bedUse));
            assertTrue(player.isSleeping());
            assertFalse(player.hasBedSpawn());

            assertTrue(Main.finishAcceptedBedSleep(player, world, bedUse));

            assertFalse(player.isSleeping());
            assertEquals(4.0f, player.getPosition().x, 0.0001f);
            assertEquals(100.0f, player.getPosition().y, 0.0001f);
            assertEquals(4.0f, player.getPosition().z, 0.0001f);
            assertFalse(player.hasBedSpawn());
            assertEquals(20.5f, player.getSpawnX(), 0.0001f);
            assertEquals(90.0f, player.getSpawnY(), 0.0001f);
            assertEquals(20.5f, player.getSpawnZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Leaving bed should clear occupied state without skipping night")
    void leavingBedCancelsSleepWithoutSkippingNight() {
        World world = new World(628607L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(18000);
            world.setDayCycleManager(dayCycle);
            world.setWeatherState("rain");
            Player player = new Player(4.0f, 100.0f, 4.0f);

            World.BedUseResult bedUse = world.useBed(0, 100, 0);

            assertTrue(Main.beginAcceptedBedSleep(player, world, bedUse));
            assertTrue(player.isSleeping());
            assertTrue(Main.cancelAcceptedBedSleep(player, world, bedUse));

            assertFalse(player.isSleeping());
            assertEquals(18000.0f, dayCycle.getTime(), 0.001f);
            assertEquals("rain", world.getWeatherState());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & World.BED_OCCUPIED_BIT);
            assertEquals(0, world.getBlockMetadata(0, 100, -1) & World.BED_OCCUPIED_BIT);
            assertEquals(-0.5f, player.getPosition().x, 0.0001f);
            assertEquals(100.0f, player.getPosition().y, 0.0001f);
            assertEquals(-0.5f, player.getPosition().z, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Death respawn should revalidate a saved bed before using it")
    void deathRespawnRevalidatesSavedBed() {
        World world = new World(628602L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            setNight(world);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            World.BedUseResult bedUse = world.useBed(0, 100, 0);
            assertTrue(Main.applyBedSpawnIfSafe(player, world, bedUse));
            assertTrue(world.completeBedSleep(bedUse));

            player.setPosition(50.0f, 120.0f, 50.0f);
            assertEquals(Main.RespawnTarget.BED, Main.preparePlayerRespawn(player, world, 20, 90, 20));
            player.respawn();

            assertEquals(-0.5f, player.getPosition().x, 0.0001f);
            assertEquals(100.0f, player.getPosition().y, 0.0001f);
            assertEquals(-0.5f, player.getPosition().z, 0.0001f);
            assertTrue(player.hasBedSpawn());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Death respawn should fall back to world spawn when saved bed is gone")
    void deathRespawnFallsBackWhenSavedBedIsGone() {
        World world = new World(628603L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            setNight(world);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            World.BedUseResult bedUse = world.useBed(0, 100, 0);
            assertTrue(Main.applyBedSpawnIfSafe(player, world, bedUse));
            assertTrue(world.completeBedSleep(bedUse));
            assertTrue(world.breakBlock(0, 100, 0, false));

            player.setPosition(50.0f, 120.0f, 50.0f);
            assertEquals(Main.RespawnTarget.WORLD_SPAWN, Main.preparePlayerRespawn(player, world, 20, 90, 20));
            player.respawn();

            assertEquals(20.5f, player.getPosition().x, 0.0001f);
            assertEquals(90.0f, player.getPosition().y, 0.0001f);
            assertEquals(20.5f, player.getPosition().z, 0.0001f);
            assertFalse(player.hasBedSpawn());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Death respawn should fall back to world spawn when saved bed is obstructed")
    void deathRespawnFallsBackWhenSavedBedIsObstructed() {
        World world = new World(628604L);
        try {
            fillRespawnFloor(world, -1, 1, -2, 1, 99);
            placeSupportedBed(world, 0, 100, 0, 0);
            setNight(world);
            Player player = new Player(0.0f, 100.0f, 0.0f);
            World.BedUseResult bedUse = world.useBed(0, 100, 0);
            assertTrue(Main.applyBedSpawnIfSafe(player, world, bedUse));
            assertTrue(world.completeBedSleep(bedUse));
            blockAdjacentRespawnSpaces(world);

            player.setPosition(50.0f, 120.0f, 50.0f);
            assertEquals(Main.RespawnTarget.WORLD_SPAWN, Main.preparePlayerRespawn(player, world, 20, 90, 20));
            player.respawn();

            assertEquals(20.5f, player.getPosition().x, 0.0001f);
            assertEquals(90.0f, player.getPosition().y, 0.0001f);
            assertEquals(20.5f, player.getPosition().z, 0.0001f);
            assertFalse(player.hasBedSpawn());
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

    private static void placeSupportedBed(World world, int x, int y, int z, int facing) {
        assertTrue(world.placeBed(x, y, z, facing, null) != null);
    }

    private static void blockAdjacentRespawnSpaces(World world) {
        for (int z = -2; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                if ((x == 0 && z == 0) || (x == 0 && z == -1)) {
                    continue;
                }
                world.setBlock(x, 100, z, BlockType.STONE);
            }
        }
    }
}
