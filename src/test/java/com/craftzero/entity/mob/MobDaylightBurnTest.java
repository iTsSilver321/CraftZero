package com.craftzero.entity.mob;

import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.BiomeType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MobDaylightBurnTest {
    @Test
    @DisplayName("Undead mobs should burn in open daytime sky when the old daylight roll succeeds")
    void zombieBurnsInOpenDaylightWhenRollSucceeds() {
        World world = new World(201L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000.0f);
            world.setDayCycleManager(dayCycle);
            prepareColumn(world);

            Zombie zombie = new Zombie();
            zombie.random = fixedNextFloats(0.0f);
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertTrue(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Undead mobs should wait for a later tick when the old daylight roll misses")
    void zombieDoesNotBurnWhenDaylightRollMisses() {
        World world = new World(204L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000.0f);
            world.setDayCycleManager(dayCycle);
            prepareColumn(world);

            Zombie zombie = new Zombie();
            zombie.random = fixedNextFloats(0.05f);
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Undead mobs should not burn under solid cover")
    void zombieDoesNotBurnUnderCover() {
        World world = new World(202L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000.0f);
            world.setDayCycleManager(dayCycle);
            prepareColumn(world);
            world.setBlock(0, 104, 0, BlockType.STONE);

            Zombie zombie = new Zombie();
            zombie.random = fixedNextFloats(0.0f);
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water should extinguish undead before daylight burn applies")
    void waterExtinguishesBeforeDaylightBurn() {
        World world = new World(203L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000.0f);
            world.setDayCycleManager(dayCycle);
            prepareColumn(world);
            world.setBlock(0, 100, 0, BlockType.WATER);

            Zombie zombie = new Zombie();
            zombie.random = fixedNextFloats(0.0f);
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            zombie.setOnFire(80);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rain should extinguish undead and suppress daylight burn")
    void rainExtinguishesAndPreventsDaylightBurn() {
        World world = new World(205L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000.0f);
            world.setDayCycleManager(dayCycle);
            int[] pos = findRainBiome(world);
            prepareColumn(world, pos[0], pos[1]);
            world.setWeatherState("rain");

            Zombie zombie = new Zombie();
            zombie.random = fixedNextFloats(0.0f);
            zombie.setPosition(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            zombie.setOnFire(80);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    private static void prepareColumn(World world) {
        prepareColumn(world, 0, 0);
    }

    private static void prepareColumn(World world, int x, int z) {
        world.getChunkNow(0, 0);
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, BlockType.STONE);
        for (int y = 100; y < 128; y++) {
            world.setBlock(x, y, z, BlockType.AIR);
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

    private static Random fixedNextFloats(float... values) {
        return new Random() {
            private int index;

            @Override
            public float nextFloat() {
                if (values.length == 0) {
                    return 0.0f;
                }
                float value = values[index % values.length];
                index++;
                return value;
            }
        };
    }
}
