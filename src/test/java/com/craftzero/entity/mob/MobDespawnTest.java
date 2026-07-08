package com.craftzero.entity.mob;

import com.craftzero.main.Difficulty;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobDespawnTest {
    @Test
    @DisplayName("Release 1.0 monster mobs can despawn when extremely far from the player")
    void monstersCanDespawnWhenFarFromPlayer() {
        World world = new World(6277L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Zombie zombie = new Zombie();
            zombie.setPosition(300.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(zombie.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 monster mobs hard-despawn beyond 128 blocks")
    void monstersHardDespawnBeyond128Blocks() {
        World world = new World(6281L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Zombie zombie = new Zombie();
            zombie.setPosition(129.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(zombie.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 monster mobs do not soft-despawn before old age")
    void monstersDoNotSoftDespawnBeforeOldAge() {
        World world = new World(6282L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Zombie zombie = new Zombie();
            zombie.random = new ZeroRandom();
            zombie.despawnTimer = Mob.SOFT_DESPAWN_MIN_AGE - 1;
            zombie.setPosition(40.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(zombie.isRemoved());
            assertEquals(Mob.SOFT_DESPAWN_MIN_AGE, zombie.despawnTimer);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 old monster mobs can randomly soft-despawn outside 32 blocks")
    void oldMonstersCanRandomlySoftDespawnOutside32Blocks() {
        World world = new World(6283L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Zombie zombie = new Zombie();
            zombie.random = new ZeroRandom();
            zombie.despawnTimer = Mob.SOFT_DESPAWN_MIN_AGE;
            zombie.setPosition(40.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(zombie.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 nearby monster mobs reset soft-despawn age")
    void nearbyMonstersResetSoftDespawnAge() {
        World world = new World(6284L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Zombie zombie = new Zombie();
            zombie.random = new ZeroRandom();
            zombie.despawnTimer = Mob.SOFT_DESPAWN_MIN_AGE + 100;
            zombie.setPosition(16.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(zombie.isRemoved());
            assertEquals(0, zombie.despawnTimer);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 passive animals should not distance-despawn")
    void passiveAnimalsDoNotDistanceDespawn() {
        World world = new World(6278L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Pig pig = new Pig();
            pig.setPosition(300.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(pig));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(pig.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 utility mobs should not distance-despawn")
    void utilityMobsDoNotDistanceDespawn() {
        World world = new World(6279L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            SnowGolem golem = new SnowGolem();
            golem.setPosition(300.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(golem));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(golem.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 water creatures hard-despawn beyond 128 blocks")
    void waterCreaturesHardDespawnBeyond128Blocks() {
        World world = new World(6285L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Squid squid = new Squid();
            squid.setPosition(129.0f, 62.0f, 0.0f);
            world.replaceEntities(List.of(squid));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(squid.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 old water creatures can randomly soft-despawn outside 32 blocks")
    void oldWaterCreaturesCanRandomlySoftDespawnOutside32Blocks() {
        World world = new World(6286L);
        try {
            world.setPlayer(new Player(0.0f, 70.0f, 0.0f));
            Squid squid = new Squid();
            squid.random = new ZeroRandom();
            squid.despawnTimer = Mob.SOFT_DESPAWN_MIN_AGE;
            squid.setPosition(40.0f, 62.0f, 0.0f);
            world.replaceEntities(List.of(squid));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(squid.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 monster mobs should vanish on Peaceful difficulty")
    void monsterMobsVanishOnPeacefulDifficulty() {
        World world = new World(6280L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setDifficulty(Difficulty.PEACEFUL);
            world.setPlayer(player);
            Zombie zombie = new Zombie();
            zombie.setPosition(4.0f, 70.0f, 0.0f);
            Pig pig = new Pig();
            pig.setPosition(5.0f, 70.0f, 0.0f);
            SnowGolem golem = new SnowGolem();
            golem.setPosition(6.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie, pig, golem));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(zombie.isRemoved());
            assertFalse(pig.isRemoved());
            assertFalse(golem.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public float nextFloat() {
            return 0.0f;
        }
    }
}
