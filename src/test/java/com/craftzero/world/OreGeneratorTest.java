package com.craftzero.world;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreGeneratorTest {
    @Test
    @DisplayName("Ore generation should use raw Release 1.0 origins and replay future neighbor spills")
    void oreGenerationUsesRawReleaseOneOrigins() {
        Chunk chunk = solidStoneChunk(0, 0);

        new OreGenerator().generate(chunk, 1L);

        assertSame(BlockType.LAPIS_ORE, chunk.getBlock(0, 5, 6),
                "BiomeDecorator ore helpers draw x/z as origin + nextInt(16), without the decorator +8 offset");
        assertSame(BlockType.COAL_ORE, chunk.getBlock(15, 123, 5),
                "Unshifted eastern population origins can spill ore back into the target chunk edge");
        assertSame(BlockType.IRON_ORE, chunk.getBlock(2, 17, 15),
                "Unshifted southern population origins can spill ore back into the target chunk edge");
    }

    @Test
    @DisplayName("WorldGenMinable endpoints should not add a non-source half-block x/z offset")
    void oreVeinEndpointCentersMatchReleaseOneMath() throws Exception {
        Chunk chunk = solidStoneChunk(0, 0);

        Method generateVein = OreGenerator.class.getDeclaredMethod("generateVein",
                Chunk.class, Random.class, int.class, int.class, int.class, BlockType.class, int.class);
        generateVein.setAccessible(true);
        generateVein.invoke(new OreGenerator(), chunk, new FixedVeinRandom(), 8, 8, 8, BlockType.IRON_ORE, 8);

        assertSame(BlockType.IRON_ORE, chunk.getBlock(7, 7, 7),
                "Release 1.0 line endpoints are centered on the shifted start coordinate, not start + 0.5");
        assertSame(BlockType.STONE, chunk.getBlock(8, 7, 9),
                "The removed half-block endpoint offset would incorrectly shift this vein north/east");
    }

    @Test
    @DisplayName("WorldGenMinable should use the Release sine table for vein boundaries")
    void oreVeinBoundariesUseReleaseOneSineTable() throws Exception {
        Chunk chunk = solidStoneChunk(0, 0);

        Method generateVein = OreGenerator.class.getDeclaredMethod("generateVein",
                Chunk.class, Random.class, int.class, int.class, int.class, BlockType.class, int.class);
        generateVein.setAccessible(true);
        generateVein.invoke(new OreGenerator(), chunk, new FixedVeinRandom(0.2882f, 0.05), 8, 8, 8,
                BlockType.IRON_ORE, 6);

        assertSame(BlockType.IRON_ORE, chunk.getBlock(7, 7, 7),
                "The old Math.sin/cos path misses this source sine-table boundary block");
        assertSame(BlockType.IRON_ORE, chunk.getBlock(7, 8, 7));
        assertSame(BlockType.IRON_ORE, chunk.getBlock(8, 7, 8));
        assertSame(BlockType.IRON_ORE, chunk.getBlock(8, 8, 8));
    }

    @Test
    @DisplayName("WorldGenMinable should support source scratch readers and writers")
    void oreVeinUsesSuppliedWorldReaderAndWriter() throws Exception {
        Set<String> writes = new HashSet<>();
        OreGenerator.BlockReader reader = (x, y, z) -> BlockType.STONE;
        OreGenerator.BlockWriter writer = (x, y, z, block) -> writes.add(x + "," + y + "," + z + ":" + block);
        Method generateVein = OreGenerator.class.getDeclaredMethod("generateVein",
                OreGenerator.BlockReader.class, OreGenerator.BlockWriter.class, Random.class,
                int.class, int.class, int.class, BlockType.class, int.class);
        generateVein.setAccessible(true);

        generateVein.invoke(new OreGenerator(), reader, writer, new FixedVeinRandom(),
                -1, 8, 8, BlockType.IRON_ORE, 8);

        assertTrue(writes.stream().anyMatch(write -> write.startsWith("-")),
                "Source scratch ore replay should be able to write world coordinates outside a target chunk");
    }

    private static Chunk solidStoneChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }
        return chunk;
    }

    private static final class FixedVeinRandom extends Random {
        private final float nextFloat;
        private final double nextDouble;

        private FixedVeinRandom() {
            this(0.0f, 0.99);
        }

        private FixedVeinRandom(float nextFloat, double nextDouble) {
            this.nextFloat = nextFloat;
            this.nextDouble = nextDouble;
        }

        @Override
        public float nextFloat() {
            return nextFloat;
        }

        @Override
        public int nextInt(int bound) {
            if (bound != 3) {
                throw new AssertionError("Unexpected ore-vein bound: " + bound);
            }
            return 2;
        }

        @Override
        public double nextDouble() {
            return nextDouble;
        }
    }
}
