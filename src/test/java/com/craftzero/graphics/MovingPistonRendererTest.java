package com.craftzero.graphics;

import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovingPistonRendererTest {

    @Test
    @DisplayName("Moving sticky piston heads should render with sticky head metadata")
    void movingStickyPistonHeadPreservesStickyRenderMetadata() {
        World.MovingPistonState state = new World.MovingPistonState(
                1, 100, 0, Block.FACE_EAST,
                BlockType.PISTON_HEAD, Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                BlockType.PISTON_HEAD, Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                0.0f, 100.0f, 0.0f,
                1.0f, 100.0f, 0.0f,
                0L, false);

        int metadata = MovingPistonRenderer.renderMetadata(state);

        assertEquals(Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT, metadata);
        assertEquals(106, textureIndex(BlockType.PISTON_HEAD, Block.FACE_EAST, metadata));
    }

    @Test
    @DisplayName("Moving piston head render metadata should keep current facing authoritative")
    void movingPistonHeadRenderMetadataUsesStateFacing() {
        World.MovingPistonState state = new World.MovingPistonState(
                1, 100, 0, Block.FACE_WEST,
                BlockType.PISTON_HEAD, Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                BlockType.PISTON_HEAD, Block.FACE_WEST | RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                2.0f, 100.0f, 0.0f,
                1.0f, 100.0f, 0.0f,
                0L, false);

        int metadata = MovingPistonRenderer.renderMetadata(state);

        assertEquals(Block.FACE_WEST | RedstoneEngine.PISTON_HEAD_STICKY_BIT, metadata);
    }

    private static int textureIndex(BlockType type, int face, int metadata) {
        float[] uv = type.getTextureCoords(face, metadata);
        int col = Math.round((uv[0] - 0.001f) * 16.0f);
        int row = Math.round((uv[1] - 0.001f) * 16.0f);
        return row * 16 + col;
    }
}
