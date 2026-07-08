package com.craftzero.graphics;

import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingTableRendererTest {
    @Test
    @DisplayName("Enchanting table book model should follow tile spread and block position")
    void bookModelFollowsTileState() {
        World world = new World(5152L);
        try {
            EnchantingTableTileEntity table = new EnchantingTableTileEntity(4, 70, -3);
            world.setPlayer(new Player(4.5f, 70.0f, -1.5f));
            table.tick(world, 1.0f / 20.0f);

            Matrix4f left = EnchantingTableRenderer.bookHalfModel(table, 0.0f, true);
            Matrix4f right = EnchantingTableRenderer.bookHalfModel(table, 0.0f, false);
            Vector3f leftPosition = left.getTranslation(new Vector3f());
            Vector3f rightPosition = right.getTranslation(new Vector3f());

            assertTrue(leftPosition.x > 4.0f && leftPosition.x < 5.0f);
            assertTrue(leftPosition.y > 70.85f && leftPosition.y < 71.1f);
            assertTrue(leftPosition.z > -3.5f && leftPosition.z < -2.5f);
            assertNotEquals(left.m01(), right.m01(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }
}
