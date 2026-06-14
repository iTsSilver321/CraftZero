package com.craftzero.world;

/**
 * Holds raw mesh data computed on a background thread.
 * Contains pure Java arrays - no OpenGL calls.
 * This data is uploaded to GPU on the main thread via Chunk.applyMeshData().
 */
public class ChunkMeshData {

    // Opaque mesh data
    public final float[] opaquePositions;
    public final float[] opaqueTexCoords;
    public final float[] opaqueNormals;
    public final float[] opaqueColors;
    public final int[] opaqueIndices;

    // Cutout mesh data (alpha-tested blocks such as leaves)
    public final float[] cutoutPositions;
    public final float[] cutoutTexCoords;
    public final float[] cutoutNormals;
    public final float[] cutoutColors;
    public final int[] cutoutIndices;

    // Transparent mesh data
    public final float[] transPositions;
    public final float[] transTexCoords;
    public final float[] transNormals;
    public final float[] transColors;
    public final int[] transIndices;

    // Empty flag
    public final boolean empty;

    public ChunkMeshData(
            float[] opaquePositions, float[] opaqueTexCoords, float[] opaqueNormals,
            float[] opaqueColors, int[] opaqueIndices,
            float[] cutoutPositions, float[] cutoutTexCoords, float[] cutoutNormals,
            float[] cutoutColors, int[] cutoutIndices,
            float[] transPositions, float[] transTexCoords, float[] transNormals,
            float[] transColors, int[] transIndices) {

        this.opaquePositions = opaquePositions;
        this.opaqueTexCoords = opaqueTexCoords;
        this.opaqueNormals = opaqueNormals;
        this.opaqueColors = opaqueColors;
        this.opaqueIndices = opaqueIndices;

        this.cutoutPositions = cutoutPositions;
        this.cutoutTexCoords = cutoutTexCoords;
        this.cutoutNormals = cutoutNormals;
        this.cutoutColors = cutoutColors;
        this.cutoutIndices = cutoutIndices;

        this.transPositions = transPositions;
        this.transTexCoords = transTexCoords;
        this.transNormals = transNormals;
        this.transColors = transColors;
        this.transIndices = transIndices;

        this.empty = (opaquePositions.length == 0 && cutoutPositions.length == 0 && transPositions.length == 0);
    }

    /**
     * Check if this mesh data has any opaque geometry.
     */
    public boolean hasOpaqueMesh() {
        return opaquePositions.length > 0;
    }

    public boolean hasCutoutMesh() {
        return cutoutPositions.length > 0;
    }

    /**
     * Check if this mesh data has any transparent geometry.
     */
    public boolean hasTransparentMesh() {
        return transPositions.length > 0;
    }
}
