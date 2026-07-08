package com.craftzero.main;

import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.mob.Pig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDimensionTransferTest {
    @Test
    @DisplayName("Dimension transfer placement detaches minecart riders and clears carried motion")
    void dimensionTransferDetachesMinecartAndResetsMotion() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        MinecartEntity cart = new MinecartEntity(12.0f, 70.0f, 0.0f, MinecartEntity.CartKind.RIDEABLE);

        assertTrue(player.mountMinecart(cart));
        player.getVelocity().set(1.0f, -3.0f, 2.0f);

        player.placeAfterDimensionTransfer(100.5f, 64.0f, -32.5f);
        player.syncRidingPosition();

        assertFalse(player.isRidingMinecart());
        assertNull(player.getRidingMinecart());
        assertFalse(cart.hasPlayerPassenger());
        assertAtTransferTarget(player);
        assertMotionCleared(player);
    }

    @Test
    @DisplayName("Dimension transfer placement detaches boat riders and clears carried motion")
    void dimensionTransferDetachesBoatAndResetsMotion() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        BoatEntity boat = new BoatEntity(12.0f, 70.0f, 0.0f);

        assertTrue(player.mountBoat(boat));
        player.getVelocity().set(1.0f, -3.0f, 2.0f);

        player.placeAfterDimensionTransfer(100.5f, 64.0f, -32.5f);
        player.syncRidingPosition();

        assertFalse(player.isRidingBoat());
        assertNull(player.getRidingBoat());
        assertFalse(boat.hasPlayerPassenger());
        assertAtTransferTarget(player);
        assertMotionCleared(player);
    }

    @Test
    @DisplayName("Dimension transfer placement detaches pig riders and clears carried motion")
    void dimensionTransferDetachesPigAndResetsMotion() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        pig.setPosition(12.0f, 70.0f, 0.0f);
        pig.setSaddled(true);

        assertTrue(player.mountPig(pig));
        player.getVelocity().set(1.0f, -3.0f, 2.0f);

        player.placeAfterDimensionTransfer(100.5f, 64.0f, -32.5f);
        player.syncRidingPosition();

        assertFalse(player.isRidingPig());
        assertNull(player.getRidingPig());
        assertFalse(pig.hasPlayerPassenger());
        assertAtTransferTarget(player);
        assertMotionCleared(player);
    }

    private static void assertAtTransferTarget(Player player) {
        assertEquals(100.5f, player.getPosition().x, 0.0001f);
        assertEquals(64.0f, player.getPosition().y, 0.0001f);
        assertEquals(-32.5f, player.getPosition().z, 0.0001f);
    }

    private static void assertMotionCleared(Player player) {
        assertEquals(0.0f, player.getVelocity().x, 0.0001f);
        assertEquals(0.0f, player.getVelocity().y, 0.0001f);
        assertEquals(0.0f, player.getVelocity().z, 0.0001f);
    }
}
