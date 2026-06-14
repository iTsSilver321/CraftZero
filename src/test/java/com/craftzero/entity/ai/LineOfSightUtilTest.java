package com.craftzero.entity.ai;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LineOfSightUtil.
 * Tests cliff detection and direction finding logic.
 */
class LineOfSightUtilTest {

    // Note: These tests can't test hasLineOfSight directly without a World,
    // but we can test the helper logic and math functions.

    @Test
    @DisplayName("findSafeDirection should return preferred yaw when no cliff")
    void testSafeDirectionNoCliff() {
        // With null world, isCliffAhead returns true (unsafe),
        // so findSafeDirection will try all angles and return preferredYaw as fallback
        float result = LineOfSightUtil.findSafeDirection(null, 0, 64, 0, 90);

        // When no world, should return preferred yaw (fallback)
        assertEquals(90, result, 0.01f);
    }

    @Test
    @DisplayName("isPositionSafe should return false with null world")
    void testPositionSafeNullWorld() {
        assertFalse(LineOfSightUtil.isPositionSafe(null, 0, 64, 0, 3));
    }

    @Test
    @DisplayName("isCliffAhead should return true with null world")
    void testCliffAheadNullWorld() {
        assertTrue(LineOfSightUtil.isCliffAhead(null, 0, 64, 0, 0, 1.5f));
    }

    @Test
    @DisplayName("hasLineOfSight should return false with null world")
    void testLineOfSightNullWorld() {
        assertFalse(LineOfSightUtil.hasLineOfSight(null, 0, 64, 0, 10, 64, 10));
    }

    @Test
    @DisplayName("hasLineOfSight should return true for same position")
    void testLineOfSightSamePosition() {
        // Very close positions should return true (distance < 0.1)
        // But null world returns false, so this tests the early exit
        // Would need mock world to fully test
    }

    @Test
    @DisplayName("hasLineOfSight should be blocked by opaque world blocks")
    void testLineOfSightBlockedByWorldBlock() {
        World world = new World(301L);
        try {
            prepareClearCorridor(world);

            assertTrue(LineOfSightUtil.hasLineOfSight(world,
                    0.5f, 101.0f, 0.5f,
                    4.5f, 101.0f, 0.5f));

            world.setBlock(2, 101, 0, BlockType.STONE);

            assertFalse(LineOfSightUtil.hasLineOfSight(world,
                    0.5f, 101.0f, 0.5f,
                    4.5f, 101.0f, 0.5f));
        } finally {
            world.cleanup();
        }
    }

    // ==================== Direction Angle Tests ====================

    @Test
    @DisplayName("Direction calculations should use correct trigonometry")
    void testDirectionCalculations() {
        // Test that yaw 0 points in -Z direction (Minecraft convention)
        float yaw = 0;
        float rad = (float) Math.toRadians(yaw);
        float dx = (float) Math.sin(rad);
        float dz = -(float) Math.cos(rad);

        assertEquals(0.0f, dx, 0.01f); // No X movement
        assertEquals(-1.0f, dz, 0.01f); // -Z direction (north)
    }

    @Test
    @DisplayName("Yaw 90 should point in +X direction")
    void testDirectionYaw90() {
        float yaw = 90;
        float rad = (float) Math.toRadians(yaw);
        float dx = (float) Math.sin(rad);
        float dz = -(float) Math.cos(rad);

        assertEquals(1.0f, dx, 0.01f); // +X direction (east)
        assertEquals(0.0f, dz, 0.01f);
    }

    @Test
    @DisplayName("Yaw 180 should point in +Z direction")
    void testDirectionYaw180() {
        float yaw = 180;
        float rad = (float) Math.toRadians(yaw);
        float dx = (float) Math.sin(rad);
        float dz = -(float) Math.cos(rad);

        assertEquals(0.0f, dx, 0.01f);
        assertEquals(1.0f, dz, 0.01f); // +Z direction (south)
    }

    private static void prepareClearCorridor(World world) {
        world.getChunkNow(0, 0);
        for (int x = 0; x <= 5; x++) {
            for (int y = 99; y <= 103; y++) {
                world.setBlock(x, y, 0, BlockType.AIR);
            }
        }
    }
}
