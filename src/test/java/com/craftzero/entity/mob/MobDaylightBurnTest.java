package com.craftzero.entity.mob;

import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobDaylightBurnTest {
    @Test
    @DisplayName("Undead mobs should burn in open daytime sky")
    void zombieBurnsInOpenDaylight() {
        World world = new World(201L);
        try {
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(6000.0f);
            world.setDayCycleManager(dayCycle);
            prepareColumn(world);

            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertTrue(zombie.isOnFire());
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
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            zombie.setOnFire(80);
            world.spawnEntity(zombie);
            world.updateEntities(1.0f / 60.0f);

            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    private static void prepareColumn(World world) {
        world.getChunkNow(0, 0);
        world.setBlock(0, 99, 0, BlockType.STONE);
        for (int y = 100; y < 128; y++) {
            world.setBlock(0, y, 0, BlockType.AIR);
        }
    }
}
