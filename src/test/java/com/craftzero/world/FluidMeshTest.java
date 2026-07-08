package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FluidMeshTest {
    @Test
    @DisplayName("Fluid mesh should use metadata height instead of a full block cuboid")
    void fluidMeshUsesMetadataHeight() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 10, 1, BlockType.FLOWING_WATER, 4);

        ChunkMeshData mesh = ChunkMeshBuilder.buildMeshData(chunk);

        assertTrue(mesh.hasTransparentMesh());
        assertEquals(10.0f + 4.0f / 9.0f, maxTransparentY(mesh), 0.0001f);
    }

    @Test
    @DisplayName("Fluid mesh should sample diagonal chunk neighbors for corner heights")
    void fluidMeshSamplesDiagonalChunkNeighborsForCornerHeights() {
        Chunk center = new Chunk(0, 0);
        Chunk north = new Chunk(0, -1);
        Chunk west = new Chunk(-1, 0);
        Chunk northwest = new Chunk(-1, -1);
        center.setNeighbors(north, null, null, west);
        north.setNeighbors(null, center, null, northwest);
        west.setNeighbors(northwest, null, center, null);

        center.setBlock(0, 10, 0, BlockType.FLOWING_WATER, 7);
        northwest.setBlock(15, 10, 15, BlockType.WATER, 0);

        ChunkMeshData mesh = ChunkMeshBuilder.buildMeshData(center);

        assertEquals(10.5f, maxTransparentY(mesh), 0.0001f);
    }

    @Test
    @DisplayName("Fluid mesh should hide same-fluid internal side faces")
    void fluidMeshHidesInternalFaces() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 10, 1, BlockType.WATER, 0);
        chunk.setBlock(2, 10, 1, BlockType.WATER, 0);

        ChunkMeshData mesh = ChunkMeshBuilder.buildMeshData(chunk);

        int faceCount = mesh.transIndices.length / 6;
        assertEquals(10, faceCount);
    }

    @Test
    @DisplayName("Fluid mesh should not draw shoreline side faces hidden by solid blocks")
    void fluidMeshHidesSidesAgainstSolidShore() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 10, 1, BlockType.WATER, 0);
        chunk.setBlock(0, 10, 1, BlockType.SAND, 0);

        ChunkMeshData mesh = ChunkMeshBuilder.buildMeshData(chunk);

        int faceCount = mesh.transIndices.length / 6;
        assertEquals(5, faceCount);
    }

    private static float maxTransparentY(ChunkMeshData mesh) {
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 1; i < mesh.transPositions.length; i += 3) {
            maxY = Math.max(maxY, mesh.transPositions[i]);
        }
        return maxY;
    }
}
