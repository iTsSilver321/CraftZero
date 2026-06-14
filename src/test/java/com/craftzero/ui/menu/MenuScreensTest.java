package com.craftzero.ui.menu;

import com.craftzero.main.GameSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MenuScreensTest {

    @Test
    @DisplayName("Video settings should expose render distance as a 2-16 chunk slider")
    void videoSettingsUsesRenderDistanceSlider() {
        GameSettings settings = GameSettings.defaults();
        settings.setRenderDistance(8);

        BaseMenuScreen screen = MenuScreens.video(854, 480, settings, () -> {
        });

        MenuComponent component = screen.components().stream()
                .filter(candidate -> candidate.id().equals("render-distance"))
                .findFirst()
                .orElse(null);

        assertNotNull(component);
        MenuSlider slider = assertInstanceOf(MenuSlider.class, component);
        assertEquals(GameSettings.MIN_RENDER_DISTANCE_CHUNKS, slider.min(), 0.0001);
        assertEquals(GameSettings.MAX_RENDER_DISTANCE_CHUNKS, slider.max(), 0.0001);
        assertEquals(1.0, slider.step(), 0.0001);
    }
}
