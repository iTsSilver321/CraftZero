package com.craftzero.entity.mob;

import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfFollowBehaviorTest {
    @Test
    @DisplayName("Tamed standing wolves follow the player when far enough away")
    void tamedWolfFollowsPlayer() {
        World world = new World(6260L);
        try {
            makeFloor(world, -2, 10, -2, 2, 69);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            own(wolf, player);
            wolf.setSitting(false);
            wolf.setPosition(8.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf));

            for (int i = 0; i < 12; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertTrue(wolf.getX() < 8.0f, () -> "wolf x=" + wolf.getX());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sitting tamed wolves do not follow the player")
    void sittingWolfDoesNotFollowPlayer() {
        World world = new World(6261L);
        try {
            makeFloor(world, -2, 10, -2, 2, 69);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            own(wolf, player);
            wolf.setSitting(true);
            wolf.setPosition(8.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf));

            for (int i = 0; i < 12; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertEquals(8.0f, wolf.getX(), 0.001f);
            assertEquals(0.0f, wolf.getMotionX(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Very distant tamed wolves teleport near the player")
    void distantWolfTeleportsNearPlayer() {
        World world = new World(6262L);
        try {
            makeFloor(world, -3, 3, -3, 3, 69);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            own(wolf, player);
            wolf.setSitting(false);
            wolf.setPosition(20.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf));

            world.updateEntities(1.0f / 20.0f);

            float dx = wolf.getX() - player.getPosition().x;
            float dz = wolf.getZ() - player.getPosition().z;
            assertTrue(dx * dx + dz * dz <= 5.0f * 5.0f,
                    () -> "wolf did not teleport near owner: " + wolf.getX() + "," + wolf.getZ());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Tamed wolf teleport skips liquid, fire, and cactus landing spots")
    void distantWolfTeleportSkipsUnsafeLandingSpots() {
        World world = new World(6263L);
        try {
            makeFloor(world, -3, 3, -3, 3, 69);
            clearSpace(world, -3, 3, -3, 3, 70, 71);
            world.setBlock(-2, 70, -2, BlockType.WATER, 0);
            world.setBlock(-2, 70, -1, BlockType.FIRE, 0);
            world.setBlock(-2, 70, 0, BlockType.CACTUS, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            own(wolf, player);
            wolf.setSitting(false);
            wolf.setPosition(20.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(-2, (int) Math.floor(wolf.getX()));
            assertEquals(1, (int) Math.floor(wolf.getZ()));
            assertEquals(BlockType.AIR, world.getBlock((int) Math.floor(wolf.getX()), 70,
                    (int) Math.floor(wolf.getZ())));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Tamed wolf teleport can land one block below the owner")
    void distantWolfTeleportSearchesNearbyVerticalOffsets() {
        World world = new World(6264L);
        try {
            makeFloor(world, -3, 3, -3, 3, 68);
            clearSpace(world, -3, 3, -3, 3, 69, 70);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            own(wolf, player);
            wolf.setSitting(false);
            wolf.setPosition(20.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(69.0f, wolf.getY(), 0.001f);
            assertEquals(-2, (int) Math.floor(wolf.getX()));
            assertEquals(-2, (int) Math.floor(wolf.getZ()));
        } finally {
            world.cleanup();
        }
    }

    private static void clearSpace(World world, int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void makeFloor(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, y, z, BlockType.STONE, 0);
            }
        }
    }

    private static void own(Wolf wolf, Player player) {
        wolf.setTamed(true);
        wolf.setOwnerName(player.getPlayerName());
    }
}
