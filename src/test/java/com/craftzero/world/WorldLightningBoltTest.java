package com.craftzero.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldLightningBoltTest {
    @Test
    void generatedLightningBoltsUseRepeatedFlashWindows() {
        WorldLightningBolt bolt = new WorldLightningBolt(0.5f, 90.0f, 0.5f, new Random(1234L));

        List<WorldLightningBolt.FlashWindow> windows = bolt.getFlashWindows();
        assertTrue(windows.size() >= 2, "Release-era lightning should have a reflash after the first strike");
        float previousEnd = 0.0f;
        for (WorldLightningBolt.FlashWindow window : windows) {
            assertTrue(window.startTick() >= previousEnd);
            assertTrue(window.endTick() > window.startTick());
            previousEnd = window.endTick();
        }
        assertTrue(bolt.getLifetimeTicks() > windows.get(windows.size() - 1).endTick());
    }

    @Test
    void lightningAlphaTurnsOffBetweenPulsesAndRelights() {
        WorldLightningBolt bolt = new WorldLightningBolt(0.5f, 90.0f, 0.5f,
                List.of(new WorldLightningBolt.Segment(0.5f, 100.0f, 0.5f, 0.5f, 90.0f, 0.5f)),
                List.of(
                        new WorldLightningBolt.FlashWindow(0.0f, 3.0f),
                        new WorldLightningBolt.FlashWindow(5.0f, 6.0f)),
                7);

        assertEquals(1.0f, bolt.getAlpha(0.0f), 0.0001f);
        assertTrue(bolt.getAlpha(2.0f) > 0.0f);
        assertEquals(0.0f, bolt.getAlpha(3.0f), 0.0001f);
        assertTrue(bolt.getAlpha(5.0f) > 0.0f);
        assertEquals(0.0f, bolt.getAlpha(6.0f), 0.0001f);
    }

    @Test
    void lightningBoltExpiresAfterTheLastFlashTail() {
        WorldLightningBolt bolt = new WorldLightningBolt(0.5f, 90.0f, 0.5f,
                List.of(new WorldLightningBolt.Segment(0.5f, 100.0f, 0.5f, 0.5f, 90.0f, 0.5f)),
                List.of(new WorldLightningBolt.FlashWindow(0.0f, 2.0f)),
                3);

        assertFalse(bolt.update(2.0f / 20.0f));
        assertTrue(bolt.update(1.0f / 20.0f));
    }
}
