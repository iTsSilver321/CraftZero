package com.craftzero.inventory;

import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapItemDataTest {
    private static final int DEFAULT_PIXEL_BLOCKS = 1 << MapItemData.DEFAULT_SCALE;

    @Test
    @DisplayName("Release-era map use should initialize scale, dimension, player marker, and terrain colors")
    void mapUseInitializesAndSamplesLoadedTerrain() {
        World world = new World(9010L);
        try {
            placeMapPixelArea(world, 0, 0, BlockType.GRASS);
            ItemStack map = new ItemStack(ItemType.MAP, 1);

            assertTrue(MapItemData.useMap(world, map, 0.5f, 0.5f));

            MapItemData.View view = MapItemData.view(map);
            assertNotNull(view);
            assertTrue(view.initialized());
            assertEquals("map_0", map.getMetadata().get("map.id"));
            assertEquals(0, map.getDurability());
            assertEquals(MapItemData.DEFAULT_SCALE, view.scale());
            assertEquals(0, view.centerX());
            assertEquals(0, view.centerZ());
            assertEquals(Dimension.OVERWORLD.getId(), view.dimension());
            assertEquals(64, view.playerPixelX());
            assertEquals(64, view.playerPixelZ());
            assertEquals(0, view.playerRotation());
            assertMapBaseColor(view, 64, 64, 1);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release-era maps should align scale-three centers to the world-spawn map grid")
    void mapInitializationAlignsCenterToWorldSpawnGrid() {
        World world = new World(9019L);
        try {
            world.setWorldSpawn(600, 80, 600);
            placeMapPixelArea(world, 600, 600, BlockType.SAND);
            ItemStack map = new ItemStack(ItemType.MAP, 1);

            assertTrue(MapItemData.useMap(world, map, 600.5f, 600.5f));

            MapItemData.View view = MapItemData.view(map);
            assertNotNull(view);
            assertEquals(1024, view.centerX());
            assertEquals(1024, view.centerZ());
            assertEquals(11, view.playerPixelX());
            assertEquals(11, view.playerPixelZ());
            assertMapBaseColor(view, 11, 11, 3);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Map player marker rotation should quantize yaw into the old 16 directions")
    void mapRotationQuantizesYawIntoSixteenDirections() {
        assertEquals(0, MapItemData.mapRotation(0.0f));
        assertEquals(2, MapItemData.mapRotation(45.0f));
        assertEquals(4, MapItemData.mapRotation(90.0f));
        assertEquals(8, MapItemData.mapRotation(180.0f));
        assertEquals(12, MapItemData.mapRotation(270.0f));
        assertEquals(0, MapItemData.mapRotation(359.0f));
        assertEquals(15, MapItemData.mapRotation(-22.5f));
    }

    @Test
    @DisplayName("Maps should not repaint terrain when held in a different dimension")
    void differentDimensionDoesNotRepaintMap() {
        World overworld = new World(9011L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        World nether = new World(9011L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            placeMapPixelArea(overworld, 0, 0, BlockType.GRASS);
            placeMapPixelArea(nether, 0, 0, BlockType.NETHERRACK);
            ItemStack map = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(overworld, map, 0.5f, 0.5f);
            byte[] before = MapItemData.view(map).colors().clone();

            assertTrue(MapItemData.updateHeldMap(nether, map, 0.5f, 0.5f));

            MapItemData.View after = MapItemData.view(map);
            assertArrayEquals(before, after.colors());
            assertEquals(-1, after.playerPixelX());
            assertEquals(-1, after.playerPixelZ());
            assertEquals(-1, after.playerRotation());
        } finally {
            overworld.cleanup();
            nether.cleanup();
        }
    }

    @Test
    @DisplayName("Copied initialized maps should preserve map identity and explored colors")
    void copiedInitializedMapsPreserveMetadata() {
        World world = new World(9013L);
        try {
            placeMapPixelArea(world, 0, 0, BlockType.GRASS);
            ItemStack map = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, map, 0.5f, 0.5f);

            ItemStack copy = MapItemData.copyInitializedMap(map, 2);

            assertNotNull(copy);
            assertEquals(2, copy.getCount());
            assertTrue(MapItemData.isInitializedMap(copy));
            assertEquals(map.getDurability(), copy.getDurability());
            assertEquals(map.getMetadata(), copy.getMetadata());
            assertArrayEquals(MapItemData.view(map).colors(), MapItemData.view(copy).colors());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Copied maps should share live explored colors through the world map store")
    void copiedMapsShareLiveWorldColorData() {
        World world = new World(9014L);
        try {
            placeMapPixelArea(world, 0, 0, BlockType.GRASS);
            ItemStack source = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, source, 0.5f, 0.5f);
            ItemStack copy = MapItemData.copyInitializedMap(source, 1);

            placeMapPixelArea(world, 0, 0, BlockType.WATER);
            assertTrue(MapItemData.updateHeldMap(world, copy, 0.5f, 0.5f));

            int center = 64 + 64 * MapItemData.MAP_SIZE;
            assertEquals(4, MapItemData.basePaletteIndex(MapItemData.view(world, copy).colors()[center] & 0xFF));
            assertEquals(4, MapItemData.basePaletteIndex(MapItemData.view(world, source).colors()[center] & 0xFF));
            assertEquals(copy.getMetadata().get("map.colors"), source.getMetadata().get("map.colors"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Separately initialized maps should receive distinct map ids even at the same center")
    void independentMapsAtSameCenterDoNotAccidentallyShareData() {
        World world = new World(9015L);
        try {
            placeMapPixelArea(world, 0, 0, BlockType.GRASS);
            ItemStack first = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, first, 0.5f, 0.5f);

            placeMapPixelArea(world, 0, 0, BlockType.WATER);
            ItemStack second = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, second, 0.5f, 0.5f);

            int center = 64 + 64 * MapItemData.MAP_SIZE;
            assertNotEquals(first.getMetadata().get("map.id"), second.getMetadata().get("map.id"));
            assertNotEquals(first.getDurability(), second.getDurability());
            assertEquals(1, MapItemData.basePaletteIndex(MapItemData.view(world, first).colors()[center] & 0xFF));
            assertEquals(4, MapItemData.basePaletteIndex(MapItemData.view(world, second).colors()[center] & 0xFF));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Initialized maps should restore missing metadata identity from item damage")
    void initializedMapsRestoreIdentityFromItemDamage() {
        ItemStack map = new ItemStack(ItemType.MAP, 1, 42);
        map.putMetadata("map.initialized", "true");

        assertTrue(MapItemData.isInitializedMap(map));

        assertEquals("map_42", map.getMetadata().get("map.id"));
        assertEquals(42, map.getDurability());
    }

    @Test
    @DisplayName("Map colors should encode Release-style brightness from terrain relief and water depth")
    void mapColorsEncodeTerrainAndWaterShading() {
        World world = new World(9016L);
        try {
            placeMapPixelArea(world, 0, 0, BlockType.GRASS, 126);
            placeMapPixelArea(world, -DEFAULT_PIXEL_BLOCKS, 0, BlockType.GRASS, 120);
            placeMapPixelArea(world, DEFAULT_PIXEL_BLOCKS, 0, BlockType.GRASS, 120);
            placeMapPixelArea(world, 0, -DEFAULT_PIXEL_BLOCKS, BlockType.GRASS, 120);
            placeMapPixelArea(world, 0, DEFAULT_PIXEL_BLOCKS, BlockType.GRASS, 120);
            ItemStack highMap = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, highMap, 0.5f, 0.5f);
            int highColor = MapItemData.view(world, highMap).colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF;
            assertEquals(1, MapItemData.basePaletteIndex(highColor));
            assertEquals(2, MapItemData.shadeIndex(highColor));

            placeMapPixelArea(world, 0, 0, BlockType.GRASS, 120);
            placeMapPixelArea(world, -DEFAULT_PIXEL_BLOCKS, 0, BlockType.GRASS, 126);
            placeMapPixelArea(world, DEFAULT_PIXEL_BLOCKS, 0, BlockType.GRASS, 126);
            placeMapPixelArea(world, 0, -DEFAULT_PIXEL_BLOCKS, BlockType.GRASS, 126);
            placeMapPixelArea(world, 0, DEFAULT_PIXEL_BLOCKS, BlockType.GRASS, 126);
            ItemStack lowMap = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, lowMap, 0.5f, 0.5f);
            int lowColor = MapItemData.view(world, lowMap).colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF;
            assertEquals(1, MapItemData.basePaletteIndex(lowColor));
            assertEquals(0, MapItemData.shadeIndex(lowColor));

            placeWaterColumnArea(world, 0, 0, 116, 127);
            ItemStack waterMap = new ItemStack(ItemType.MAP, 1);
            MapItemData.useMap(world, waterMap, 0.5f, 0.5f);
            int waterColor = MapItemData.view(world, waterMap).colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF;
            assertEquals(4, MapItemData.basePaletteIndex(waterColor));
            assertEquals(0, MapItemData.shadeIndex(waterColor));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Medium-depth map water should use the Release checkerboard shade term")
    void mapWaterShadingUsesReleaseCheckerTerm() {
        World world = new World(9018L);
        try {
            placeWaterColumnArea(world, 0, 0, 124, 127);
            placeWaterColumnArea(world, DEFAULT_PIXEL_BLOCKS, 0, 124, 127);
            ItemStack map = new ItemStack(ItemType.MAP, 1);

            MapItemData.useMap(world, map, 0.5f, 0.5f);

            byte[] colors = MapItemData.view(world, map).colors();
            int evenWater = colors[64 + 64 * MapItemData.MAP_SIZE] & 0xFF;
            int oddWater = colors[65 + 64 * MapItemData.MAP_SIZE] & 0xFF;
            assertEquals(4, MapItemData.basePaletteIndex(evenWater));
            assertEquals(4, MapItemData.basePaletteIndex(oddWater));
            assertEquals(2, MapItemData.shadeIndex(evenWater));
            assertEquals(1, MapItemData.shadeIndex(oddWater));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Map pixels should use the dominant block color across their represented area")
    void mapPixelUsesDominantAreaColorInsteadOfCenterColumn() {
        World world = new World(9017L);
        try {
            placeMapPixelArea(world, 0, 0, BlockType.GRASS);
            placeTopBlock(world, 4, 4, BlockType.WATER);
            ItemStack map = new ItemStack(ItemType.MAP, 1);

            MapItemData.useMap(world, map, 0.5f, 0.5f);

            int color = MapItemData.view(world, map).colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF;
            assertEquals(1, MapItemData.basePaletteIndex(color));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Legacy raw map color bytes should normalize before shaded rendering")
    void legacyRawMapColorBytesNormalizeToShadedEncoding() {
        byte[] legacyColors = new byte[MapItemData.MAP_SIZE * MapItemData.MAP_SIZE];
        legacyColors[64 + 64 * MapItemData.MAP_SIZE] = 4;
        ItemStack map = new ItemStack(ItemType.MAP, 1);
        map.putMetadata("map.colors", Base64.getEncoder().encodeToString(legacyColors));

        int color = MapItemData.view(map).colors()[64 + 64 * MapItemData.MAP_SIZE] & 0xFF;

        assertEquals(4, MapItemData.basePaletteIndex(color));
        assertEquals(2, MapItemData.shadeIndex(color));
        assertEquals("release-shaded", map.getMetadata().get("map.colorFormat"));
    }

    @Test
    @DisplayName("Non-map stacks should be ignored by map behavior")
    void nonMapStacksAreIgnored() {
        ItemStack stick = new ItemStack(ItemType.STICK, 1);
        World world = new World(9012L);
        try {
            assertFalse(MapItemData.updateHeldMap(world, stick, 0.0f, 0.0f));
            assertFalse(MapItemData.useMap(world, stick, 0.0f, 0.0f));
        } finally {
            world.cleanup();
        }
    }

    private static void placeTopBlock(World world, int x, int z, BlockType type) {
        placeTopBlock(world, x, z, type, 126);
    }

    private static void placeTopBlock(World world, int x, int z, BlockType type, int y) {
        world.setBlock(x, 127, z, BlockType.AIR, 0);
        for (int clearY = y + 1; clearY < 127; clearY++) {
            world.setBlock(x, clearY, z, BlockType.AIR, 0);
        }
        world.setBlock(x, y, z, type, 0);
    }

    private static void placeMapPixelArea(World world, int startX, int startZ, BlockType type) {
        placeMapPixelArea(world, startX, startZ, type, 126);
    }

    private static void placeMapPixelArea(World world, int startX, int startZ, BlockType type, int y) {
        for (int dx = 0; dx < DEFAULT_PIXEL_BLOCKS; dx++) {
            for (int dz = 0; dz < DEFAULT_PIXEL_BLOCKS; dz++) {
                placeTopBlock(world, startX + dx, startZ + dz, type, y);
            }
        }
    }

    private static void placeWaterColumn(World world, int x, int z, int minY, int maxY) {
        for (int y = minY; y <= maxY; y++) {
            world.setBlock(x, y, z, BlockType.WATER, 0);
        }
    }

    private static void placeWaterColumnArea(World world, int startX, int startZ, int minY, int maxY) {
        for (int dx = 0; dx < DEFAULT_PIXEL_BLOCKS; dx++) {
            for (int dz = 0; dz < DEFAULT_PIXEL_BLOCKS; dz++) {
                placeWaterColumn(world, startX + dx, startZ + dz, minY, maxY);
            }
        }
    }

    private static void assertMapBaseColor(MapItemData.View view, int x, int z, int baseColor) {
        assertEquals(baseColor, MapItemData.basePaletteIndex(view.colors()[x + z * MapItemData.MAP_SIZE] & 0xFF));
    }
}
