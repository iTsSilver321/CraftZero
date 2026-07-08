package com.craftzero.graphics;

import com.craftzero.world.BiomeType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecipitationRendererTest {
    @Test
    void curtainRadiusScalesWithWeatherStrengthAndUsesRoundFootprint() {
        assertEquals(2, PrecipitationRenderer.curtainRadius(0.01f));
        assertEquals(5, PrecipitationRenderer.curtainRadius(0.51f));
        assertEquals(PrecipitationRenderer.MAX_CURTAIN_RADIUS, PrecipitationRenderer.curtainRadius(1.0f));

        assertTrue(PrecipitationRenderer.withinCurtainRadius(3, 4, 5));
        assertFalse(PrecipitationRenderer.withinCurtainRadius(4, 4, 5));
    }

    @Test
    void verticalOffsetLoopsRainFasterThanSnow() {
        assertEquals(0.70f,
                PrecipitationRenderer.verticalOffset(2.0f, PrecipitationRenderer.PrecipitationType.RAIN),
                0.0001f);
        assertEquals(0.05f,
                PrecipitationRenderer.verticalOffset(3.0f, PrecipitationRenderer.PrecipitationType.RAIN),
                0.0001f);
        assertEquals(0.24f,
                PrecipitationRenderer.verticalOffset(3.0f, PrecipitationRenderer.PrecipitationType.SNOW),
                0.0001f);
    }

    @Test
    void columnStyleIsStableAndDesynchronizesNeighboringCurtains() {
        PrecipitationRenderer.PrecipitationColumnStyle style =
                PrecipitationRenderer.columnStyle(8, -3, PrecipitationRenderer.PrecipitationType.RAIN);
        PrecipitationRenderer.PrecipitationColumnStyle same =
                PrecipitationRenderer.columnStyle(8, -3, PrecipitationRenderer.PrecipitationType.RAIN);
        PrecipitationRenderer.PrecipitationColumnStyle neighbor =
                PrecipitationRenderer.columnStyle(9, -3, PrecipitationRenderer.PrecipitationType.RAIN);

        assertEquals(style, same);
        assertTrue(style.phaseTicks() >= 0.0f && style.phaseTicks() < 32.0f);
        assertTrue(style.speedScale() >= 0.84f && style.speedScale() <= 1.16f);
        assertTrue(style.widthScale() >= 0.72f && style.widthScale() <= 1.08f);
        assertTrue(style.heightScale() >= 0.86f && style.heightScale() <= 1.14f);
        assertTrue(Math.abs(style.phaseTicks() - neighbor.phaseTicks()) > 0.0001f);
    }

    @Test
    void columnStyleOffsetsScrollPhase() {
        PrecipitationRenderer.PrecipitationColumnStyle style =
                new PrecipitationRenderer.PrecipitationColumnStyle(4.0f, 0.5f, 1.0f, 1.0f);

        assertEquals(0.05f,
                PrecipitationRenderer.verticalOffset(2.0f, PrecipitationRenderer.PrecipitationType.RAIN, style),
                0.0001f);
    }

    @Test
    void stripMatrixCentersOnColumnAndFacesCamera() {
        Vector3f camera = new Vector3f(4.5f, 70.0f, 4.5f);
        Matrix4f matrix = PrecipitationRenderer.stripMatrix(8, 3, camera, 0.25f,
                PrecipitationRenderer.PrecipitationType.RAIN);

        Vector3f translation = matrix.getTranslation(new Vector3f());
        assertEquals(8.5f, translation.x, 0.0001f);
        assertEquals(70.0f + PrecipitationRenderer.RAIN_CENTER_Y_BIAS - 0.25f, translation.y, 0.0001f);
        assertEquals(3.5f, translation.z, 0.0001f);

        Vector3f normal = matrix.transformDirection(new Vector3f(0.0f, 0.0f, 1.0f));
        Vector3f expected = new Vector3f(camera.x - 8.5f, 0.0f, camera.z - 3.5f).normalize();
        assertEquals(expected.x, normal.x, 0.0001f);
        assertEquals(expected.z, normal.z, 0.0001f);
    }

    @Test
    void stripMatrixAppliesColumnScaleVariation() {
        Vector3f camera = new Vector3f(4.5f, 70.0f, 4.5f);
        PrecipitationRenderer.PrecipitationColumnStyle style =
                new PrecipitationRenderer.PrecipitationColumnStyle(0.0f, 1.0f, 1.25f, 0.5f);
        Matrix4f matrix = PrecipitationRenderer.stripMatrix(8, 3, camera, 0.25f,
                PrecipitationRenderer.PrecipitationType.SNOW, style);

        assertEquals(1.25f, matrix.transformDirection(new Vector3f(1.0f, 0.0f, 0.0f)).length(), 0.0001f);
        assertEquals(0.5f, matrix.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).length(), 0.0001f);
    }

    @Test
    void precipitationKindUsesBiomeWeatherAndOpenSky() {
        World world = new World(9039L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] rain = findRainBiome(world);
            prepareOpenColumn(world, rain[0], rain[1]);
            world.setWeatherState("rain");

            assertEquals(PrecipitationRenderer.PrecipitationType.RAIN,
                    PrecipitationRenderer.precipitationAt(world, rain[0], 100, rain[1]));

            world.setBlock(rain[0], 104, rain[1], BlockType.STONE, 0);
            assertEquals(PrecipitationRenderer.PrecipitationType.NONE,
                    PrecipitationRenderer.precipitationAt(world, rain[0], 100, rain[1]));

            int[] snow = findSnowBiome(world);
            prepareOpenColumn(world, snow[0], snow[1]);
            assertEquals(PrecipitationRenderer.PrecipitationType.SNOW,
                    PrecipitationRenderer.precipitationAt(world, snow[0], 100, snow[1]));
        } finally {
            world.cleanup();
        }
    }

    private static int[] findRainBiome(World world) {
        for (int x = -512; x <= 512; x += 8) {
            for (int z = -512; z <= 512; z += 8) {
                BiomeType biome = world.getReleaseBiome(x, z);
                if (biome.hasPrecipitation() && !biome.canFreezeWater()) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No rain biome found near spawn search area");
    }

    private static int[] findSnowBiome(World world) {
        for (int radius = 0; radius <= 2048; radius += 16) {
            if (radius == 0) {
                int[] origin = snowBiomeAt(world, 0, 0);
                if (origin != null) {
                    return origin;
                }
                continue;
            }
            for (int x = -radius; x <= radius; x += 16) {
                int[] north = snowBiomeAt(world, x, -radius);
                if (north != null) {
                    return north;
                }
                int[] south = snowBiomeAt(world, x, radius);
                if (south != null) {
                    return south;
                }
            }
            for (int z = -radius + 16; z <= radius - 16; z += 16) {
                int[] west = snowBiomeAt(world, -radius, z);
                if (west != null) {
                    return west;
                }
                int[] east = snowBiomeAt(world, radius, z);
                if (east != null) {
                    return east;
                }
            }
        }
        throw new AssertionError("No snow biome found near spawn search area");
    }

    private static int[] snowBiomeAt(World world, int x, int z) {
        BiomeType biome = world.getReleaseBiome(x, z);
        return biome.hasPrecipitation() && biome.canFreezeWater() ? new int[] { x, z } : null;
    }

    private static void prepareOpenColumn(World world, int x, int z) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, BlockType.STONE, 0);
        for (int y = 100; y < Chunk.HEIGHT; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }
}
