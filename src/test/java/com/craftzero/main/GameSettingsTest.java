package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSettingsTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Missing options.txt should load Release 1.0-style defaults")
    void missingOptionsLoadsDefaults() throws Exception {
        GameSettings settings = GameSettings.load(tempDir.resolve("options.txt"));

        assertEquals(GameSettings.DEFAULT_PLAYER_NAME, settings.getPlayerName());
        assertSame(GameMode.SURVIVAL, settings.getGameMode());
        assertSame(Difficulty.NORMAL, settings.getDifficulty());
        assertEquals(1.0f, settings.getMusicVolume(), 0.0001f);
        assertEquals(1.0f, settings.getSoundVolume(), 0.0001f);
        assertEquals(GameSettings.DEFAULT_RENDER_DISTANCE_CHUNKS, settings.getRenderDistance());
        assertTrue(settings.isFancyGraphics());
        assertTrue(settings.isSmoothLighting());
        assertEquals(87, settings.getKeyBinding(GameSettings.KeyBinding.FORWARD));
        assertEquals(-100, settings.getKeyBinding(GameSettings.KeyBinding.ATTACK));
    }

    @Test
    @DisplayName("Directory-shaped options.txt should not prevent startup defaults")
    void optionsDirectoryLoadsDefaults() throws Exception {
        Path optionsDirectory = tempDir.resolve("options.txt");
        Files.createDirectories(optionsDirectory);

        GameSettings settings = GameSettings.load(optionsDirectory);

        assertEquals(GameSettings.DEFAULT_PLAYER_NAME, settings.getPlayerName());
        assertSame(GameMode.SURVIVAL, settings.getGameMode());
        assertSame(Difficulty.NORMAL, settings.getDifficulty());
    }

    @Test
    @DisplayName("options.txt should load, clamp, preserve keybinds, and save round-trip")
    void optionsRoundTrip() throws Exception {
        Path options = tempDir.resolve("options.txt");
        Files.write(options, List.of(
                "playerName:Alex",
                "gameMode:creative",
                "difficulty:hard",
                "music:0.25",
                "sound:2.0",
                "renderDistance:3",
                "fancyGraphics:false",
                "fullscreen:true",
                "texturePack:Retro Pack",
                "lastServer:localhost:25565",
                "key_key.forward:73",
                "key_key.attack:-101",
                "moddedOption:kept"));

        GameSettings loaded = GameSettings.load(options);

        assertEquals("Alex", loaded.getPlayerName());
        assertSame(GameMode.CREATIVE, loaded.getGameMode());
        assertSame(Difficulty.HARD, loaded.getDifficulty());
        assertEquals(0.25f, loaded.getMusicVolume(), 0.0001f);
        assertEquals(1.0f, loaded.getSoundVolume(), 0.0001f);
        assertEquals(GameSettings.MIN_RENDER_DISTANCE_CHUNKS, loaded.getRenderDistance());
        assertFalse(loaded.isFancyGraphics());
        assertTrue(loaded.isFullscreen());
        assertEquals("Retro Pack", loaded.getSelectedTexturePack());
        assertEquals("localhost:25565", loaded.getLastServer());
        assertEquals(73, loaded.getKeyBinding(GameSettings.KeyBinding.FORWARD));
        assertEquals(-101, loaded.getKeyBinding(GameSettings.KeyBinding.ATTACK));
        assertEquals("kept", loaded.getUnknownOptions().get("moddedOption"));

        Path saved = tempDir.resolve("saved-options.txt");
        loaded.save(saved);
        GameSettings restored = GameSettings.load(saved);

        assertEquals("Alex", restored.getPlayerName());
        assertSame(GameMode.CREATIVE, restored.getGameMode());
        assertSame(Difficulty.HARD, restored.getDifficulty());
        assertEquals(GameSettings.MIN_RENDER_DISTANCE_CHUNKS, restored.getRenderDistance());
        assertEquals(73, restored.getKeyBinding(GameSettings.KeyBinding.FORWARD));
        assertEquals("kept", restored.getUnknownOptions().get("moddedOption"));
    }

    @Test
    @DisplayName("Render distance should save chunk counts and load legacy enum options")
    void renderDistanceLoadsLegacyAndChunkValues() throws Exception {
        Path legacy = tempDir.resolve("legacy-options.txt");
        Files.write(legacy, List.of("renderDistance:1"));
        assertEquals(8, GameSettings.load(legacy).getRenderDistance());

        Path chunks = tempDir.resolve("chunk-options.txt");
        Files.write(chunks, List.of(
                "renderDistance:3",
                "renderDistanceChunks:16"));
        assertEquals(16, GameSettings.load(chunks).getRenderDistance());

        GameSettings settings = GameSettings.defaults();
        settings.setRenderDistance(99);
        assertEquals(16, settings.getRenderDistance());
        settings.setRenderDistance(-5);
        assertEquals(2, settings.getRenderDistance());
    }
}
