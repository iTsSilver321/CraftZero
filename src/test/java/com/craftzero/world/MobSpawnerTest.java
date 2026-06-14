package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobSpawnerTest {
    @Test
    @DisplayName("Hostile spawn rule should allow dark clear solid ground")
    void hostileSpawnAllowedInDarkClearSpace() {
        World world = new World(401L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);

            MobSpawner spawner = new MobSpawner(world);

            assertTrue(spawner.canSpawnHostileAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile spawn rule should reject bright spaces")
    void hostileSpawnRejectedInBrightSpace() {
        World world = new World(402L);
        try {
            setTime(world, 6000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);

            MobSpawner spawner = new MobSpawner(world);

            assertFalse(spawner.canSpawnHostileAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawn rules should reject blocked feet or head space")
    void spawnRulesRejectBlockedSpace() {
        World world = new World(403L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);
            world.setBlock(0, 72, 0, BlockType.STONE);

            MobSpawner spawner = new MobSpawner(world);

            assertFalse(spawner.canSpawnHostileAt(0, 70, 0));
            assertFalse(spawner.canSpawnPassiveAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Passive spawn rule should require grass and sky light")
    void passiveSpawnRequiresGrassAndLight() {
        World world = new World(404L);
        try {
            prepareSpawnColumn(world, 0, 70, 0, BlockType.GRASS);
            MobSpawner spawner = new MobSpawner(world);

            assertTrue(spawner.canSpawnPassiveAt(0, 70, 0));

            world.setBlock(0, 70, 0, BlockType.DIRT);

            assertFalse(spawner.canSpawnPassiveAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    private static void setTime(World world, float time) {
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(time);
        world.setDayCycleManager(dayCycle);
    }

    private static void prepareSpawnColumn(World world, int x, int y, int z, BlockType ground) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, y, z, ground);
        world.setBlock(x, y + 1, z, BlockType.AIR);
        world.setBlock(x, y + 2, z, BlockType.AIR);
    }
}
