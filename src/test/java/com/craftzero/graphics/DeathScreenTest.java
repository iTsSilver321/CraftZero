package com.craftzero.graphics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathScreenTest {
    @Test
    @DisplayName("Death screen score text should use the Release-era label")
    void deathScreenScoreTextUsesReleaseLabel() {
        assertEquals("Score: 4625", DeathScreen.scoreText(4625));
        assertEquals("Score: 0", DeathScreen.scoreText(-3));
    }

    @Test
    @DisplayName("Death screen should expose Release-era respawn and title menu hitboxes")
    void deathScreenButtonsUseReleaseLayout() {
        DeathScreen.ButtonBounds respawn = DeathScreen.respawnButtonBounds(1280, 720);
        DeathScreen.ButtonBounds title = DeathScreen.titleMenuButtonBounds(1280, 720);
        DeathScreen.ButtonBounds deleteWorld = DeathScreen.deleteWorldButtonBounds(1280, 720);

        assertEquals(440, respawn.x());
        assertEquals(380, respawn.y());
        assertEquals(440, title.x());
        assertEquals(428, title.y());
        assertEquals(440, deleteWorld.x());
        assertEquals(428, deleteWorld.y());
        assertEquals(400, respawn.width());
        assertEquals(40, respawn.height());
        assertEquals(400, title.width());
        assertEquals(40, title.height());
        assertEquals(400, deleteWorld.width());
        assertEquals(40, deleteWorld.height());
        assertEquals("You cannot respawn in hardcore mode!", DeathScreen.hardcoreMessageText());
    }

    @Test
    @DisplayName("Death screen click helpers should distinguish Respawn from Title Menu")
    void deathScreenClickHelpersDistinguishButtons() {
        DeathScreen screen = new DeathScreen();
        screen.updateOrtho(1280, 720);

        screen.updateButtonHover(640, 400);
        assertEquals(true, screen.isRespawnClicked(true));
        assertEquals(false, screen.isTitleMenuClicked(true));

        screen.updateButtonHover(640, 448);
        assertEquals(false, screen.isRespawnClicked(true));
        assertEquals(true, screen.isTitleMenuClicked(true));

        screen.updateButtonHover(640, 500);
        assertEquals(false, screen.isRespawnClicked(true));
        assertEquals(false, screen.isTitleMenuClicked(true));
    }
}
