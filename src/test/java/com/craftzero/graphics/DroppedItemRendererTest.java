package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DroppedItemRendererTest {
    @Test
    @DisplayName("Dropped block item cubes should preserve variant metadata")
    void droppedBlockCubeVerticesUseItemMetadata() {
        float[] oakLog = DroppedItemRenderer.blockCubeVertices(ItemType.OAK_LOG);
        float[] spruceLog = DroppedItemRenderer.blockCubeVertices(ItemType.SPRUCE_LOG);
        float[] redWool = DroppedItemRenderer.blockCubeVertices(ItemType.RED_WOOL);

        assertFaceUvs(spruceLog, Block.FACE_NORTH, BlockType.OAK_LOG, ItemType.SPRUCE_LOG.getPlacedBlockMetadata());
        assertFaceUvs(redWool, Block.FACE_TOP, BlockType.WHITE_WOOL, ItemType.RED_WOOL.getPlacedBlockMetadata());
        assertNotEquals(firstFaceV(oakLog, Block.FACE_NORTH), firstFaceV(spruceLog, Block.FACE_NORTH),
                "Dropped spruce logs should render with spruce bark instead of oak bark");
    }

    @Test
    @DisplayName("Dropped furnace item cubes should keep face-specific terrain UVs")
    void droppedBlockCubeVerticesUseFaceSpecificUvs() {
        float[] furnace = DroppedItemRenderer.blockCubeVertices(ItemType.FURNACE);

        assertFaceUvs(furnace, Block.FACE_TOP, BlockType.FURNACE, ItemType.FURNACE.getPlacedBlockMetadata());
        assertFaceUvs(furnace, Block.FACE_BOTTOM, BlockType.FURNACE, ItemType.FURNACE.getPlacedBlockMetadata());
        assertFaceUvs(furnace, Block.FACE_NORTH, BlockType.FURNACE, ItemType.FURNACE.getPlacedBlockMetadata());
        assertNotEquals(firstFaceU(furnace, Block.FACE_TOP), firstFaceU(furnace, Block.FACE_NORTH),
                "Dropped furnaces should not use one repeated side texture for every face");
    }

    private static void assertFaceUvs(float[] vertices, int face, BlockType type, int metadata) {
        float[] expected = Block.getFaceTexCoords(type, face, metadata);
        int base = face * 4 * DroppedItemRenderer.ITEM_VERTEX_FLOATS;
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(expected[vertex * 2],
                    vertices[base + vertex * DroppedItemRenderer.ITEM_VERTEX_FLOATS + 3],
                    0.000001f);
            assertEquals(expected[vertex * 2 + 1],
                    vertices[base + vertex * DroppedItemRenderer.ITEM_VERTEX_FLOATS + 4],
                    0.000001f);
        }
    }

    private static float firstFaceU(float[] vertices, int face) {
        return vertices[face * 4 * DroppedItemRenderer.ITEM_VERTEX_FLOATS + 3];
    }

    private static float firstFaceV(float[] vertices, int face) {
        return vertices[face * 4 * DroppedItemRenderer.ITEM_VERTEX_FLOATS + 4];
    }
}
