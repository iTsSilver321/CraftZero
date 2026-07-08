package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NetherCaveGeneratorTest {
    @Test
    @DisplayName("Release 1.0 Nether cave carver should carve air without replacing surface patches")
    void releaseOneNetherCavesCarveAirWithoutReplacingSurfacePatches() {
        Chunk chunk = filledChunk();
        chunk.setBlock(0, 20, 0, BlockType.GRAVEL);
        chunk.setBlock(0, 21, 0, BlockType.SOUL_SAND);

        new NetherCaveGenerator().generate(chunk, 515151L);

        assertEquals(778, countBlocks(chunk, BlockType.AIR));
        assertEquals(0, countLava(chunk));
        assertSame(BlockType.GRAVEL, chunk.getBlock(0, 20, 0));
        assertSame(BlockType.SOUL_SAND, chunk.getBlock(0, 21, 0));
        assertSame(BlockType.AIR, chunk.getBlock(0, 19, 0));
        assertSame(BlockType.AIR, chunk.getBlock(0, 86, 7));
        assertSame(BlockType.NETHERRACK, chunk.getBlock(8, 64, 8));
    }

    @Test
    @DisplayName("Release 1.0 Nether cave carver should match source block-output vectors")
    void releaseOneNetherCavesMatchSourceBlockOutputVectors() {
        List<NetherCaveExpectation> expectations = List.of(
                new NetherCaveExpectation(515151L, 0, 0, 780, 31988, -1109311095,
                        BlockType.AIR, BlockType.AIR, BlockType.NETHERRACK),
                new NetherCaveExpectation(515151L, -16, -6, 223, 32545, 408327378,
                        BlockType.NETHERRACK, BlockType.NETHERRACK, BlockType.NETHERRACK),
                new NetherCaveExpectation(515151L, 7, -8, 280, 32488, 1512822229,
                        BlockType.NETHERRACK, BlockType.NETHERRACK, BlockType.NETHERRACK),
                new NetherCaveExpectation(1234L, 0, 0, 1246, 31522, -1354028769,
                        BlockType.AIR, BlockType.NETHERRACK, BlockType.NETHERRACK),
                new NetherCaveExpectation(1234L, 7, -8, 520, 32248, 1191294851,
                        BlockType.NETHERRACK, BlockType.NETHERRACK, BlockType.NETHERRACK),
                new NetherCaveExpectation(987654321L, -3, 5, 280, 32488, -725047423,
                        BlockType.NETHERRACK, BlockType.NETHERRACK, BlockType.NETHERRACK));

        for (NetherCaveExpectation expectation : expectations) {
            assertNetherCaveMatchesExpectation(expectation);
        }
    }

    private static Chunk filledChunk() {
        return filledChunk(0, 0);
    }

    private static Chunk filledChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, BlockType.NETHERRACK);
                }
            }
        }
        return chunk;
    }

    private static void assertNetherCaveMatchesExpectation(NetherCaveExpectation expectation) {
        Chunk chunk = filledChunk(expectation.chunkX(), expectation.chunkZ());
        new NetherCaveGenerator().generate(chunk, expectation.seed());
        String label = "seed " + expectation.seed() + " chunk ("
                + expectation.chunkX() + "," + expectation.chunkZ() + ")";

        assertEquals(expectation.airCount(), countBlocks(chunk, BlockType.AIR), label + " air count");
        assertEquals(0, countLava(chunk), label + " lava count");
        assertEquals(expectation.netherrackCount(), countBlocks(chunk, BlockType.NETHERRACK),
                label + " netherrack count");
        assertEquals(expectation.blockHash(), blockIdHash(chunk), label + " block hash");
        assertSame(expectation.sampleA(), chunk.getBlock(0, 19, 0), label + " sample 0,19,0");
        assertSame(expectation.sampleB(), chunk.getBlock(0, 86, 7), label + " sample 0,86,7");
        assertSame(expectation.sampleC(), chunk.getBlock(8, 64, 8), label + " sample 8,64,8");
    }

    private static int countBlocks(Chunk chunk, BlockType type) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countLava(Chunk chunk) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z).isLava()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int blockIdHash(Chunk chunk) {
        int hash = 1;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    hash = 31 * hash + chunk.getBlock(x, y, z).getId();
                }
            }
        }
        return hash;
    }

    private record NetherCaveExpectation(long seed, int chunkX, int chunkZ, int airCount,
            int netherrackCount, int blockHash, BlockType sampleA, BlockType sampleB, BlockType sampleC) {
    }
}
