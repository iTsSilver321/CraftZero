package com.craftzero.world;

import com.craftzero.graphics.Mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunk class representing a 16x256x16 section of the world.
 * Uses face culling to optimize mesh generation.
 */
public class Chunk {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 256;
    public static final int DEPTH = 16;
    public static final int TOTAL_BLOCKS = WIDTH * HEIGHT * DEPTH;

    private final int chunkX;
    private final int chunkZ;
    private final short[] blocks;
    private Mesh mesh; // Opaque
    private Mesh transparentMesh; // Transparent (glass, water, leaves)
    private boolean dirty;
    private boolean empty;

    // References to neighboring chunks for seamless face culling
    private Chunk northNeighbor; // -Z
    private Chunk southNeighbor; // +Z
    private Chunk eastNeighbor; // +X
    private Chunk westNeighbor; // -X

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[TOTAL_BLOCKS];
        this.dirty = true;
        this.empty = true;
    }

    /**
     * Convert local coordinates to array index.
     * Using Y-up with formula: index = x + (z * WIDTH) + (y * WIDTH * DEPTH)
     */
    public static int getIndex(int x, int y, int z) {
        return x + (z * WIDTH) + (y * WIDTH * DEPTH);
    }

    /**
     * Check if local coordinates are within chunk bounds.
     */
    public static boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && z >= 0 && z < DEPTH;
    }

    /**
     * Get block type at local coordinates.
     */
    public BlockType getBlock(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return BlockType.AIR;
        }
        return BlockType.fromId(blocks[getIndex(x, y, z)]);
    }

    /**
     * Set block at local coordinates.
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        if (!isInBounds(x, y, z)) {
            return;
        }
        blocks[getIndex(x, y, z)] = (short) type.getId();
        dirty = true;
        empty = false;
    }

    /**
     * Get block ID at local coordinates.
     */
    public int getBlockId(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return 0;
        }
        return blocks[getIndex(x, y, z)];
    }

    /**
     * Check if face should be rendered (neighbor is air or transparent).
     */
    private boolean shouldRenderFace(int x, int y, int z, int face, BlockType currentBlock) {
        int nx = x, ny = y, nz = z;

        switch (face) {
            case Block.FACE_TOP -> ny++;
            case Block.FACE_BOTTOM -> ny--;
            case Block.FACE_NORTH -> nz--;
            case Block.FACE_SOUTH -> nz++;
            case Block.FACE_EAST -> nx++;
            case Block.FACE_WEST -> nx--;
        }

        BlockType neighbor;

        // Check chunk boundaries
        if (ny < 0 || ny >= HEIGHT) {
            return face == Block.FACE_TOP; // Only render top face at world top
        }

        if (nx < 0) {
            neighbor = westNeighbor != null ? westNeighbor.getBlock(WIDTH - 1, ny, nz) : BlockType.AIR;
        } else if (nx >= WIDTH) {
            neighbor = eastNeighbor != null ? eastNeighbor.getBlock(0, ny, nz) : BlockType.AIR;
        } else if (nz < 0) {
            neighbor = northNeighbor != null ? northNeighbor.getBlock(nx, ny, DEPTH - 1) : BlockType.AIR;
        } else if (nz >= DEPTH) {
            neighbor = southNeighbor != null ? southNeighbor.getBlock(nx, ny, 0) : BlockType.AIR;
        } else {
            neighbor = getBlock(nx, ny, nz);
        }

        // Optimization: Never render face between identical blocks (water/glass)
        if (currentBlock == neighbor) {
            return false;
        }

        // Render face if neighbor is air or transparent (and current block isn't the
        // same transparent type)
        if (neighbor.isAir()) {
            return true;
        }
        if (neighbor.isTransparent() && !currentBlock.isTransparent()) {
            return true;
        }
        if (currentBlock.isTransparent()) {
            // If we are transparent, render unless neighbor occludes us
            return !neighbor.occludesFace();
        }

        return false;
    }

    /**
     * Build the chunk mesh with face culling optimization.
     */
    public void buildMesh() {
        if (!dirty) {
            return;
        }

        // Clean up old meshes
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        if (transparentMesh != null) {
            transparentMesh.cleanup();
            transparentMesh = null;
        }

        // Opaque buffers
        List<Float> opaquePositions = new ArrayList<>();
        List<Float> opaqueTexCoords = new ArrayList<>();
        List<Float> opaqueNormals = new ArrayList<>();
        List<Integer> opaqueIndices = new ArrayList<>();
        int opaqueVertexCount = 0;

        // Transparent buffers
        List<Float> transPositions = new ArrayList<>();
        List<Float> transTexCoords = new ArrayList<>();
        List<Float> transNormals = new ArrayList<>();
        List<Integer> transIndices = new ArrayList<>();
        int transVertexCount = 0;

        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    BlockType type = getBlock(x, y, z);

                    if (type.isAir()) {
                        continue;
                    }

                    // World position for vertex generation
                    int worldX = chunkX * WIDTH + x;
                    int worldZ = chunkZ * DEPTH + z;

                    // Check each face
                    for (int face = 0; face < 6; face++) {
                        if (shouldRenderFace(x, y, z, face, type)) {
                            // Select buffers based on transparency
                            List<Float> positions;
                            List<Float> texCoords;
                            List<Float> normals;
                            List<Integer> indices;
                            int vCount;

                            if (type.isTransparent()) {
                                positions = transPositions;
                                texCoords = transTexCoords;
                                normals = transNormals;
                                indices = transIndices;
                                vCount = transVertexCount;
                            } else {
                                positions = opaquePositions;
                                texCoords = opaqueTexCoords;
                                normals = opaqueNormals;
                                indices = opaqueIndices;
                                vCount = opaqueVertexCount;
                            }

                            // Add vertices
                            float[] faceVerts = Block.getFaceVertices(face, worldX, y, worldZ);
                            for (float v : faceVerts) {
                                positions.add(v);
                            }

                            // Add texture coordinates
                            float[] faceTexCoords = Block.getFaceTexCoords(type, face);
                            for (float t : faceTexCoords) {
                                texCoords.add(t);
                            }

                            // Add normals
                            float[] faceNormals = Block.getFaceNormals(face);
                            for (float n : faceNormals) {
                                normals.add(n);
                            }

                            // Add indices
                            int[] faceIndices = Block.getFaceIndices(vCount);
                            for (int idx : faceIndices) {
                                indices.add(idx);
                            }

                            if (type.isTransparent()) {
                                transVertexCount += 4;
                            } else {
                                opaqueVertexCount += 4;
                            }
                        }
                    }
                }
            }
        }

        // Create opaque mesh
        if (!opaquePositions.isEmpty()) {
            mesh = new Mesh(
                    toFloatArray(opaquePositions),
                    toFloatArray(opaqueTexCoords),
                    toFloatArray(opaqueNormals),
                    toIntArray(opaqueIndices));
        }

        // Create transparent mesh
        if (!transPositions.isEmpty()) {
            transparentMesh = new Mesh(
                    toFloatArray(transPositions),
                    toFloatArray(transTexCoords),
                    toFloatArray(transNormals),
                    toIntArray(transIndices));
        }

        empty = (mesh == null && transparentMesh == null);
        dirty = false;
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Mesh getTransparentMesh() {
        return transparentMesh;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isEmpty() {
        return empty;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getWorldX() {
        return chunkX * WIDTH;
    }

    public int getWorldZ() {
        return chunkZ * DEPTH;
    }

    public void setNeighbors(Chunk north, Chunk south, Chunk east, Chunk west) {
        if (this.northNeighbor != north || this.southNeighbor != south ||
                this.eastNeighbor != east || this.westNeighbor != west) {
            this.northNeighbor = north;
            this.southNeighbor = south;
            this.eastNeighbor = east;
            this.westNeighbor = west;
            this.dirty = true;
        }
    }

    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        if (transparentMesh != null) {
            transparentMesh.cleanup();
            transparentMesh = null;
        }
    }
}
