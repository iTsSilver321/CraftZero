package com.craftzero.world;

import com.craftzero.math.Noise;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Release 1.0-style interpolated density sampler for Overworld base terrain.
 */
final class OverworldDensityField {
    private static final int XZ_STEP = 4;
    private static final int Y_STEP = 8;
    private static final int BIOME_BLEND_RADIUS = 16;
    private static final int BIOME_BLEND_STEP = 16;

    private final Noise terrainNoise;
    private final Noise detailNoise;
    private final BiomeSampler biomeSampler;
    private final ConcurrentHashMap<Long, Double> surfaceCache = new ConcurrentHashMap<>();

    interface BiomeSampler {
        BiomeType getBiome(int blockX, int blockZ);
    }

    OverworldDensityField(Noise terrainNoise, Noise detailNoise, BiomeSampler biomeSampler) {
        this.terrainNoise = terrainNoise;
        this.detailNoise = detailNoise;
        this.biomeSampler = biomeSampler;
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
        long key = (((long) blockX) << 32) ^ (blockZ & 0xFFFFFFFFL);
        Double cached = surfaceCache.get(key);
        if (cached != null) {
            return cached;
        }
        double computed = computeSurfaceAnchor(blockX, blockZ, biome);
        surfaceCache.put(key, computed);
        return computed;
    }

    private double computeSurfaceAnchor(int blockX, int blockZ, BiomeType biome) {
        if (biomeSampler == null || biome == BiomeType.HELL || biome == BiomeType.SKY) {
            return rawSurfaceAnchor(blockX, blockZ, biome);
        }

        BiomeType centerBiome = biomeSampler.getBiome(blockX, blockZ);
        double weightedHeight = 0.0;
        double totalWeight = 0.0;
        for (int dx = -BIOME_BLEND_RADIUS; dx <= BIOME_BLEND_RADIUS; dx += BIOME_BLEND_STEP) {
            for (int dz = -BIOME_BLEND_RADIUS; dz <= BIOME_BLEND_RADIUS; dz += BIOME_BLEND_STEP) {
                int sx = blockX + dx;
                int sz = blockZ + dz;
                BiomeType sampleBiome = dx == 0 && dz == 0 ? centerBiome : biomeSampler.getBiome(sx, sz);
                double distanceSq = dx * dx + dz * dz;
                double weight = 1.0 / (1.0 + distanceSq / 96.0);
                weightedHeight += rawSurfaceAnchor(sx, sz, sampleBiome) * weight;
                totalWeight += weight;
            }
        }

        double blended = weightedHeight / Math.max(0.0001, totalWeight);
        double local = rawSurfaceAnchor(blockX, blockZ, centerBiome);
        double height = lerp(0.62, local, blended);
        return Math.max(5.0, Math.min(Chunk.HEIGHT - 6.0, height));
    }

    private double rawSurfaceAnchor(int blockX, int blockZ, BiomeType biome) {
        double broad = terrainNoise.octaveNoise2D(blockX * 0.0035, blockZ * 0.0035, 4, 0.5);
        double medium = detailNoise.octaveNoise2D(blockX * 0.011, blockZ * 0.011, 3, 0.52);
        double rough = terrainNoise.octaveNoise2D((blockX - 5000) * 0.028, (blockZ + 5000) * 0.028, 2, 0.5);
        double ridged = 1.0 - Math.abs(detailNoise.octaveNoise2D(blockX * 0.006, blockZ * 0.006, 3, 0.5));

        double height = ReleaseOneWorldGenerator.SEA_LEVEL + broad * 12.0 + medium * 4.5 + rough * 1.4;
        if (biome.isOceanic() || biome == BiomeType.RIVER || biome == BiomeType.FROZEN_RIVER) {
            height -= 13.0 + Math.max(0.0, -broad) * 5.0;
        } else if (biome == BiomeType.EXTREME_HILLS || biome == BiomeType.ICE_MOUNTAINS) {
            height += 12.0 + ridged * 14.0;
        } else if (biome == BiomeType.EXTREME_HILLS_EDGE) {
            height += 6.0 + ridged * 7.0;
        } else if (biome == BiomeType.FOREST_HILLS || biome == BiomeType.TAIGA_HILLS || biome == BiomeType.DESERT_HILLS) {
            height += 5.0 + ridged * 6.0;
        } else if (biome == BiomeType.PLAINS || biome == BiomeType.DESERT) {
            height = ReleaseOneWorldGenerator.SEA_LEVEL + broad * 5.5 + medium * 1.8;
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
