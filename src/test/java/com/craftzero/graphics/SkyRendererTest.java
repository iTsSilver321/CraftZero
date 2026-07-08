package com.craftzero.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkyRendererTest {
    @Test
    @DisplayName("Moon phase UVs should select the Release-era 4x2 phase sheet cells")
    void moonPhaseUvsSelectPhaseSheetCells() {
        assertArrayEquals(new float[] {
                0.0f, 0.0f,
                0.0f, 0.5f,
                0.25f, 0.5f,
                0.25f, 0.0f
        }, SkyRenderer.moonPhaseTexCoords(0), 0.0001f);

        assertArrayEquals(new float[] {
                0.75f, 0.0f,
                0.75f, 0.5f,
                1.0f, 0.5f,
                1.0f, 0.0f
        }, SkyRenderer.moonPhaseTexCoords(3), 0.0001f);

        assertArrayEquals(new float[] {
                0.0f, 0.5f,
                0.0f, 1.0f,
                0.25f, 1.0f,
                0.25f, 0.5f
        }, SkyRenderer.moonPhaseTexCoords(4), 0.0001f);

        assertArrayEquals(new float[] {
                0.75f, 0.5f,
                0.75f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.5f
        }, SkyRenderer.moonPhaseTexCoords(7), 0.0001f);
    }

    @Test
    @DisplayName("Moon phase selection should wrap to the old eight-phase cycle")
    void moonPhaseSelectionWrapsEightPhaseCycle() {
        assertEquals(0, SkyRenderer.normalizedMoonPhase(0));
        assertEquals(7, SkyRenderer.normalizedMoonPhase(7));
        assertEquals(0, SkyRenderer.normalizedMoonPhase(8));
        assertEquals(7, SkyRenderer.normalizedMoonPhase(-1));
    }
}
