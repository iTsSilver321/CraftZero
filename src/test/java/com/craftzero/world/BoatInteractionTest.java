package com.craftzero.world;

import com.craftzero.entity.BoatEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatInteractionTest {
    @Test
    @DisplayName("Boats place on water and enter the entity list")
    void placeBoatOnWater() {
        World world = new World(6210L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);

            assertTrue(world.placeBoatOnWater(0, 70, 0, 90.0f));
            world.updateEntities(1.0f / 20.0f);

            BoatEntity boat = world.getEntities().stream()
                    .filter(BoatEntity.class::isInstance)
                    .map(BoatEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.5f, boat.getX(), 0.001f);
            assertEquals(0.5f, boat.getZ(), 0.001f);
            assertEquals(90.0f, boat.getYaw(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boats do not place on non-water blocks")
    void boatPlacementRequiresWater() {
        World world = new World(6211L);
        try {
            world.setBlock(0, 70, 0, BlockType.GRASS, 0);

            assertFalse(world.placeBoatOnWater(0, 70, 0, 0.0f));
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream().noneMatch(BoatEntity.class::isInstance));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boat placement rejects same-tick pending boat overlap")
    void boatPlacementRejectsPendingBoatOverlap() {
        World world = new World(6212L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);

            assertTrue(world.placeBoatOnWater(0, 70, 0, 0.0f));
            assertFalse(world.placeBoatOnWater(0, 70, 0, 0.0f));
            world.updateEntities(1.0f / 20.0f);

            assertEquals(1, world.getEntities().stream()
                    .filter(BoatEntity.class::isInstance)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boat placement rejects live entity overlap")
    void boatPlacementRejectsLiveEntityOverlap() {
        World world = new World(6213L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);
            world.spawnEntity(new BoatEntity(0.5f, 70.25f, 0.5f));
            world.updateEntities(1.0f / 20.0f);

            assertFalse(world.placeBoatOnWater(0, 70, 0, 0.0f));

            assertEquals(1, world.getEntities().stream()
                    .filter(BoatEntity.class::isInstance)
                    .count());
        } finally {
            world.cleanup();
        }
    }
}
