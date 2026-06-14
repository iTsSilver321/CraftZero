package com.craftzero.entity;

import com.craftzero.physics.AABB;
import org.joml.Vector3f;

final class ArrowEntityRay {
    private ArrowEntityRay() {
    }

    static float intersects(Vector3f origin, Vector3f direction, AABB box) {
        float tMin = 0.0f;
        float tMax = Float.MAX_VALUE;
        float[] starts = { origin.x, origin.y, origin.z };
        float[] dirs = { direction.x, direction.y, direction.z };
        float[] mins = { box.getMin().x, box.getMin().y, box.getMin().z };
        float[] maxs = { box.getMax().x, box.getMax().y, box.getMax().z };

        for (int i = 0; i < 3; i++) {
            float dir = dirs[i];
            if (Math.abs(dir) < 0.0001f) {
                if (starts[i] < mins[i] || starts[i] > maxs[i]) {
                    return -1.0f;
                }
                continue;
            }
            float invD = 1.0f / dir;
            float t0 = (mins[i] - starts[i]) * invD;
            float t1 = (maxs[i] - starts[i]) * invD;
            if (invD < 0) {
                float tmp = t0;
                t0 = t1;
                t1 = tmp;
            }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax) {
                return -1.0f;
            }
        }
        return tMin;
    }
}
