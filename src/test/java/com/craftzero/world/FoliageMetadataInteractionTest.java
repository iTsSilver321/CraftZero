package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoliageMetadataInteractionTest {
    @Test
    @DisplayName("Tall grass metadata should select shrub, grass, and fern textures")
    void tallGrassMetadataSelectsReleaseOneTextures() {
        assertEquals(55, textureIndex(BlockType.TALL_GRASS, Block.FACE_NORTH, 0));
        assertEquals(39, textureIndex(BlockType.TALL_GRASS, Block.FACE_NORTH, 1));
        assertEquals(56, textureIndex(BlockType.TALL_GRASS, Block.FACE_NORTH, 2));
        assertEquals(39, textureIndex(BlockType.TALL_GRASS, Block.FACE_NORTH, 3));
    }

    @Test
    @DisplayName("Huge mushroom metadata should select Release 1.0 cap, pore, and stem textures")
    void hugeMushroomMetadataSelectsReleaseOneTextures() {
        assertEquals(126, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_TOP, 5));
        assertEquals(125, textureIndex(BlockType.RED_MUSHROOM_BLOCK, Block.FACE_TOP, 5));

        assertEquals(126, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_NORTH, 2));
        assertEquals(142, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_SOUTH, 2));
        assertEquals(126, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_WEST, 4));
        assertEquals(142, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_EAST, 4));
        assertEquals(126, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_EAST, 6));

        assertEquals(141, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_NORTH, 10));
        assertEquals(142, textureIndex(BlockType.BROWN_MUSHROOM_BLOCK, Block.FACE_TOP, 10));
        assertEquals(125, textureIndex(BlockType.RED_MUSHROOM_BLOCK, Block.FACE_BOTTOM, 14));
        assertEquals(141, textureIndex(BlockType.RED_MUSHROOM_BLOCK, Block.FACE_TOP, 15));
    }

    private static int textureIndex(BlockType type, int face, int metadata) {
        float[] coords = type.getTextureCoords(face, metadata);
        int col = Math.round((coords[0] - 0.001f) / BlockType.TEXTURE_SIZE);
        int row = Math.round((coords[1] - 0.001f) / BlockType.TEXTURE_SIZE);
        return row * BlockType.ATLAS_SIZE + col;
    }
}
