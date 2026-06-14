package com.craftzero.entity;

import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrowEntityTest {
    @Test
    @DisplayName("Arrow should stick in the first solid block it intersects")
    void arrowSticksInBlock() {
        World world = new World(11L);
        try {
            world.setBlock(3, 100, 0, BlockType.STONE);
            ArrowEntity arrow = world.spawnArrow(0.5f, 100.5f, 0.5f, 4.0f, 0.0f, 0.0f,
                    null, true, 4.0f);

            world.updateEntities(1.0f / 60.0f);

            assertFalse(arrow.isRemoved());
            assertTrue(arrow.isInGround());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrow should damage living entities on collision")
    void arrowDamagesMob() {
        World world = new World(12L);
        try {
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            ArrowEntity arrow = world.spawnArrow(0.5f, 101.0f, 0.5f, 3.0f, 0.0f, 0.0f,
                    null, true, 5.0f);
            world.spawnEntity(zombie);

            world.updateEntities(1.0f / 60.0f);

            assertTrue(arrow.isRemoved());
            assertTrue(zombie.getHealth() < 20.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Skeleton ranged attack should spawn a visible arrow entity")
    void skeletonSpawnsArrow() {
        World world = new World(13L);
        try {
            world.getChunkNow(0, 0);
            for (int x = 0; x <= 15; x++) {
                world.setBlock(x, 99, 0, BlockType.STONE);
                for (int y = 100; y <= 103; y++) {
                    world.setBlock(x, y, 0, BlockType.AIR);
                }
            }
            Player player = new Player(14.0f, 100.0f, 0.5f);
            player.setWorld(world);
            world.setPlayer(player);

            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(skeleton);

            world.updateEntities(1.0f / 60.0f);
            assertTrue(skeleton.getAI().hasMoveTarget(), "skeleton should acquire the player");

            for (int i = 0; i < 45; i++) {
                world.updateEntities(1.0f / 60.0f);
            }

            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof ArrowEntity),
                    () -> world.getEntities().stream()
                            .map(entity -> entity.getClass().getSimpleName())
                            .toList()
                            .toString());
        } finally {
            world.cleanup();
        }
    }
}
