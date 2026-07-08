package com.craftzero.world;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Release 1.0-style interpolated density sampler for Overworld base terrain.
 */
final class OverworldDensityField {
    private static final int XZ_STEP = 4;
    private static final int Y_STEP = 8;
    private static final int MAX_DENSITY_CACHE_ENTRIES = 262_144;

    private final ReleaseOneOctaveNoise noiseGen1;
    private final ReleaseOneOctaveNoise noiseGen2;
    private final ReleaseOneOctaveNoise noiseGen3;
    private final ReleaseOneOctaveNoise noiseGen4;
    private final ReleaseOneOctaveNoise noiseGen5;
    private final ReleaseOneOctaveNoise noiseGen6;
    private final BiomeSampler biomeSampler;
    private final float[] parabolicField = new float[25];
    private final ConcurrentHashMap<DensityKey, Double> densityCache = new ConcurrentHashMap<>();

    interface BiomeSampler {
        BiomeType getBiome(int layerX, int layerZ);
    }

    OverworldDensityField(long seed, BiomeSampler biomeSampler) {
        Random random = new Random(seed);
        this.noiseGen1 = new ReleaseOneOctaveNoise(random, 16);
        this.noiseGen2 = new ReleaseOneOctaveNoise(random, 16);
        this.noiseGen3 = new ReleaseOneOctaveNoise(random, 8);
        this.noiseGen4 = new ReleaseOneOctaveNoise(random, 4);
        this.noiseGen5 = new ReleaseOneOctaveNoise(random, 10);
        this.noiseGen6 = new ReleaseOneOctaveNoise(random, 16);
        this.biomeSampler = biomeSampler;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                parabolicField[dx + 2 + (dz + 2) * 5] = 10.0F / (float) Math.sqrt(dx * dx + dz * dz + 0.2F);
            }
        }
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

    double[] stoneNoiseForChunk(int chunkX, int chunkZ) {
        double scale = 0.03125 * 2.0;
        return noiseGen4.generateNoiseOctaves(null, chunkX * 16, chunkZ * 16, 0,
                16, 16, 1, scale, scale, scale);
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
        int gridX = Math.floorDiv(blockX, XZ_STEP);
        int gridY = Math.floorDiv(y, Y_STEP);
        int gridZ = Math.floorDiv(blockZ, XZ_STEP);
        DensityKey key = new DensityKey(gridX, gridY, gridZ);
        Double cached = densityCache.get(key);
        if (cached != null) {
            return cached;
        }
        double computed = computeProviderDensity(gridX, gridY, gridZ);
        if (densityCache.size() > MAX_DENSITY_CACHE_ENTRIES) {
            densityCache.clear();
        }
        densityCache.put(key, computed);
        return computed;
    }

    private double computeProviderDensity(int gridX, int gridY, int gridZ) {
        double horizontalScale = 684.41200000000003;
        double verticalScale = 684.41200000000003;
        double depthNoise = noiseGen6.generateNoiseOctaves(null, gridX, gridZ, 1, 1,
                200.0, 200.0, 0.5)[0] / 8000.0;
        double minLimit = noiseGen1.sampleNoiseOctaves3D(gridX, gridY, gridZ,
                horizontalScale, verticalScale, horizontalScale) / 512.0;
        double maxLimit = noiseGen2.sampleNoiseOctaves3D(gridX, gridY, gridZ,
                horizontalScale, verticalScale, horizontalScale) / 512.0;
        double selector = (noiseGen3.sampleNoiseOctaves3D(gridX, gridY, gridZ,
                horizontalScale / 80.0, verticalScale / 160.0, horizontalScale / 80.0) / 10.0 + 1.0) / 2.0;

        float blendedMaxHeight = 0.0F;
        float blendedMinHeight = 0.0F;
        float totalWeight = 0.0F;
        BiomeType centerBiome = biomeAtGrid(gridX, gridZ);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BiomeType sampleBiome = biomeAtGrid(gridX + dx, gridZ + dz);
                float weight = parabolicField[dx + 2 + (dz + 2) * 5] / (sampleBiome.minHeight() + 2.0F);
                if (sampleBiome.minHeight() > centerBiome.minHeight()) {
                    weight /= 2.0F;
                }
                blendedMaxHeight += sampleBiome.maxHeight() * weight;
                blendedMinHeight += sampleBiome.minHeight() * weight;
                totalWeight += weight;
            }
        }
        blendedMaxHeight /= totalWeight;
        blendedMinHeight /= totalWeight;
        blendedMaxHeight = blendedMaxHeight * 0.9F + 0.1F;
        blendedMinHeight = (blendedMinHeight * 4.0F - 1.0F) / 8.0F;

        if (depthNoise < 0.0) {
            depthNoise = -depthNoise * 0.3;
        }
        depthNoise = depthNoise * 3.0 - 2.0;
        if (depthNoise < 0.0) {
            depthNoise /= 2.0;
            if (depthNoise < -1.0) {
                depthNoise = -1.0;
            }
            depthNoise /= 1.4;
            depthNoise /= 2.0;
        } else {
            if (depthNoise > 1.0) {
                depthNoise = 1.0;
            }
            depthNoise /= 8.0;
        }

        double minHeight = blendedMinHeight + depthNoise * 0.2;
        minHeight = minHeight * 17.0 / 16.0;
        double terrainCenter = 17.0 / 2.0 + minHeight * 4.0;
        double vertical = ((gridY - terrainCenter) * 12.0) / blendedMaxHeight;
        if (vertical < 0.0) {
            vertical *= 4.0;
        }

        double density;
        if (selector < 0.0) {
            density = minLimit;
        } else if (selector > 1.0) {
            density = maxLimit;
        } else {
            density = minLimit + (maxLimit - minLimit) * selector;
        }
        density -= vertical;

        if (gridY > 13) {
            double fade = (double) (gridY - 13) / 3.0;
            density = density * (1.0 - fade) + -10.0 * fade;
        }
        return density;
    }

    private BiomeType biomeAtGrid(int gridX, int gridZ) {
        return biomeSampler == null ? BiomeType.PLAINS : biomeSampler.getBiome(gridX, gridZ);
    }

    private static int floorToStep(int value, int step) {
        return Math.floorDiv(value, step) * step;
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }

    private record DensityKey(int gridX, int gridY, int gridZ) {
    }
}
