package com.craftzero.graphics;

import com.craftzero.world.tile.BlockPos;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SignTextRendererTest {
    @Test
    void glyphUvUsesSixteenBySixteenFontAtlasCells() {
        assertArrayEquals(new float[] { 1.0f / 16.0f, 4.0f / 16.0f, 2.0f / 16.0f, 5.0f / 16.0f },
                SignTextRenderer.glyphUv('A'), 0.0001f);
    }

    @Test
    void signLinesAreCenteredAcrossTheBoard() {
        assertEquals(-2.0f * SignTextRenderer.GLYPH_WIDTH, SignTextRenderer.lineStartX("Text"), 0.0001f);
        assertEquals(SignTextRenderer.FIRST_LINE_Y - 2.0f * SignTextRenderer.LINE_SPACING,
                SignTextRenderer.lineY(2), 0.0001f);
    }

    @Test
    void wallSignTextSitsOnTheRenderedSignFace() {
        Vector3f northFace = SignTextRenderer.wallSignBaseMatrix(new BlockPos(4, 70, -2), 2)
                .getTranslation(new Vector3f());
        assertEquals(4.5f, northFace.x, 0.0001f);
        assertEquals(70.0f + SignTextRenderer.WALL_SIGN_TEXT_Y, northFace.y, 0.0001f);
        assertEquals(-2.0f + 0.875f - SignTextRenderer.WALL_SIGN_FACE_EPSILON, northFace.z, 0.0001f);

        Vector3f standing = SignTextRenderer.standingSignBaseMatrix(new BlockPos(4, 70, -2), 8)
                .getTranslation(new Vector3f());
        assertEquals(4.5f, standing.x, 0.0001f);
        assertEquals(70.0f + SignTextRenderer.STANDING_SIGN_TEXT_Y, standing.y, 0.0001f);
        assertEquals(-1.5f + SignTextRenderer.STANDING_SIGN_FACE_OFFSET, standing.z, 0.0001f);
    }
}
