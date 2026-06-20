package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FluidStateTest {
    @Test
    @DisplayName("Fluid metadata should expose Release 1.0 source, decay, falling, and render height")
    void fluidMetadataHelpersMatchReleaseOneRules() {
        assertTrue(FluidState.isSource(0));
        assertFalse(FluidState.isSource(8));
        assertFalse(FluidState.isFalling(7));
        assertTrue(FluidState.isFalling(8));
        assertEquals(0, FluidState.flowDecay(8));
        assertEquals(4.0f / 9.0f, FluidState.height(4), 0.0001f);
        assertEquals(8.0f / 9.0f, FluidState.height(12), 0.0001f);
    }
}
