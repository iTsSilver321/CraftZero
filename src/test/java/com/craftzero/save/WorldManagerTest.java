package com.craftzero.save;

import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldManagerTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Legacy saves/default should appear as Default World without moving files")
    void legacyDefaultWorldIsListedWithoutMigrationMove() throws Exception {
        Path saves = tempDir.resolve("saves");
        Path defaultWorld = saves.resolve("default");
        Files.createDirectories(defaultWorld.resolve("chunks"));
        Files.writeString(defaultWorld.resolve("level.json"), """
                {
                  "formatVersion": 2,
                  "targetVersion": "Minecraft Java Release 1.0",
                  "seed": 42
                }
                """);

        WorldManager manager = new WorldManager(saves);
        List<WorldManager.WorldInfo> worlds = manager.listWorlds();

        assertEquals(1, worlds.size());
        WorldManager.WorldInfo info = worlds.get(0);
        assertEquals(WorldManager.DEFAULT_WORLD_ID, info.id());
        assertEquals(WorldManager.DEFAULT_WORLD_NAME, info.displayName());
        assertEquals(defaultWorld.toAbsolutePath().normalize(), info.path());
        assertTrue(info.isLegacyDefault());
        assertTrue(info.hasLevelData());
        assertFalse(info.hasMetadata());
        assertEquals(42L, info.seed());
        assertTrue(Files.exists(defaultWorld));
        assertFalse(Files.exists(saves.resolve(WorldManager.DEFAULT_WORLD_NAME)));
    }

    @Test
    @DisplayName("World manager should create, rename, and delete metadata worlds")
    void createRenameDeleteWorld() throws Exception {
        WorldManager manager = new WorldManager(tempDir.resolve("saves"));

        WorldManager.WorldInfo created = manager.createWorld("My Test World", 9876L,
                GameMode.CREATIVE, Difficulty.EASY);

        assertEquals("my-test-world", created.id());
        assertEquals("My Test World", created.displayName());
        assertEquals(9876L, created.seed());
        assertSame(GameMode.CREATIVE, created.gameMode());
        assertSame(Difficulty.EASY, created.difficulty());
        assertTrue(created.hasMetadata());
        assertTrue(Files.isDirectory(created.path()));
        assertTrue(Files.isRegularFile(created.path().resolve(WorldManager.METADATA_FILE)));

        WorldManager.WorldInfo renamed = manager.renameWorld(created.id(), "Renamed World");
        assertEquals(created.id(), renamed.id());
        assertEquals("Renamed World", renamed.displayName());
        assertTrue(Files.isDirectory(created.path()));

        assertTrue(manager.deleteWorld(created.id()));
        assertFalse(Files.exists(created.path()));
        assertFalse(manager.deleteWorld(created.id()));
    }

    @Test
    @DisplayName("Level data should be authoritative when world metadata is stale")
    void levelDataOverridesStaleMetadata() throws Exception {
        Path saves = tempDir.resolve("saves");
        Path world = saves.resolve("stale-world");
        Files.createDirectories(world);
        Files.writeString(world.resolve(WorldManager.METADATA_FILE), """
                {
                  "displayName": "Menu Name",
                  "seed": 1,
                  "gameMode": "creative",
                  "difficulty": "hard",
                  "createdAt": "2020-01-01T00:00:00Z",
                  "lastPlayed": "2020-01-01T00:00:00Z"
                }
                """);
        Files.writeString(world.resolve("level.json"), """
                {
                  "formatVersion": 3,
                  "targetVersion": "Minecraft Java Release 1.0",
                  "levelName": "Saved Name",
                  "lastPlayed": 1600000000000,
                  "seed": 222,
                  "gameMode": "SURVIVAL",
                  "difficulty": "EASY"
                }
                """);
        Instant oldFileTime = Instant.parse("2019-01-01T00:00:00Z");
        Files.setLastModifiedTime(world, FileTime.from(oldFileTime));
        Files.setLastModifiedTime(world.resolve(WorldManager.METADATA_FILE), FileTime.from(oldFileTime));
        Files.setLastModifiedTime(world.resolve("level.json"), FileTime.from(oldFileTime));

        WorldManager.WorldInfo info = new WorldManager(saves).listWorlds().get(0);

        assertEquals("Menu Name", info.displayName());
        assertEquals(222L, info.seed());
        assertSame(GameMode.SURVIVAL, info.gameMode());
        assertSame(Difficulty.EASY, info.difficulty());
        assertEquals(Instant.ofEpochMilli(1600000000000L), info.lastModified());
    }

    @Test
    @DisplayName("Legacy level names should be used when menu metadata is absent")
    void levelNameUsedWhenMetadataAbsent() throws Exception {
        Path saves = tempDir.resolve("saves");
        Path world = saves.resolve("old-save");
        Files.createDirectories(world);
        Files.writeString(world.resolve("level.json"), """
                {
                  "formatVersion": 3,
                  "targetVersion": "Minecraft Java Release 1.0",
                  "levelName": "Saved Display",
                  "seed": 333,
                  "gameMode": "creative",
                  "difficulty": "peaceful"
                }
                """);

        WorldManager.WorldInfo info = new WorldManager(saves).listWorlds().get(0);

        assertEquals("Saved Display", info.displayName());
        assertEquals(333L, info.seed());
        assertSame(GameMode.CREATIVE, info.gameMode());
        assertSame(Difficulty.PEACEFUL, info.difficulty());
    }
}
