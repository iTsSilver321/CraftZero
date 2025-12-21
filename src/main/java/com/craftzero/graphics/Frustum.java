package com.craftzero.graphics;

import org.joml.Matrix4f;

/**
 * Frustum for view frustum culling.
 * Extracts 6 planes from view-projection matrix and tests AABBs for visibility.
 */
public class Frustum {

    // The 6 frustum planes: left, right, bottom, top, near, far
    // Each plane is (a, b, c, d) where ax + by + cz + d = 0
    private final float[][] planes = new float[6][4];

    // Plane indices
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;

    /**
     * Update the frustum planes from the combined view-projection matrix.
     * Uses Gribb/Hartmann plane extraction method.
     */
    public void update(Matrix4f viewProjection) {
        // Get matrix elements (column-major)
        float m00 = viewProjection.m00(), m01 = viewProjection.m01(), m02 = viewProjection.m02(),
                m03 = viewProjection.m03();
        float m10 = viewProjection.m10(), m11 = viewProjection.m11(), m12 = viewProjection.m12(),
                m13 = viewProjection.m13();
        float m20 = viewProjection.m20(), m21 = viewProjection.m21(), m22 = viewProjection.m22(),
                m23 = viewProjection.m23();
        float m30 = viewProjection.m30(), m31 = viewProjection.m31(), m32 = viewProjection.m32(),
                m33 = viewProjection.m33();

        // Left plane: row4 + row1
        setPlane(LEFT, m03 + m00, m13 + m10, m23 + m20, m33 + m30);

        // Right plane: row4 - row1
        setPlane(RIGHT, m03 - m00, m13 - m10, m23 - m20, m33 - m30);

        // Bottom plane: row4 + row2
        setPlane(BOTTOM, m03 + m01, m13 + m11, m23 + m21, m33 + m31);

        // Top plane: row4 - row2
        setPlane(TOP, m03 - m01, m13 - m11, m23 - m21, m33 - m31);

        // Near plane: row4 + row3
        setPlane(NEAR, m03 + m02, m13 + m12, m23 + m22, m33 + m32);

        // Far plane: row4 - row3
        setPlane(FAR, m03 - m02, m13 - m12, m23 - m22, m33 - m32);
    }

    /**
     * Set a plane and normalize it.
     */
    private void setPlane(int index, float a, float b, float c, float d) {
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        if (length > 0) {
            planes[index][0] = a / length;
            planes[index][1] = b / length;
            planes[index][2] = c / length;
            planes[index][3] = d / length;
        }
    }

    /**
     * Test if an AABB is visible in the frustum.
     * Uses the "positive vertex" optimization for fast rejection.
     *
     * @return true if the box is at least partially inside the frustum
     */
    public boolean isBoxVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            float a = planes[i][0];
            float b = planes[i][1];
            float c = planes[i][2];
            float d = planes[i][3];

            // Get the "positive" vertex (furthest in direction of normal)
            float px = a >= 0 ? maxX : minX;
            float py = b >= 0 ? maxY : minY;
            float pz = c >= 0 ? maxZ : minZ;

            // If positive vertex is outside, whole box is outside
            if (a * px + b * py + c * pz + d < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience method to test a chunk's visibility.
     * Chunks are 16x256x16 blocks.
     *
     * @param chunkWorldX World X coordinate of chunk origin
     * @param chunkWorldZ World Z coordinate of chunk origin
     * @return true if the chunk is visible
     */
    public boolean isChunkVisible(int chunkWorldX, int chunkWorldZ) {
        return isBoxVisible(
                chunkWorldX, 0, chunkWorldZ,
                chunkWorldX + 16, 256, chunkWorldZ + 16);
    }
}
