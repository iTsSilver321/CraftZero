package com.craftzero.main;

import com.craftzero.entity.BoatEntity;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBoatInteractionTest {
    @Test
    @DisplayName("Player can mount and dismount a boat")
    void playerMountsAndDismountsBoat() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        BoatEntity boat = new BoatEntity(2.0f, 70.0f, 0.0f);

        assertTrue(player.mountBoat(boat));

        assertTrue(player.isRidingBoat());
        assertSame(boat, player.getRidingBoat());
        assertTrue(boat.hasPlayerPassenger());

        player.dismountBoat();

        assertFalse(player.isRidingBoat());
        assertFalse(boat.hasPlayerPassenger());
    }

    @Test
    @DisplayName("Removed mounted boat clears player riding state on sync")
    void removedBoatClearsRidingState() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        BoatEntity boat = new BoatEntity(2.0f, 70.0f, 0.0f);

        assertTrue(player.mountBoat(boat));
        boat.remove();
        player.syncRidingPosition();

        assertFalse(player.isRidingBoat());
    }

    @Test
    @DisplayName("World boat collision pass should shove the local player")
    void worldBoatCollisionPassShovesPlayer() {
        World world = new World(6208L);
        try {
            Player player = new Player(1.2f, 90.0f, 0.5f);
            world.setPlayer(player);
            BoatEntity boat = new BoatEntity(0.5f, 90.0f, 0.5f);
            boat.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(List.of(boat));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(player.getVelocity().x > 0.03f);
            assertEquals(0.0f, player.getVelocity().z, 0.0001f);
            assertTrue(boat.getMotionX() < 0.18f);
            assertEquals(0.0f, boat.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mounted boat players should not be shoved by their own boat")
    void mountedBoatPlayerSkipsWorldCollisionShove() {
        World world = new World(6209L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 90.0f, 0.5f);
            boat.setMotion(0.2f, 0.0f, 0.0f);
            Player player = new Player(0.5f, 90.1f, 0.5f);
            world.setPlayer(player);
            assertTrue(player.mountBoat(boat));
            world.replaceEntities(List.of(boat));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, player.getVelocity().x, 0.0001f);
            assertEquals(0.0f, player.getVelocity().z, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mounted boat travel should update boat distance statistics")
    void mountedBoatTravelUpdatesStatistics() {
        World world = new World(6216L);
        try {
            for (int x = 0; x <= 2; x++) {
                world.setBlock(x, 90, 0, BlockType.WATER, 0);
            }
            BoatEntity boat = new BoatEntity(0.5f, 90.25f, 0.5f);
            Player player = new Player(0.5f, 90.35f, 0.5f);
            world.setPlayer(player);
            world.replaceEntities(List.of(boat));

            assertTrue(player.mountBoat(boat));
            boat.setMotion(0.2f, 0.0f, 0.0f);
            boat.tick();
            boat.updatePhysics(1.0f / 20.0f);

            player.update(1.0f / 20.0f, world);

            assertTrue(player.getStats().getStatistics().getDistanceByBoatCm() > 0);
            assertEquals(0, player.getStats().getStatistics().getDistanceWalkedCm());
        } finally {
            world.cleanup();
        }
    }
}
