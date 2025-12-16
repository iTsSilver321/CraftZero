package com.craftzero.math;

import java.util.Random;

/**
 * Perlin and Simplex noise implementation for terrain generation.
 * Provides 2D noise for heightmaps and 3D noise for caves.
 */
public class Noise {
    
    private final int[] permutation;
    private final int[] p;
    
    public Noise(long seed) {
        permutation = new int[256];
        p = new int[512];
        
        Random random = new Random(seed);
        
        // Initialize permutation table
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        
        // Shuffle
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Duplicate for wraparound
        for (int i = 0; i < 512; i++) {
            p[i] = permutation[i & 255];
        }
    }
    
    /**
     * 2D Perlin noise.
     * Returns value in range [-1, 1].
     */
    public double noise2D(double x, double y) {
        // Find unit grid cell
        int X = fastFloor(x) & 255;
        int Y = fastFloor(y) & 255;
        
        // Relative position in cell
        x -= fastFloor(x);
        y -= fastFloor(y);
        
        // Fade curves
        double u = fade(x);
        double v = fade(y);
        
        // Hash coordinates of corners
        int A = p[X] + Y;
        int B = p[X + 1] + Y;
        
        // Blend
        return lerp(v,
            lerp(u, grad2D(p[A], x, y), grad2D(p[B], x - 1, y)),
            lerp(u, grad2D(p[A + 1], x, y - 1), grad2D(p[B + 1], x - 1, y - 1))
        );
    }
    
    /**
     * 3D Perlin noise.
     * Returns value in range [-1, 1].
     */
    public double noise3D(double x, double y, double z) {
        // Find unit cube
        int X = fastFloor(x) & 255;
        int Y = fastFloor(y) & 255;
        int Z = fastFloor(z) & 255;
        
        // Relative position in cube
        x -= fastFloor(x);
        y -= fastFloor(y);
        z -= fastFloor(z);
        
        // Fade curves
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        
        // Hash coordinates of cube corners
        int A = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;
        
        // Blend
        return lerp(w,
            lerp(v,
                lerp(u, grad3D(p[AA], x, y, z), grad3D(p[BA], x - 1, y, z)),
                lerp(u, grad3D(p[AB], x, y - 1, z), grad3D(p[BB], x - 1, y - 1, z))
            ),
            lerp(v,
                lerp(u, grad3D(p[AA + 1], x, y, z - 1), grad3D(p[BA + 1], x - 1, y, z - 1)),
                lerp(u, grad3D(p[AB + 1], x, y - 1, z - 1), grad3D(p[BB + 1], x - 1, y - 1, z - 1))
            )
        );
    }
    
    /**
     * Octave noise (fractal Brownian motion).
     * Combines multiple noise samples at different frequencies.
     */
    public double octaveNoise2D(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        
        return total / maxValue;
    }
    
    /**
     * Octave noise 3D.
     */
    public double octaveNoise3D(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        
        return total / maxValue;
    }
    
    // Utility functions
    
    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
    
    private static double fade(double t) {
        // 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
    
    private static double grad2D(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    private static double grad3D(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
