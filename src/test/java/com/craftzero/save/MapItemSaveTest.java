package com.craftzero.save;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapItemSaveTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Initialized map metadata and color data should survive save/load")
    void initializedMapMetadataRoundTripsThroughSave() throws Exception {
        Path worldDir = tempDir.resolve("map-save-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(9030L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.5f, 80.0f, 0.5f);
            ItemStack map = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, map, 0.5f, 0.5f);
            byte[] colors = MapItemData.view(map).colors();
            colors[64 + 64 * MapItemData.MAP_SIZE] = 1;
            map.putMetadata("map.colors", Base64.getEncoder().encodeToString(colors));
            player.getInventory().getHotbar()[0] = map;

            manager.save(world, player, new DayCycleManager());
            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status(),
                    () -> result.error() == null ? "level did not load" : result.error().message());
            SaveManager.LevelData loaded = result.levelData();
            assertNotNull(loaded);

            Player restoredPlayer = new Player(0.0f, 70.0f, 0.0f);
            restoredWorld = new World(loaded.seed);
            manager.applyLevel(loaded, restoredPlayer, new DayCycleManager(), restoredWorld);

            ItemStack restored = restoredPlayer.getInventory().getHotbar()[0];
            assertNotNull(restored);
            MapItemData.View restoredView = MapItemData.view(restored);
            assertNotNull(restoredView);
            assertTrue(restoredView.initialized());
            assertEquals(map.getDurability(), restored.getDurability());
            assertEquals(map.getMetadata().get("map.id"), restored.getMetadata().get("map.id"));
            assertEquals(MapItemData.DEFAULT_SCALE, restoredView.scale());
            assertEquals(64, restoredView.playerPixelX());
            assertEquals(64, restoredView.playerPixelZ());
            assertEquals(1,
                    MapItemData.basePaletteIndex(restoredView.colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF));
            assertEquals(map.getMetadata().get("map.colors"), restored.getMetadata().get("map.colors"));
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Shared copied-map color data should survive save/load")
    void copiedMapSharedDataRoundTripsThroughSave() throws Exception {
        Path worldDir = tempDir.resolve("shared-map-save-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(9031L);
        World restoredWorld = null;
        try {
            placeTopBlock(world, 4, 4, BlockType.GRASS);
            ItemStack source = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, source, 0.5f, 0.5f);
            ItemStack copy = MapItemData.copyInitializedMap(source, 1);
            String originalId = source.getMetadata().get("map.id");

            placeTopBlock(world, 4, 4, BlockType.WATER);
            MapItemData.updateHeldMap(world, copy, 0.5f, 0.5f);

            Player player = new Player(0.5f, 80.0f, 0.5f);
            player.getInventory().getHotbar()[0] = source;
            player.getInventory().getHotbar()[1] = copy;
            manager.save(world, player, new DayCycleManager());

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status(),
                    () -> result.error() == null ? "level did not load" : result.error().message());
            SaveManager.LevelData loaded = result.levelData();
            assertNotNull(loaded);
            assertNotNull(loaded.filledMaps);
            assertEquals(1, loaded.filledMaps.size());

            Player restoredPlayer = new Player(0.0f, 70.0f, 0.0f);
            restoredWorld = new World(loaded.seed);
            manager.applyLevel(loaded, restoredPlayer, new DayCycleManager(), restoredWorld);

            ItemStack restoredSource = restoredPlayer.getInventory().getHotbar()[0];
            ItemStack restoredCopy = restoredPlayer.getInventory().getHotbar()[1];
            assertNotNull(restoredSource);
            assertNotNull(restoredCopy);
            assertEquals(source.getDurability(), restoredSource.getDurability());
            assertEquals(copy.getDurability(), restoredCopy.getDurability());
            int center = 64 + 64 * MapItemData.MAP_SIZE;
            assertEquals(4,
                    MapItemData.basePaletteIndex(MapItemData.view(restoredWorld, restoredSource).colors()[center] & 0xFF));
            assertEquals(4,
                    MapItemData.basePaletteIndex(MapItemData.view(restoredWorld, restoredCopy).colors()[center] & 0xFF));

            ItemStack newMap = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(restoredWorld, newMap, 0.5f, 0.5f);
            assertNotEquals(originalId, newMap.getMetadata().get("map.id"));
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    private static void placeTopBlock(World world, int x, int z, BlockType type) {
        world.setBlock(x, 127, z, BlockType.AIR, 0);
        world.setBlock(x, 126, z, type, 0);
    }
}
