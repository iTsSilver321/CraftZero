package com.craftzero.combat;

import com.craftzero.entity.ai.LineOfSightUtil;
import com.craftzero.physics.AABB;
import com.craftzero.world.World;
import org.joml.Vector3f;

/**
 * Samples how much of a target's bounds is visible from an explosion center.
 */
public final class ExplosionExposure {
    private static final float EDGE_INSET = 0.001f;

    private ExplosionExposure() {
    }

    public static float sample(World world, float explosionX, float explosionY, float explosionZ, AABB bounds) {
        if (world == null || bounds == null) {
            return 0.0f;
        }

        Vector3f min = bounds.getMin();
        Vector3f max = bounds.getMax();
        float[] xs = sampleAxis(min.x, max.x);
        float[] ys = sampleAxis(min.y, max.y);
        float[] zs = sampleAxis(min.z, max.z);

        int clear = 0;
        int total = 0;
        for (float sx : xs) {
            for (float sy : ys) {
                for (float sz : zs) {
                    total++;
                    if (LineOfSightUtil.hasLineOfSight(world, explosionX, explosionY, explosionZ, sx, sy, sz)) {
                        clear++;
                    }
                }
            }
        }

        return total == 0 ? 0.0f : clear / (float) total;
    }

    private static float[] sampleAxis(float min, float max) {
        if (max - min <= EDGE_INSET * 2.0f) {
            return new float[] { (min + max) * 0.5f };
        }
        return new float[] {
                min + EDGE_INSET,
                (min + max) * 0.5f,
                max - EDGE_INSET
        };
    }
}
