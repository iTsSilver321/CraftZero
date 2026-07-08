package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRendererFirstPersonPoseTest {
    private static final int HELD_VERTEX_FLOATS = 11;

    @Test
    @DisplayName("Bow drawing should use a distinct pulled first-person pose")
    void bowDrawUsesPulledPose() {
        PlayerRenderer.FirstPersonUseTransform start = PlayerRenderer.firstPersonUseTransform(
                ItemType.BOW, false, false, true, 0.0f);
        PlayerRenderer.FirstPersonUseTransform full = PlayerRenderer.firstPersonUseTransform(
                ItemType.BOW, false, false, true, 1.0f);

        assertEquals(PlayerRenderer.FirstPersonUsePose.BOW_DRAW, start.pose());
        assertEquals(PlayerRenderer.FirstPersonUsePose.BOW_DRAW, full.pose());
        assertTrue(full.translateX() < start.translateX(), "A drawn bow should pull farther across the screen");
        assertTrue(full.translateZ() < start.translateZ(), "A drawn bow should move deeper toward the camera view");
        assertTrue(full.rotateX() < start.rotateX(), "A drawn bow should pitch more sharply than the start pose");
        assertTrue(full.scaleY() > start.scaleY(), "The held bow should gain the drawn-bow stretch");
    }

    @Test
    @DisplayName("Sword blocking should not depend on generic use progress")
    void blockingUsesGuardPoseWithoutProgress() {
        PlayerRenderer.FirstPersonUseTransform transform = PlayerRenderer.firstPersonUseTransform(
                ItemType.DIAMOND_SWORD, true, false, false, 0.0f);

        assertEquals(PlayerRenderer.FirstPersonUsePose.BLOCK, transform.pose());
        assertTrue(transform.rotateX() <= -70.0f, "Blocking should tilt the sword into a guard pose");
        assertTrue(transform.translateY() > 0.0f, "Blocking should lift the held item into view");
    }

    @Test
    @DisplayName("Eating and drinking should lift the item toward the mouth")
    void eatingDrinkingUsesMouthPose() {
        PlayerRenderer.FirstPersonUseTransform transform = PlayerRenderer.firstPersonUseTransform(
                ItemType.BREAD, false, true, false, 0.5f);

        assertEquals(PlayerRenderer.FirstPersonUsePose.EAT_DRINK, transform.pose());
        assertTrue(transform.translateY() > 0.1f, "Consumption should lift the held item");
        assertTrue(transform.translateZ() < -0.05f, "Consumption should bring the held item inward");
        assertTrue(transform.rotateY() > 0.0f, "Consumption should turn the item toward the mouth");
    }

    @Test
    @DisplayName("Idle and generic use poses should remain separate")
    void idleAndGenericUseStaySeparate() {
        PlayerRenderer.FirstPersonUseTransform idle = PlayerRenderer.firstPersonUseTransform(
                ItemType.STONE, false, false, false, 0.0f);
        PlayerRenderer.FirstPersonUseTransform generic = PlayerRenderer.firstPersonUseTransform(
                ItemType.STONE, false, false, false, 0.5f);

        assertEquals(PlayerRenderer.FirstPersonUsePose.NONE, idle.pose());
        assertEquals(PlayerRenderer.FirstPersonUsePose.GENERIC_USE, generic.pose());
        assertTrue(generic.translateZ() < 0.0f, "Generic use should keep the old forward jab motion");
    }

    @Test
    @DisplayName("Drawing state should only trigger bow pose for an actual bow")
    void bowDrawPoseRequiresBowItem() {
        PlayerRenderer.FirstPersonUseTransform transform = PlayerRenderer.firstPersonUseTransform(
                ItemType.STICK, false, false, true, 1.0f);

        assertEquals(PlayerRenderer.FirstPersonUsePose.GENERIC_USE, transform.pose());
    }

    @Test
    @DisplayName("Held block cubes should use terrain UVs for each rendered face")
    void heldBlockMeshesUseFaceSpecificTerrainUvs() {
        float[] vertices = PlayerRenderer.heldBlockVertices(BlockType.FURNACE, 3);

        assertFaceUvs(vertices, Block.FACE_TOP, BlockType.FURNACE, 3);
        assertFaceUvs(vertices, Block.FACE_BOTTOM, BlockType.FURNACE, 3);
        assertFaceUvs(vertices, Block.FACE_NORTH, BlockType.FURNACE, 3);
        assertFaceUvs(vertices, Block.FACE_SOUTH, BlockType.FURNACE, 3);
        assertNotEquals(firstFaceU(vertices, Block.FACE_NORTH), firstFaceU(vertices, Block.FACE_SOUTH),
                "A held furnace should show its front texture on the facing side, not one repeated side tile");
    }

    @Test
    @DisplayName("Held block cubes should preserve item metadata variants")
    void heldBlockMeshesPreserveItemMetadataVariants() {
        float[] oakLog = PlayerRenderer.heldBlockVertices(BlockType.OAK_LOG, 0);
        float[] spruceLog = PlayerRenderer.heldBlockVertices(BlockType.OAK_LOG, 1);
        float[] redWool = PlayerRenderer.heldBlockVertices(BlockType.WHITE_WOOL, ItemType.RED_WOOL.getDataValue());

        assertFaceUvs(spruceLog, Block.FACE_NORTH, BlockType.OAK_LOG, 1);
        assertFaceUvs(redWool, Block.FACE_TOP, BlockType.WHITE_WOOL, ItemType.RED_WOOL.getDataValue());
        assertNotEquals(firstFaceV(oakLog, Block.FACE_NORTH), firstFaceV(spruceLog, Block.FACE_NORTH),
                "Spruce log block items should no longer render with oak side UVs");
    }

    private static void assertFaceUvs(float[] vertices, int face, BlockType type, int metadata) {
        float[] expected = Block.getFaceTexCoords(type, face, metadata);
        int base = face * 4 * HELD_VERTEX_FLOATS;
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(expected[vertex * 2], vertices[base + vertex * HELD_VERTEX_FLOATS + 3], 0.000001f);
            assertEquals(expected[vertex * 2 + 1], vertices[base + vertex * HELD_VERTEX_FLOATS + 4], 0.000001f);
        }
    }

    private static float firstFaceU(float[] vertices, int face) {
        return vertices[face * 4 * HELD_VERTEX_FLOATS + 3];
    }

    private static float firstFaceV(float[] vertices, int face) {
        return vertices[face * 4 * HELD_VERTEX_FLOATS + 4];
    }
}
