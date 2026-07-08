package com.craftzero.combat;

import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.physics.AABB;
import com.craftzero.world.World;
import org.joml.Vector3f;

/**
 * Samples how much of a target's bounds is visible from an explosion center.
 */
public final class ExplosionExposure {
    private static final float SAMPLE_EPSILON = 1.0e-6f;

    private ExplosionExposure() {
    }

    public static float sample(World world, float explosionX, float explosionY, float explosionZ, AABB bounds) {
        if (world == null || bounds == null || !bounds.isFinite()
                || !Float.isFinite(explosionX) || !Float.isFinite(explosionY) || !Float.isFinite(explosionZ)) {
            return 0.0f;
        }

        AABB safeBounds = new AABB(bounds.getMin(), bounds.getMax());
        Vector3f min = safeBounds.getMin();
        Vector3f max = safeBounds.getMax();
        float xStep = sampleStep(max.x - min.x);
        float yStep = sampleStep(max.y - min.y);
        float zStep = sampleStep(max.z - min.z);
        if (!Float.isFinite(xStep) || !Float.isFinite(yStep) || !Float.isFinite(zStep)
                || xStep <= 0.0f || yStep <= 0.0f || zStep <= 0.0f) {
            return 0.0f;
        }

        int clear = 0;
        int total = 0;
        float xOffset = horizontalSampleOffset(xStep);
        float zOffset = horizontalSampleOffset(zStep);
        for (float fx = 0.0f; fx <= 1.0f + SAMPLE_EPSILON; fx += xStep) {
            for (float fy = 0.0f; fy <= 1.0f + SAMPLE_EPSILON; fy += yStep) {
                for (float fz = 0.0f; fz <= 1.0f + SAMPLE_EPSILON; fz += zStep) {
                    float sx = lerp(min.x, max.x, clamp01(fx)) + xOffset;
                    float sy = lerp(min.y, max.y, clamp01(fy));
                    float sz = lerp(min.z, max.z, clamp01(fz)) + zOffset;
                    total++;
                    if (LineOfSightUtil.hasLineOfSight(world, sx, sy, sz, explosionX, explosionY, explosionZ)) {
                        clear++;
                    }
                }
            }
        }

        return total == 0 ? 0.0f : clear / (float) total;
    }

    private static float sampleStep(float size) {
        if (!Float.isFinite(size) || size < 0.0f) {
            return Float.NaN;
        }
        return 1.0f / (size * 2.0f + 1.0f);
    }

    private static float horizontalSampleOffset(float step) {
        if (!Float.isFinite(step) || step <= 0.0f) {
            return 0.0f;
        }
        return (float) ((1.0d - Math.floor(1.0d / step) * step) * 0.5d);
    }

    private static float lerp(float min, float max, float factor) {
        return min + (max - min) * factor;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
