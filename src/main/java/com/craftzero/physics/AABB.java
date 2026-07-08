package com.craftzero.physics;

import lombok.Getter;
import org.joml.Vector3f;

/**
 * Axis-Aligned Bounding Box for collision detection.
 * Uses Minecraft-style independent axis collision resolution.
 */
public class AABB {

    @Getter
    private Vector3f min;
    @Getter
    private Vector3f max;

    // Small epsilon to prevent floating-point precision issues
    private static final float EPSILON = 0.0001f;

    public AABB(Vector3f min, Vector3f max) {
        this(
                min == null ? 0.0f : min.x,
                min == null ? 0.0f : min.y,
                min == null ? 0.0f : min.z,
                max == null ? 0.0f : max.x,
                max == null ? 0.0f : max.y,
                max == null ? 0.0f : max.z);
    }

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min = new Vector3f(finiteOrZero(minX), finiteOrZero(minY), finiteOrZero(minZ));
        this.max = new Vector3f(finiteOrZero(maxX), finiteOrZero(maxY), finiteOrZero(maxZ));
        normalizeInPlace();
    }

    /**
     * Create AABB from center position and dimensions.
     */
    public static AABB fromCenter(Vector3f center, float width, float height, float depth) {
        float centerX = center == null ? 0.0f : finiteOrZero(center.x);
        float centerY = center == null ? 0.0f : finiteOrZero(center.y);
        float centerZ = center == null ? 0.0f : finiteOrZero(center.z);
        float safeWidth = Math.max(0.0f, finiteOrZero(width));
        float safeHeight = Math.max(0.0f, finiteOrZero(height));
        float safeDepth = Math.max(0.0f, finiteOrZero(depth));
        float halfW = safeWidth / 2;
        float halfD = safeDepth / 2;
        return new AABB(
                centerX - halfW, centerY, centerZ - halfD,
                centerX + halfW, centerY + safeHeight, centerZ + halfD);
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
        if (other == null || !isFinite() || !other.isFinite()) {
            return false;
        }
        normalizeInPlace();
        other.normalizeInPlace();
        return (min.x < other.max.x && max.x > other.min.x) &&
                (min.y < other.max.y && max.y > other.min.y) &&
                (min.z < other.max.z && max.z > other.min.z);
    }

    /**
     * Check if this AABB contains a point.
     */
    public boolean contains(Vector3f point) {
        if (!isFiniteVector(point) || !isFinite()) {
            return false;
        }
        normalizeInPlace();
        return point.x >= min.x && point.x <= max.x &&
                point.y >= min.y && point.y <= max.y &&
                point.z >= min.z && point.z <= max.z;
    }

    /**
     * Check if this AABB contains another AABB.
     */
    public boolean contains(AABB other) {
        if (other == null || !isFinite() || !other.isFinite()) {
            return false;
        }
        normalizeInPlace();
        other.normalizeInPlace();
        return other.min.x >= min.x && other.max.x <= max.x &&
                other.min.y >= min.y && other.max.y <= max.y &&
                other.min.z >= min.z && other.max.z <= max.z;
    }

    /**
     * Expand the AABB by the given amount in all directions.
     */
    public AABB expand(float amount) {
        normalizeInPlace();
        float safeAmount = finiteOrZero(amount);
        return new AABB(
                min.x - safeAmount, min.y - safeAmount, min.z - safeAmount,
                max.x + safeAmount, max.y + safeAmount, max.z + safeAmount);
    }

    /**
     * Offset the AABB by the given vector.
     */
    public AABB offset(Vector3f offset) {
        normalizeInPlace();
        float offsetX = offset == null ? 0.0f : finiteOrZero(offset.x);
        float offsetY = offset == null ? 0.0f : finiteOrZero(offset.y);
        float offsetZ = offset == null ? 0.0f : finiteOrZero(offset.z);
        return new AABB(
                min.x + offsetX, min.y + offsetY, min.z + offsetZ,
                max.x + offsetX, max.y + offsetY, max.z + offsetZ);
    }

    /**
     * Offset the AABB by the given values.
     */
    public AABB offset(float x, float y, float z) {
        normalizeInPlace();
        float offsetX = finiteOrZero(x);
        float offsetY = finiteOrZero(y);
        float offsetZ = finiteOrZero(z);
        return new AABB(
                min.x + offsetX, min.y + offsetY, min.z + offsetZ,
                max.x + offsetX, max.y + offsetY, max.z + offsetZ);
    }

    /**
     * Calculate collision offset on X axis.
     * Returns adjusted velocity to prevent intersection.
     * 'other' is the static block, 'this' is the moving player.
     */
    public float clipXCollide(AABB other, float velocityX) {
        if (!Float.isFinite(velocityX)) {
            return 0.0f;
        }
        if (other == null || !isFinite() || !other.isFinite()) {
            return velocityX;
        }
        normalizeInPlace();
        other.normalizeInPlace();

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
        if (!Float.isFinite(velocityY)) {
            return 0.0f;
        }
        if (other == null || !isFinite() || !other.isFinite()) {
            return velocityY;
        }
        normalizeInPlace();
        other.normalizeInPlace();

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
        if (!Float.isFinite(velocityZ)) {
            return 0.0f;
        }
        if (other == null || !isFinite() || !other.isFinite()) {
            return velocityZ;
        }
        normalizeInPlace();
        other.normalizeInPlace();

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
        normalizeInPlace();
        min.add(finiteOrZero(x), finiteOrZero(y), finiteOrZero(z));
        max.add(finiteOrZero(x), finiteOrZero(y), finiteOrZero(z));
        normalizeInPlace();
    }

    // Getters (min/max generated by Lombok, calculated ones below)

    public float getWidth() {
        normalizeInPlace();
        return max.x - min.x;
    }

    public float getHeight() {
        normalizeInPlace();
        return max.y - min.y;
    }

    public float getDepth() {
        normalizeInPlace();
        return max.z - min.z;
    }

    public Vector3f getCenter() {
        normalizeInPlace();
        return new Vector3f(
                (min.x + max.x) / 2,
                (min.y + max.y) / 2,
                (min.z + max.z) / 2);
    }

    public boolean isFinite() {
        return isFiniteVector(min) && isFiniteVector(max);
    }

    @Override
    public String toString() {
        return "AABB[" + min + " -> " + max + "]";
    }

    private void normalizeInPlace() {
        min.x = finiteOrZero(min.x);
        min.y = finiteOrZero(min.y);
        min.z = finiteOrZero(min.z);
        max.x = finiteOrZero(max.x);
        max.y = finiteOrZero(max.y);
        max.z = finiteOrZero(max.z);

        if (min.x > max.x) {
            float swap = min.x;
            min.x = max.x;
            max.x = swap;
        }
        if (min.y > max.y) {
            float swap = min.y;
            min.y = max.y;
            max.y = swap;
        }
        if (min.z > max.z) {
            float swap = min.z;
            min.z = max.z;
            max.z = swap;
        }
    }

    private static boolean isFiniteVector(Vector3f value) {
        return value != null
                && Float.isFinite(value.x)
                && Float.isFinite(value.y)
                && Float.isFinite(value.z);
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }
}
