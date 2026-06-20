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
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 1; i < mesh.transPositions.length; i += 3) {
            maxY = Math.max(maxY, mesh.transPositions[i]);
        }
        assertEquals(10.0f + 4.0f / 9.0f, maxY, 0.0001f);
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
}
