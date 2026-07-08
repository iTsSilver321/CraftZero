package com.craftzero.world;

import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CactusInteractionTest {
    @Test
    @DisplayName("Living mobs touching cactus should take contact damage")
    void mobTouchingCactusTakesDamage() {
        World world = new World(9101L);
        try {
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(0, 100, 0, BlockType.CACTUS, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 100.0f, 0.5f);
            world.replaceEntities(List.of(zombie));
            float beforeHealth = zombie.getHealth();

            world.updateEntities(1.0f / 20.0f);

            assertEquals(beforeHealth - 1.0f, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players touching cactus should take contact damage")
    void playerTouchingCactusTakesDamage() {
        World world = new World(9102L);
        try {
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(0, 100, 0, BlockType.CACTUS, 0);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

            player.update(1.0f / 20.0f, world);

            assertEquals(19.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Entities just outside the narrow cactus box should not take contact damage")
    void playerOutsideCactusCollisionDoesNotTakeDamage() {
        World world = new World(9104L);
        try {
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(0, 100, 0, BlockType.CACTUS, 0);
            Player player = new Player(1.25f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

            player.update(1.0f / 20.0f, world);

            assertEquals(20.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items touching cactus should be destroyed")
    void droppedItemTouchingCactusIsDestroyed() {
        World world = new World(9103L);
        try {
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(0, 100, 0, BlockType.CACTUS, 0);
            world.replaceDroppedItems(List.of(new DroppedItem(0.5f, 100.2f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f)));

            world.updateDroppedItems(1.0f / 20.0f);

            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items outside the narrow cactus box should survive")
    void droppedItemOutsideCactusCollisionSurvives() {
        World world = new World(9105L);
        try {
            world.setBlock(0, 99, 0, BlockType.SAND, 0);
            world.setBlock(0, 100, 0, BlockType.CACTUS, 0);
            world.replaceDroppedItems(List.of(new DroppedItem(1.07f, 100.2f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f)));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(1, world.getDroppedItems().size());
        } finally {
            world.cleanup();
        }
    }
}
