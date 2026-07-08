package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfCombatBehaviorTest {
    @Test
    @DisplayName("Wild wolves become angry and bite players that attack them")
    void wildWolfRetaliatesAgainstPlayerAttack() {
        World world = new World(6262L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Player player = new Player(1.2f, 70.0f, 0.5f);
            player.setWorld(world);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf));

            assertTrue(wolf.damage(1.0f, DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z,
                    0.0f, 0.0f)));
            assertTrue(wolf.isAngry());
            assertTrue(wolf.getAI().hasMoveTarget());

            world.updateEntities(1.0f / 20.0f);

            assertEquals(20.0f - 2.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player attacks should alert nearby calm wild wolves")
    void playerAttackAlertsNearbyWildWolves() {
        World world = new World(6268L);
        try {
            makeFloor(world, -2, 20, -2, 2, 69);
            Player player = new Player(1.2f, 70.0f, 0.5f);
            player.setWorld(world);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);
            Wolf attacked = new Wolf();
            attacked.setPosition(0.5f, 70.0f, 0.5f);
            Wolf nearby = new Wolf();
            nearby.setPosition(8.0f, 70.0f, 0.5f);
            Wolf far = new Wolf();
            far.setPosition(18.0f, 70.0f, 0.5f);
            Wolf tamed = new Wolf();
            tamed.setTamed(true);
            tamed.setOwnerName(player.getPlayerName());
            tamed.setPosition(2.0f, 70.0f, 0.5f);
            world.replaceEntities(List.of(attacked, nearby, far, tamed));

            assertTrue(attacked.damage(1.0f, DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z,
                    0.0f, 0.0f)));

            assertTrue(attacked.isAngry());
            assertTrue(nearby.isAngry());
            assertFalse(far.isAngry());
            assertFalse(tamed.isAngry());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("One-hit wolf kills should not alert the nearby pack")
    void lethalPlayerAttackDoesNotAlertNearbyWolves() {
        World world = new World(6269L);
        try {
            Player player = new Player(1.2f, 70.0f, 0.5f);
            player.setWorld(world);
            world.setPlayer(player);
            Wolf killed = new Wolf();
            killed.setPosition(0.5f, 70.0f, 0.5f);
            Wolf nearby = new Wolf();
            nearby.setPosition(2.0f, 70.0f, 0.5f);
            world.replaceEntities(List.of(killed, nearby));

            assertTrue(killed.damage(killed.getMaxHealth(), DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z,
                    0.0f, 0.0f)));

            assertFalse(killed.isAngry());
            assertFalse(nearby.isAngry());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Standing tamed wolves bite their assigned combat target")
    void tamedWolfAttacksAssistTarget() {
        World world = new World(6263L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setOwnerName(player.getPlayerName());
            wolf.setSitting(false);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Zombie zombie = new Zombie();
            zombie.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf, zombie));

            wolf.setAssistTarget(zombie);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(zombie.getMaxHealth() - Wolf.TAMED_ATTACK_DAMAGE, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hurt tamed wolves should stand and retaliate against living attackers")
    void hurtTamedWolfStandsAndTargetsAttacker() {
        World world = new World(6270L);
        try {
            Player owner = new Player(0.0f, 70.0f, 0.0f);
            owner.setPlayerName("Alex");
            world.setPlayer(owner);
            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setOwnerName(owner.getPlayerName());
            wolf.setSitting(true);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Zombie zombie = new Zombie();
            zombie.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf, zombie));

            assertTrue(wolf.damage(1.0f, DamageSource.entity(DamageSource.Type.MOB_MELEE, zombie)));

            assertFalse(wolf.isSitting());
            assertSame(zombie, wolf.getAssistTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrow-hit tamed wolves should retaliate against the living shooter")
    void arrowHitTamedWolfTargetsShooter() {
        World world = new World(6271L);
        try {
            Player owner = new Player(0.0f, 70.0f, 0.0f);
            owner.setPlayerName("Alex");
            world.setPlayer(owner);
            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setOwnerName(owner.getPlayerName());
            wolf.setSitting(true);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(3.0f, 70.0f, 0.5f);
            ArrowEntity arrow = new ArrowEntity(2.5f, 71.0f, 0.5f,
                    -0.4f, 0.0f, 0.0f, skeleton, false, 2.0f);
            world.replaceEntities(List.of(wolf, skeleton, arrow));

            assertTrue(wolf.damage(1.0f, DamageSource.entity(DamageSource.Type.ARROW, arrow)));

            assertFalse(wolf.isSitting());
            assertSame(skeleton, wolf.getAssistTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wild calm wolves hunt and bite nearby sheep")
    void wildWolfHuntsNearbySheep() {
        World world = new World(6266L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Wolf wolf = new Wolf();
            wolf.random = fixedNextInt(0);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf, sheep));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(sheep.getMaxHealth() - Wolf.WILD_ATTACK_DAMAGE, sheep.getHealth(), 0.001f);
            assertFalse(wolf.isAngry());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wild wolf sheep hunting should use the source 1-in-200 target chance")
    void wildWolfSheepHuntUsesSourceTargetChance() {
        World world = new World(6272L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Wolf wolf = new Wolf();
            wolf.random = fixedNextInt(1);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf, sheep));

            for (int i = 0; i < 5; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertEquals(sheep.getMaxHealth(), sheep.getHealth(), 0.001f);
            assertFalse(wolf.getAI().hasTarget());
            assertFalse(wolf.isAngry());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Tamed wolves do not hunt sheep without an owner-assist target")
    void tamedWolfDoesNotHuntSheep() {
        World world = new World(6267L);
        try {
            makeFloor(world, -2, 3, -2, 2, 69);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setOwnerName(player.getPlayerName());
            wolf.setSitting(false);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.4f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf, sheep));

            for (int i = 0; i < 25; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertEquals(sheep.getMaxHealth(), sheep.getHealth(), 0.001f);
            assertFalse(wolf.getAI().hasTarget());
        } finally {
            world.cleanup();
        }
    }

    private static Random fixedNextInt(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                if (value < 0 || value >= bound) {
                    throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
                }
                return value;
            }
        };
    }

    private static void makeFloor(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, y, z, BlockType.STONE, 0);
            }
        }
    }
}
