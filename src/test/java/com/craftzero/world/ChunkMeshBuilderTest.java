package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkMeshBuilderTest {

    @Test
    @DisplayName("Fancy leaves should render leaf-to-leaf cutout faces")
    void fancyLeavesKeepNeighborFaces() {
        try {
            BlockType.setFancyGraphics(true);
            Chunk chunk = adjacentLeavesChunk();

            ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

            assertEquals(12 * 4 * 3, data.cutoutPositions.length);
        } finally {
            BlockType.setFancyGraphics(true);
        }
    }

    @Test
    @DisplayName("Fast leaves should still cull shared leaf faces")
    void fastLeavesCullNeighborFaces() {
        try {
            BlockType.setFancyGraphics(false);
            Chunk chunk = adjacentLeavesChunk();

            ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

            assertEquals(10 * 4 * 3, data.cutoutPositions.length);
        } finally {
            BlockType.setFancyGraphics(true);
        }
    }

    private static Chunk adjacentLeavesChunk() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.LEAVES);
        chunk.setBlock(2, 64, 1, BlockType.LEAVES);
        return chunk;
    }
}
