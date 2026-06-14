package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    @DisplayName("Gameplay states should be derived from visible menu and world activity flags")
    void gameplayStatesUseFlags() {
        assertTrue(GameState.PLAYING.isGameplay());
        assertTrue(GameState.IN_GAME.isGameplay());
        assertFalse(GameState.PAUSED.isGameplay());
        assertFalse(GameState.TITLE.isGameplay());

        assertTrue(GameState.PAUSED.showsWorldBehindMenu());
        assertFalse(GameState.LOADING_WORLD.isWorldActive());
    }
}
