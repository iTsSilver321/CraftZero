package com.craftzero.physics;

import org.joml.Vector3f;

/**
 * Axis-Aligned Bounding Box for collision detection.
 * Uses Minecraft-style independent axis collision resolution.
 */
public class AABB {

    private Vector3f min;
    private Vector3f max;

    // Small epsilon to prevent floating-point precision issues
    private static final float EPSILON = 0.0001f;

    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
    }

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min = new Vector3f(minX, minY, minZ);
        this.max = new Vector3f(maxX, maxY, maxZ);
    }

    /**
     * Create AABB from center position and dimensions.
     */
    public static AABB fromCenter(Vector3f center, float width, float height, float depth) {
        float halfW = width / 2;
        float halfD = depth / 2;
        return new AABB(
                center.x - halfW, center.y, center.z - halfD,
                center.x + halfW, center.y + height, center.z + halfD);
    }

    /**
     * Create AABB for a block at the given position.
     */
    public static AABB forBlock(int x, int y, int z) {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    /**
     * Check if this AABB intersects with another (strict check, no touching).
     */
    public boolean intersects(AABB other) {
        return (min.x < other.max.x && max.x > other.min.x) &&
                (min.y < other.max.y && max.y > other.min.y) &&
                (min.z < other.max.z && max.z > other.min.z);
    }

    /**
     * Check if this AABB contains a point.
     */
    public boolean contains(Vector3f point) {
        return point.x >= min.x && point.x <= max.x &&
                point.y >= min.y && point.y <= max.y &&
                point.z >= min.z && point.z <= max.z;
    }

    /**
     * Check if this AABB contains another AABB.
     */
    public boolean contains(AABB other) {
        return other.min.x >= min.x && other.max.x <= max.x &&
                other.min.y >= min.y && other.max.y <= max.y &&
                other.min.z >= min.z && other.max.z <= max.z;
    }

    /**
     * Expand the AABB by the given amount in all directions.
     */
    public AABB expand(float amount) {
        return new AABB(
                min.x - amount, min.y - amount, min.z - amount,
                max.x + amount, max.y + amount, max.z + amount);
    }

    /**
     * Offset the AABB by the given vector.
     */
    public AABB offset(Vector3f offset) {
        return new AABB(
                new Vector3f(min).add(offset),
                new Vector3f(max).add(offset));
    }

    /**
     * Offset the AABB by the given values.
     */
    public AABB offset(float x, float y, float z) {
        return new AABB(
                min.x + x, min.y + y, min.z + z,
                max.x + x, max.y + y, max.z + z);
    }

    /**
     * Calculate collision offset on X axis.
     * Returns adjusted velocity to prevent intersection.
     * 'other' is the static block, 'this' is the moving player.
     */
    public float clipXCollide(AABB other, float velocityX) {
        // Must already overlap on Y and Z axes for X collision to matter
        if (other.max.y <= min.y + EPSILON || other.min.y >= max.y - EPSILON)
            return velocityX;
        if (other.max.z <= min.z + EPSILON || other.min.z >= max.z - EPSILON)
            return velocityX;

        // Moving right (+X) - check if block is to our right
        if (velocityX > 0 && other.min.x >= max.x - EPSILON) {
            float gap = other.min.x - max.x;
            if (gap < velocityX) {
                velocityX = Math.max(0, gap);
            }
        }

        // Moving left (-X) - check if block is to our left
        if (velocityX < 0 && other.max.x <= min.x + EPSILON) {
            float gap = other.max.x - min.x;
            if (gap > velocityX) {
                velocityX = Math.min(0, gap);
            }
        }

        return velocityX;
    }

    /**
     * Calculate collision offset on Y axis.
     * Returns adjusted velocity to prevent intersection.
     * 'other' is the static block, 'this' is the moving player.
     */
    public float clipYCollide(AABB other, float velocityY) {
        // Must already overlap on X and Z axes for Y collision to matter
        if (other.max.x <= min.x + EPSILON || other.min.x >= max.x - EPSILON)
            return velocityY;
        if (other.max.z <= min.z + EPSILON || other.min.z >= max.z - EPSILON)
            return velocityY;

        // Moving up (+Y) - check if block is above us
        if (velocityY > 0 && other.min.y >= max.y - EPSILON) {
            float gap = other.min.y - max.y;
            if (gap < velocityY) {
                velocityY = Math.max(0, gap);
            }
        }

        // Moving down (-Y) - check if block is below us
        if (velocityY < 0 && other.max.y <= min.y + EPSILON) {
            float gap = other.max.y - min.y;
            if (gap > velocityY) {
                velocityY = Math.min(0, gap);
            }
        }

        return velocityY;
    }

    /**
     * Calculate collision offset on Z axis.
     * Returns adjusted velocity to prevent intersection.
     * 'other' is the static block, 'this' is the moving player.
     */
    public float clipZCollide(AABB other, float velocityZ) {
        // Must already overlap on X and Y axes for Z collision to matter
        if (other.max.x <= min.x + EPSILON || other.min.x >= max.x - EPSILON)
            return velocityZ;
        if (other.max.y <= min.y + EPSILON || other.min.y >= max.y - EPSILON)
            return velocityZ;

        // Moving forward (+Z) - check if block is in front of us
        if (velocityZ > 0 && other.min.z >= max.z - EPSILON) {
            float gap = other.min.z - max.z;
            if (gap < velocityZ) {
                velocityZ = Math.max(0, gap);
            }
        }

        // Moving backward (-Z) - check if block is behind us
        if (velocityZ < 0 && other.max.z <= min.z + EPSILON) {
            float gap = other.max.z - min.z;
            if (gap > velocityZ) {
                velocityZ = Math.min(0, gap);
            }
        }

        return velocityZ;
    }

    /**
     * Move the AABB in place.
     */
    public void move(float x, float y, float z) {
        min.add(x, y, z);
        max.add(x, y, z);
    }

    public Vector3f getMin() {
        return min;
    }

    public Vector3f getMax() {
        return max;
    }

    public float getWidth() {
        return max.x - min.x;
    }

    public float getHeight() {
        return max.y - min.y;
    }

    public float getDepth() {
        return max.z - min.z;
    }

    public Vector3f getCenter() {
        return new Vector3f(
                (min.x + max.x) / 2,
                (min.y + max.y) / 2,
                (min.z + max.z) / 2);
    }

    @Override
    public String toString() {
        return "AABB[" + min + " -> " + max + "]";
    }
}
