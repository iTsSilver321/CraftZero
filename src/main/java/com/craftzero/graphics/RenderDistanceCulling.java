package com.craftzero.graphics;

import com.craftzero.entity.Entity;
import com.craftzero.world.tile.BlockPos;

/**
 * Shared render-distance checks for visuals that should follow the active
 * Far/Normal/Short/Tiny camera clip instead of old fixed local caps.
 */
public final class RenderDistanceCulling {
    private RenderDistanceCulling() {
    }

    public static boolean isEntityTooFar(Camera camera, Entity entity, float fallbackRange) {
        if (entity == null) {
            return true;
        }
        return isPointTooFar(camera, entity.getX(), entity.getY(), entity.getZ(), fallbackRange);
    }

    public static boolean isBlockTooFar(Camera camera, BlockPos pos, float fallbackRange) {
        if (pos == null) {
            return true;
        }
        return isPointTooFar(camera, pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f, fallbackRange);
    }

    public static boolean isPointTooFar(Camera camera, float x, float y, float z, float fallbackRange) {
        if (camera == null || camera.getPosition() == null || !allFinite(x, y, z)) {
            return true;
        }
        float max = activeRange(camera, fallbackRange);
        if (!Float.isFinite(max) || max <= 0.0f) {
            return true;
        }
        float dx = x - camera.getPosition().x;
        float dy = y - camera.getPosition().y;
        float dz = z - camera.getPosition().z;
        if (!allFinite(dx, dy, dz)) {
            return true;
        }
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    private static float activeRange(Camera camera, float fallbackRange) {
        float fallback = Float.isFinite(fallbackRange) && fallbackRange > 0.0f ? fallbackRange : 64.0f;
        float far = camera == null ? fallback : camera.getFarPlane();
        return Float.isFinite(far) && far > 0.0f ? far : fallback;
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
