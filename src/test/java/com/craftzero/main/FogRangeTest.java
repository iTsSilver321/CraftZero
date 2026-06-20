package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FogRangeTest {
    @Test
    @DisplayName("Normal world fog should scale linearly with render distance")
    void normalFogTracksRenderDistanceChunks() {
        assertFogRange(2, 22.4f, 32.0f);
        assertFogRange(8, 89.6f, 128.0f);
        assertFogRange(16, 179.2f, 256.0f);
    }

    private static void assertFogRange(int chunks, float expectedStart, float expectedEnd) {
        float[] range = Main.normalFogRangeForRenderDistance(chunks);
        assertEquals(expectedStart, range[0], 0.0001f);
        assertEquals(expectedEnd, range[1], 0.0001f);
    }
}
