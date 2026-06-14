package com.craftzero.world;

import com.craftzero.math.Noise;

/**
 * Release 1.0-style interpolated density sampler for Overworld base terrain.
 */
final class OverworldDensityField {
    private static final int XZ_STEP = 4;
    private static final int Y_STEP = 8;

    private final Noise terrainNoise;
    private final Noise detailNoise;

    OverworldDensityField(Noise terrainNoise, Noise detailNoise) {
        this.terrainNoise = terrainNoise;
        this.detailNoise = detailNoise;
    }

    boolean isSolid(int blockX, int y, int blockZ, BiomeType biome) {
        return density(blockX, y, blockZ, biome) > 0.0;
    }

    int terrainTopY(int blockX, int blockZ, BiomeType biome) {
        for (int y = Chunk.HEIGHT - 2; y >= 1; y--) {
            if (isSolid(blockX, y, blockZ, biome)) {
                return y;
            }
        }
        return 1;
    }

    double density(int blockX, int y, int blockZ, BiomeType biome) {
        int x0 = floorToStep(blockX, XZ_STEP);
        int y0 = floorToStep(y, Y_STEP);
        int z0 = floorToStep(blockZ, XZ_STEP);
        int x1 = x0 + XZ_STEP;
        int y1 = y0 + Y_STEP;
        int z1 = z0 + XZ_STEP;

        double tx = (blockX - x0) / (double) XZ_STEP;
        double ty = (y - y0) / (double) Y_STEP;
        double tz = (blockZ - z0) / (double) XZ_STEP;

        double d000 = sampleDensity(x0, y0, z0, biome);
        double d100 = sampleDensity(x1, y0, z0, biome);
        double d010 = sampleDensity(x0, y1, z0, biome);
        double d110 = sampleDensity(x1, y1, z0, biome);
        double d001 = sampleDensity(x0, y0, z1, biome);
        double d101 = sampleDensity(x1, y0, z1, biome);
        double d011 = sampleDensity(x0, y1, z1, biome);
        double d111 = sampleDensity(x1, y1, z1, biome);

        double x00 = lerp(tx, d000, d100);
        double x10 = lerp(tx, d010, d110);
        double x01 = lerp(tx, d001, d101);
        double x11 = lerp(tx, d011, d111);
        double y0Blend = lerp(ty, x00, x10);
        double y1Blend = lerp(ty, x01, x11);
        return lerp(tz, y0Blend, y1Blend);
    }

    double sampleDensity(int blockX, int y, int blockZ, BiomeType biome) {
        double surface = surfaceAnchor(blockX, blockZ, biome);
        double vertical = (surface - y) / 16.0;
        double shape = terrainNoise.octaveNoise3D(blockX * 0.012, y * 0.018, blockZ * 0.012, 4, 0.55);
        double warp = detailNoise.octaveNoise3D((blockX + 3000) * 0.025, y * 0.025,
                (blockZ - 3000) * 0.025, 2, 0.5);
        double shelf = detailNoise.octaveNoise2D(blockX * 0.018, blockZ * 0.018, 3, 0.5) * 0.12;
        double density = vertical + shape * 0.52 + warp * 0.16 + shelf;
        if (y < 8) {
            density += (8 - y) * 0.35;
        }
        if (y > 118) {
            density -= (y - 118) * 0.25;
        }
        return density;
    }

    double surfaceAnchor(int blockX, int blockZ, BiomeType biome) {
        double broad = terrainNoise.octaveNoise2D(blockX * 0.0035, blockZ * 0.0035, 4, 0.5);
        double medium = detailNoise.octaveNoise2D(blockX * 0.011, blockZ * 0.011, 3, 0.52);
        double rough = terrainNoise.octaveNoise2D((blockX - 5000) * 0.028, (blockZ + 5000) * 0.028, 2, 0.5);
        double ridged = 1.0 - Math.abs(detailNoise.octaveNoise2D(blockX * 0.006, blockZ * 0.006, 3, 0.5));

        double height = ReleaseOneWorldGenerator.SEA_LEVEL + broad * 17.0 + medium * 7.0 + rough * 2.5;
        if (biome.isOceanic() || biome == BiomeType.RIVER || biome == BiomeType.FROZEN_RIVER) {
            height -= 15.0 + Math.max(0.0, -broad) * 8.0;
        } else if (biome == BiomeType.EXTREME_HILLS || biome == BiomeType.ICE_MOUNTAINS) {
            height += 18.0 + ridged * 22.0;
        } else if (biome == BiomeType.EXTREME_HILLS_EDGE) {
            height += 10.0 + ridged * 10.0;
        } else if (biome == BiomeType.FOREST_HILLS || biome == BiomeType.TAIGA_HILLS || biome == BiomeType.DESERT_HILLS) {
            height += 8.0 + ridged * 9.0;
        } else if (biome == BiomeType.PLAINS || biome == BiomeType.DESERT) {
            height = ReleaseOneWorldGenerator.SEA_LEVEL + broad * 7.0 + medium * 2.0;
        } else if (biome == BiomeType.SWAMPLAND) {
            height = Math.min(height, ReleaseOneWorldGenerator.SEA_LEVEL + 2.0);
        }
        return Math.max(5.0, Math.min(Chunk.HEIGHT - 6.0, height));
    }

    private static int floorToStep(int value, int step) {
        return Math.floorDiv(value, step) * step;
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * smooth(t);
    }

    private static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }
}
