package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ReleaseOneOctaveNoiseTest {
    @Test
    @DisplayName("Single-point 3D octave sampling should keep Y noise instead of using the 2D fast path")
    void singlePoint3DSamplingKeepsVerticalNoise() {
        ReleaseOneOctaveNoise noise = new ReleaseOneOctaveNoise(new Random(12345L), 8);

        double sourceStyle3D = noise.sampleNoiseOctaves3D(7, 5, -3,
                684.41200000000003, 684.41200000000003, 684.41200000000003);
        double ySizeOneFastPath = noise.generateNoiseOctaves(null, 7, 5, -3, 1, 1, 1,
                684.41200000000003, 684.41200000000003, 684.41200000000003)[0];

        assertNotEquals(ySizeOneFastPath, sourceStyle3D,
                "Release 1.0 density arrays use the 3D Perlin path; ySize=1 invokes the old 2D shortcut");
    }

    @Test
    @DisplayName("Single-layer octave grids should keep the source 2D fast path")
    void singleLayerOctaveGridsKeepSourceTwoDimensionalFastPath() {
        Random random = new Random(515151L);
        new ReleaseOneOctaveNoise(random, 16);
        new ReleaseOneOctaveNoise(random, 16);
        new ReleaseOneOctaveNoise(random, 8);
        ReleaseOneOctaveNoise netherSurfaceNoise = new ReleaseOneOctaveNoise(random, 4);

        double scale = 0.03125D;
        double integerY = netherSurfaceNoise.generateNoiseOctaves(null, 0, 109, 0,
                16, 1, 16, scale, 1.0D, scale)[0];
        double sourceFractionalY = netherSurfaceNoise.generateNoiseOctaves(null, 0, 109.0134D, 0,
                16, 1, 16, scale, 1.0D, scale)[0];

        assertEquals(integerY, sourceFractionalY,
                "ChunkProviderHell passes y=109.0134D, but ySize=1 uses NoiseGeneratorPerlin's old 2D branch");
    }
}
