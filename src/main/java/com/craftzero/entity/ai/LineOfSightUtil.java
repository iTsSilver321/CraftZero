package com.craftzero.entity.ai;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;

/**
 * Utility for checking line of sight between entities.
 * Uses raymarching to detect solid blocks between two points.
 */
public class LineOfSightUtil {

    /**
     * Check if there's a clear line of sight between two points.
     * Uses Bresenham-style raymarching through blocks.
     * 
     * @param world The world to check in
     * @param x1    Start X
     * @param y1    Start Y (eye level)
     * @param z1    Start Z
     * @param x2    End X
     * @param y2    End Y (eye level)
     * @param z2    End Z
     * @return true if no solid blocks obstruct the view
     */
    public static boolean hasLineOfSight(World world,
            float x1, float y1, float z1,
            float x2, float y2, float z2) {
        if (world == null)
            return false;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.1f)
            return true; // Same position

        // Normalize direction
        dx /= distance;
        dy /= distance;
        dz /= distance;

        // Step size - smaller than a block for accuracy
        float stepSize = 0.5f;
        int steps = (int) (distance / stepSize);

        float x = x1;
        float y = y1;
        float z = z1;

        for (int i = 0; i < steps; i++) {
            x += dx * stepSize;
            y += dy * stepSize;
            z += dz * stepSize;

            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z);

            BlockType block = world.getBlock(bx, by, bz);
            if (block != null && block.isSolid() && !block.isTransparent()) {
                return false; // Blocked by solid block
            }
        }

        return true; // Clear line of sight
    }

    /**
     * Check if a position is safe (not over a cliff).
     * A position is unsafe if dropping more than maxFallHeight blocks.
     * 
     * @param world         The world
     * @param x             X position
     * @param y             Current Y position
     * @param z             Z position
     * @param maxFallHeight Maximum safe fall distance (typically 3)
     * @return true if position is safe (ground nearby)
     */
    public static boolean isPositionSafe(World world, float x, float y, float z, int maxFallHeight) {
        if (world == null)
            return false;

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        // Check if there's ground within maxFallHeight blocks below
        for (int checkY = by; checkY >= by - maxFallHeight; checkY--) {
            BlockType block = world.getBlock(bx, checkY, bz);
            if (block != null && block.isSolid()) {
                return true; // Found ground
            }
        }

        return false; // No ground within safe distance - cliff!
    }

    /**
     * Check if moving in a direction would lead off a cliff.
     * 
     * @param world         The world
     * @param x             Current X
     * @param y             Current Y (feet level)
     * @param z             Current Z
     * @param yaw           Direction to check (degrees)
     * @param checkDistance How far ahead to check
     * @return true if direction leads to cliff
     */
    public static boolean isCliffAhead(World world, float x, float y, float z,
            float yaw, float checkDistance) {
        float rad = (float) Math.toRadians(yaw);
        float dx = (float) Math.sin(rad) * checkDistance;
        float dz = -(float) Math.cos(rad) * checkDistance;

        float checkX = x + dx;
        float checkZ = z + dz;

        return !isPositionSafe(world, checkX, y, checkZ, 3);
    }

    /**
     * Find a safe direction to move (avoiding cliffs).
     * Checks multiple angles and returns the first safe one.
     * 
     * @param world        The world
     * @param x            Current X
     * @param y            Current Y
     * @param z            Current Z
     * @param preferredYaw Preferred direction (will try nearby angles first)
     * @return Safe yaw angle, or preferredYaw if no safe direction found
     */
    public static float findSafeDirection(World world, float x, float y, float z,
            float preferredYaw) {
        // Check preferred direction first
        if (!isCliffAhead(world, x, y, z, preferredYaw, 1.5f)) {
            return preferredYaw;
        }

        // Try nearby angles
        float[] offsets = { 45, -45, 90, -90, 135, -135, 180 };
        for (float offset : offsets) {
            float testYaw = preferredYaw + offset;
            if (!isCliffAhead(world, x, y, z, testYaw, 1.5f)) {
                return testYaw;
            }
        }

        // No safe direction - return preferred (mob will stop)
        return preferredYaw;
    }
}
