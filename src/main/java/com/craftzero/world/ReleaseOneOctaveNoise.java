package com.craftzero.world;

import java.util.Random;

/**
 * Java 1.0-style octave Perlin noise seeded from java.util.Random.
 */
final class ReleaseOneOctaveNoise {
    private final Perlin[] octaves;

    ReleaseOneOctaveNoise(long seed, int octaveCount) {
        this(new Random(seed), octaveCount);
    }

    ReleaseOneOctaveNoise(Random random, int octaveCount) {
        this.octaves = new Perlin[Math.max(1, octaveCount)];
        for (int i = 0; i < octaves.length; i++) {
            octaves[i] = new Perlin(random);
        }
    }

    double octaveNoise2D(double x, double z, int octaveLimit, double persistence) {
        int count = Math.min(Math.max(1, octaveLimit), octaves.length);
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;
        for (int i = 0; i < count; i++) {
            total += octaves[i].noise(x * frequency, 0.0, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return total / maxValue;
    }

    double octaveNoise3D(double x, double y, double z, int octaveLimit, double persistence) {
        int count = Math.min(Math.max(1, octaveLimit), octaves.length);
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;
        for (int i = 0; i < count; i++) {
            total += octaves[i].noise(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return total / maxValue;
    }

    double[] generateNoiseOctaves(double[] values, int x, int y, int z, int xSize, int ySize, int zSize,
            double xScale, double yScale, double zScale) {
        return generateNoiseOctaves(values, x, (double) y, z, xSize, ySize, zSize, xScale, yScale, zScale);
    }

    double[] generateNoiseOctaves(double[] values, int x, double y, int z, int xSize, int ySize, int zSize,
            double xScale, double yScale, double zScale) {
        int length = xSize * ySize * zSize;
        if (values == null || values.length < length) {
            values = new double[length];
        } else {
            for (int i = 0; i < length; i++) {
                values[i] = 0.0;
            }
        }

        double octaveScale = 1.0;
        for (Perlin octave : octaves) {
            double sampleX = x * octaveScale * xScale;
            double sampleY = y * octaveScale * yScale;
            double sampleZ = z * octaveScale * zScale;
            long floorX = floorLong(sampleX);
            long floorZ = floorLong(sampleZ);
            sampleX -= floorX;
            sampleZ -= floorZ;
            floorX %= 0x1000000L;
            floorZ %= 0x1000000L;
            sampleX += floorX;
            sampleZ += floorZ;
            octave.add(values, sampleX, sampleY, sampleZ, xSize, ySize, zSize,
                    xScale * octaveScale, yScale * octaveScale, zScale * octaveScale, octaveScale);
            octaveScale /= 2.0;
        }
        return values;
    }

    double[] generateNoiseOctaves(double[] values, int x, int z, int xSize, int zSize,
            double xScale, double zScale, double amplitudeScale) {
        return generateNoiseOctaves(values, x, 10, z, xSize, 1, zSize, xScale, 1.0, zScale);
    }

    double sampleNoiseOctaves3D(int x, int y, int z, double xScale, double yScale, double zScale) {
        double total = 0.0;
        double octaveScale = 1.0;
        for (Perlin octave : octaves) {
            double sampleX = x * octaveScale * xScale;
            double sampleY = y * octaveScale * yScale;
            double sampleZ = z * octaveScale * zScale;
            long floorX = floorLong(sampleX);
            long floorZ = floorLong(sampleZ);
            sampleX -= floorX;
            sampleZ -= floorZ;
            floorX %= 0x1000000L;
            floorZ %= 0x1000000L;
            sampleX += floorX;
            sampleZ += floorZ;
            total += octave.noise(sampleX, sampleY, sampleZ) / octaveScale;
            octaveScale /= 2.0;
        }
        return total;
    }

    private static long floorLong(double value) {
        long integer = (long) value;
        return value < integer ? integer - 1L : integer;
    }

    private static final class Perlin {
        private final int[] permutation = new int[512];
        private final double xOffset;
        private final double yOffset;
        private final double zOffset;

        Perlin(Random random) {
            xOffset = random.nextDouble() * 256.0;
            yOffset = random.nextDouble() * 256.0;
            zOffset = random.nextDouble() * 256.0;
            for (int i = 0; i < 256; i++) {
                permutation[i] = i;
            }
            for (int i = 0; i < 256; i++) {
                int j = random.nextInt(256 - i) + i;
                int value = permutation[i];
                permutation[i] = permutation[j];
                permutation[j] = value;
                permutation[i + 256] = permutation[i];
            }
        }

        double noise(double x, double y, double z) {
            x += xOffset;
            y += yOffset;
            z += zOffset;

            int floorX = floor(x);
            int floorY = floor(y);
            int floorZ = floor(z);
            int cellX = floorX & 255;
            int cellY = floorY & 255;
            int cellZ = floorZ & 255;
            x -= floorX;
            y -= floorY;
            z -= floorZ;

            double fadeX = fade(x);
            double fadeY = fade(y);
            double fadeZ = fade(z);

            int a = permutation[cellX] + cellY;
            int aa = permutation[a] + cellZ;
            int ab = permutation[a + 1] + cellZ;
            int b = permutation[cellX + 1] + cellY;
            int ba = permutation[b] + cellZ;
            int bb = permutation[b + 1] + cellZ;

            return lerp(fadeZ,
                    lerp(fadeY,
                            lerp(fadeX, grad(permutation[aa], x, y, z),
                                    grad(permutation[ba], x - 1.0, y, z)),
                            lerp(fadeX, grad(permutation[ab], x, y - 1.0, z),
                                    grad(permutation[bb], x - 1.0, y - 1.0, z))),
                    lerp(fadeY,
                            lerp(fadeX, grad(permutation[aa + 1], x, y, z - 1.0),
                                    grad(permutation[ba + 1], x - 1.0, y, z - 1.0)),
                            lerp(fadeX, grad(permutation[ab + 1], x, y - 1.0, z - 1.0),
                                    grad(permutation[bb + 1], x - 1.0, y - 1.0, z - 1.0))));
        }

        void add(double[] values, double x, double y, double z, int xSize, int ySize, int zSize,
                double xScale, double yScale, double zScale, double octaveScale) {
            double inverseScale = 1.0 / octaveScale;
            int index = 0;
            if (ySize == 1) {
                for (int ix = 0; ix < xSize; ix++) {
                    double sampleX = x + ix * xScale + xOffset;
                    int floorX = floor(sampleX);
                    int cellX = floorX & 255;
                    sampleX -= floorX;
                    double fadeX = fade(sampleX);

                    for (int iz = 0; iz < zSize; iz++) {
                        double sampleZ = z + iz * zScale + zOffset;
                        int floorZ = floor(sampleZ);
                        int cellZ = floorZ & 255;
                        sampleZ -= floorZ;
                        double fadeZ = fade(sampleZ);

                        int a = permutation[cellX];
                        int aa = permutation[a] + cellZ;
                        int b = permutation[cellX + 1];
                        int ba = permutation[b] + cellZ;

                        double x0 = lerp(fadeX, grad2(permutation[aa], sampleX, sampleZ),
                                grad(permutation[ba], sampleX - 1.0, 0.0, sampleZ));
                        double x1 = lerp(fadeX, grad(permutation[aa + 1], sampleX, 0.0, sampleZ - 1.0),
                                grad(permutation[ba + 1], sampleX - 1.0, 0.0, sampleZ - 1.0));
                        values[index++] += lerp(fadeZ, x0, x1) * inverseScale;
                    }
                }
                return;
            }

            for (int ix = 0; ix < xSize; ix++) {
                for (int iz = 0; iz < zSize; iz++) {
                    for (int iy = 0; iy < ySize; iy++) {
                        values[index++] += noise(x + ix * xScale, y + iy * yScale, z + iz * zScale) * inverseScale;
                    }
                }
            }
        }

        private static int floor(double value) {
            int integer = (int) value;
            return value < integer ? integer - 1 : integer;
        }

        private static double fade(double value) {
            return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
        }

        private static double lerp(double value, double start, double end) {
            return start + value * (end - start);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }

        private static double grad2(int hash, double x, double z) {
            int h = hash & 15;
            double u = (1 - ((h & 8) >> 3)) * x;
            double v = h >= 4 ? h != 12 && h != 14 ? z : x : 0.0;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}
