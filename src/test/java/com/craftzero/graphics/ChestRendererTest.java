package com.craftzero.graphics;

import com.craftzero.world.Block;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestRendererTest {

    @Test
    @DisplayName("Chest lid renderer should use the Release-era cubic easing and right-angle hinge")
    void chestLidUsesReleaseEasing() {
        assertEquals(0.0f, ChestRenderer.lidRotationRadians(0.0f), 0.0001f);
        assertEquals((float) (Math.PI / 2.0), ChestRenderer.lidRotationRadians(1.0f), 0.0001f);
        assertEquals((float) (Math.PI / 2.0 * 0.875), ChestRenderer.lidRotationRadians(0.5f), 0.0001f);
    }

    @Test
    @DisplayName("Chest lid renderer should clamp invalid interpolation inputs")
    void chestLidEasingClampsInputs() {
        assertEquals(0.0f, ChestRenderer.lidRotationRadians(-0.25f), 0.0001f);
        assertEquals((float) (Math.PI / 2.0), ChestRenderer.lidRotationRadians(1.25f), 0.0001f);
    }

    @Test
    @DisplayName("Chest latch should sit on the front center for every facing")
    void chestLatchSitsOnFrontCenterForEveryFacing() {
        float cx = 4.5f;
        float y = 64.625f;
        float cz = 8.5f;
        float width = 0.875f;
        float depth = 0.875f;
        float halfDepth = 0.03125f;

        assertVectorEquals(new Vector3f(cx, y, cz - depth * 0.5f - halfDepth),
                transformedOrigin(ChestRenderer.latchModelForFacing(Block.FACE_NORTH, cx, y, cz, width, depth, 0.0f)));
        assertVectorEquals(new Vector3f(cx, y, cz + depth * 0.5f + halfDepth),
                transformedOrigin(ChestRenderer.latchModelForFacing(Block.FACE_SOUTH, cx, y, cz, width, depth, 0.0f)));
        assertVectorEquals(new Vector3f(cx + width * 0.5f + halfDepth, y, cz),
                transformedOrigin(ChestRenderer.latchModelForFacing(Block.FACE_EAST, cx, y, cz, width, depth, 0.0f)));
        assertVectorEquals(new Vector3f(cx - width * 0.5f - halfDepth, y, cz),
                transformedOrigin(ChestRenderer.latchModelForFacing(Block.FACE_WEST, cx, y, cz, width, depth, 0.0f)));
    }

    @Test
    @DisplayName("Large chest latch should center across the joined chest width")
    void largeChestLatchCentersAcrossJoinedChestWidth() {
        float cx = 12.0f;
        float y = 70.625f;
        float cz = 3.5f;
        float width = 1.875f;
        float depth = 0.875f;

        Vector3f latch = transformedOrigin(
                ChestRenderer.latchModelForFacing(Block.FACE_SOUTH, cx, y, cz, width, depth, 0.0f));

        assertVectorEquals(new Vector3f(cx, y, cz + depth * 0.5f + 0.03125f), latch);
    }

    @Test
    @DisplayName("Chest renderer should use Release-era single chest model texture boxes")
    void singleChestUsesReleaseTextureBoxes() {
        assertTextureBox(ChestRenderer.singleBodyTextureBox(), 0, 19, 14, 10, 14, 64, 64);
        assertTextureBox(ChestRenderer.singleLidTextureBox(), 0, 0, 14, 5, 14, 64, 64);
        assertTextureBox(ChestRenderer.singleLatchTextureBox(), 0, 0, 2, 4, 1, 64, 64);
    }

    @Test
    @DisplayName("Large chest renderer should use Release-era double chest model texture boxes")
    void largeChestUsesReleaseTextureBoxes() {
        assertTextureBox(ChestRenderer.largeBodyTextureBox(), 0, 19, 30, 10, 14, 128, 64);
        assertTextureBox(ChestRenderer.largeLidTextureBox(), 0, 0, 30, 5, 14, 128, 64);
        assertTextureBox(ChestRenderer.largeLatchTextureBox(), 0, 0, 2, 4, 1, 128, 64);
    }

    @Test
    @DisplayName("Chest mesh UVs should map faces to the Minecraft box unwrap instead of the full texture")
    void chestMeshUvsUseModelBoxUnwrap() {
        ChestRenderer.ChestBoxSpec body = ChestRenderer.singleBodyTextureBox();

        assertUvRect(body, Block.FACE_TOP, 14, 19, 28, 33);
        assertUvRect(body, Block.FACE_BOTTOM, 28, 19, 42, 33);
        assertUvRect(body, Block.FACE_NORTH, 14, 33, 28, 43);
        assertUvRect(body, Block.FACE_SOUTH, 42, 33, 56, 43);
        assertUvRect(body, Block.FACE_EAST, 28, 33, 42, 43);
        assertUvRect(body, Block.FACE_WEST, 0, 33, 14, 43);
    }

    @Test
    @DisplayName("Chest mesh data should produce a complete six-face box with normalized UVs")
    void chestMeshDataBuildsCompleteTexturedBox() {
        ChestRenderer.ChestMeshData data = ChestRenderer.largeBodyMeshData();

        assertEquals(24 * 3, data.positions().length);
        assertEquals(24 * 2, data.texCoords().length);
        assertEquals(24 * 3, data.normals().length);
        assertEquals(24 * 3, data.colors().length);
        assertEquals(6 * 6, data.indices().length);

        for (float coord : data.texCoords()) {
            assertTrue(coord >= 0.0f && coord <= 1.0f, "UV coordinate out of range: " + coord);
        }
    }

    private static Vector3f transformedOrigin(Matrix4f matrix) {
        Vector3f point = new Vector3f();
        matrix.transformPosition(point);
        return point;
    }

    private static void assertVectorEquals(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, 0.0001f);
        assertEquals(expected.y, actual.y, 0.0001f);
        assertEquals(expected.z, actual.z, 0.0001f);
    }

    private static void assertTextureBox(ChestRenderer.ChestBoxSpec spec, int textureU, int textureV,
            int width, int height, int depth, int textureWidth, int textureHeight) {
        assertEquals(textureU, spec.textureU());
        assertEquals(textureV, spec.textureV());
        assertEquals(width, spec.width());
        assertEquals(height, spec.height());
        assertEquals(depth, spec.depth());
        assertEquals(textureWidth, spec.textureWidth());
        assertEquals(textureHeight, spec.textureHeight());
    }

    private static void assertUvRect(ChestRenderer.ChestBoxSpec spec, int face,
            int x0, int y0, int x1, int y1) {
        float[] rect = ChestRenderer.uvRectForFace(spec, face);
        assertEquals(x0 / (float) spec.textureWidth(), rect[0], 0.0001f);
        assertEquals(y0 / (float) spec.textureHeight(), rect[1], 0.0001f);
        assertEquals(x1 / (float) spec.textureWidth(), rect[2], 0.0001f);
        assertEquals(y1 / (float) spec.textureHeight(), rect[3], 0.0001f);
    }
}
