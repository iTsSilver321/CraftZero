package com.craftzero.world;

import com.craftzero.graphics.Mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunk class representing a 16x256x16 section of the world.
 * Uses face culling to optimize mesh generation.
 * Uses staged loading: EMPTY → GENERATED → LIGHTED → READY
 */
public class Chunk {

    /**
     * Chunk loading states for staged processing.
     */
    public enum ChunkState {
        EMPTY, // Just created, no terrain
        GENERATING, // Terrain being generated async
        GENERATED, // Terrain done, needs lighting
        LIGHTING, // Lighting being calculated
        LIGHTED, // Lighting done, ready for mesh
        MESHING, // Mesh being built async
        READY // Fully loaded and rendered
    }

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

    // Chunk state for staged loading
    private volatile ChunkState state = ChunkState.EMPTY;

    // Sky light system (0-15 per block, stored as nibbles)
    private byte[] skyLight; // Nibble storage: 2 values per byte
    private int[] heightMap; // Highest solid block per column
    private boolean lightDirty;

    // References to neighboring chunks for seamless face culling
    private Chunk northNeighbor; // -Z
    private Chunk southNeighbor; // +Z
    private Chunk eastNeighbor; // +X
    private Chunk westNeighbor; // -X

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[TOTAL_BLOCKS];
        this.skyLight = new byte[TOTAL_BLOCKS / 2 + 1]; // Nibble storage
        this.heightMap = new int[WIDTH * DEPTH];
        this.dirty = true;
        this.lightDirty = true;
        this.empty = true;
        this.state = ChunkState.EMPTY;
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
        lightDirty = true; // Recalculate sky light when blocks change
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
     * Get Minecraft-style face shading multiplier.
     * Different faces have different inherent brightness.
     */
    private float getFaceShade(int face) {
        return switch (face) {
            case Block.FACE_TOP -> 1.0f; // Top = 100% (fully lit)
            case Block.FACE_BOTTOM -> 0.5f; // Bottom = 50% (shadowed)
            case Block.FACE_NORTH, Block.FACE_SOUTH -> 0.8f; // N/S = 80%
            case Block.FACE_EAST, Block.FACE_WEST -> 0.6f; // E/W = 60%
            default -> 1.0f;
        };
    }

    /**
     * Convert sky light level (0-15) to brightness using Minecraft's gamma curve.
     * Uses full range so deep caves are very dark but still barely visible.
     */
    private float getSkyLightBrightness(int lightLevel) {
        // Use full 15 levels with proper gamma curve
        float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
        // Minecraft gamma curve: f / (3 - 2*f)
        // This makes low levels (1-4) very dark
        float gamma = f / (3.0f - 2.0f * f);
        // Minimum floor of 0.08 - dark but visible in deepest caves
        return Math.max(0.08f, gamma);
    }

    /**
     * Get smooth vertex light by sampling the 4 blocks touching a vertex corner.
     * This creates ambient occlusion effect and smooth light gradients.
     * Only samples from non-solid blocks (air, transparent) to avoid dark spots.
     * 
     * @param lx,         ly, lz - the light sampling position (adjacent to face)
     * @param face        - which face we're lighting
     * @param vertexIndex - which of the 4 vertices (0-3)
     * @return averaged sky light level (0-15)
     */
    private int getVertexLight(int lx, int ly, int lz, int face, int vertexIndex) {
        // Offsets for the 4 blocks that touch each vertex corner
        int[][] offsets = getVertexLightOffsets(face, vertexIndex);

        int totalLight = 0;
        int count = 0;

        for (int[] offset : offsets) {
            int sx = lx + offset[0];
            int sy = ly + offset[1];
            int sz = lz + offset[2];

            // Only sample light from non-solid blocks (air or transparent)
            BlockType block = getBlock(sx, sy, sz);
            if (block.isAir() || block.isTransparent()) {
                totalLight += getSkyLight(sx, sy, sz);
                count++;
            }
        }

        // If all neighbors are solid, use the center block's light
        if (count == 0) {
            return getSkyLight(lx, ly, lz);
        }

        return totalLight / count;
    }

    /**
     * Get the 4 block offsets for sampling vertex corner light.
     * Each vertex samples from the center block (0,0,0) and 3 diagonal neighbors.
     */
    private int[][] getVertexLightOffsets(int face, int vertexIndex) {
        // For each face, define the 4 vertices' neighbor sampling offsets
        // Vertex order follows Block.getFaceVertices() winding: 0=BL, 1=BR, 2=TR, 3=TL
        // Each vertex samples from 4 blocks: center + 3 adjacent in the face plane

        switch (face) {
            case Block.FACE_TOP: // Y+ face, sample in XZ plane at ly
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, -1 }, { -1, 0, -1 } }; // left-back
                    case 1 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 1, 0, -1 } }; // right-back
                    case 2 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } }; // right-front
                    case 3 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 1 } }; // left-front
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_BOTTOM: // Y- face
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 1 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 1, 0, -1 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, -1 }, { -1, 0, -1 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_NORTH: // Z- face, sample in XY plane
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 1, -1, 0 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { -1, -1, 0 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { -1, 1, 0 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_SOUTH: // Z+ face
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { -1, -1, 0 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 1, -1, 0 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { -1, 1, 0 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_EAST: // X+ face, sample in YZ plane
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { 0, 0, -1 }, { 0, -1, 0 }, { 0, -1, -1 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, -1, 0 }, { 0, -1, 1 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 1 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 1, 0 }, { 0, 1, -1 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_WEST: // X- face
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, -1, 0 }, { 0, -1, 1 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 0, 0, -1 }, { 0, -1, 0 }, { 0, -1, -1 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 1, 0 }, { 0, 1, -1 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 1 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            default:
                return new int[][] { { 0, 0, 0 } };
        }
    }

    /**
     * Build the chunk mesh with face culling optimization.
     */
    public void buildMesh() {
        if (!dirty) {
            return;
        }

        // Calculate sky lighting first
        calculateSkyLight();

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
        List<Float> opaqueColors = new ArrayList<>();
        List<Integer> opaqueIndices = new ArrayList<>();
        int opaqueVertexCount = 0;

        // Transparent buffers
        List<Float> transPositions = new ArrayList<>();
        List<Float> transTexCoords = new ArrayList<>();
        List<Float> transNormals = new ArrayList<>();
        List<Float> transColors = new ArrayList<>();
        List<Integer> transIndices = new ArrayList<>();
        int transVertexCount = 0;

        // Get biome colors from colormap
        float[] grassColor = com.craftzero.graphics.BiomeColormap.getGrassColor();
        float[] foliageColor = com.craftzero.graphics.BiomeColormap.getFoliageColor();
        float[] waterColor = com.craftzero.graphics.BiomeColormap.getWaterColor();

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

                    // Determine vertex color based on block type
                    float[] blockColor;
                    if (type == BlockType.GRASS) {
                        blockColor = grassColor;
                    } else if (type == BlockType.LEAVES) {
                        blockColor = foliageColor;
                    } else if (type == BlockType.WATER) {
                        blockColor = waterColor;
                    } else {
                        blockColor = new float[] { 1.0f, 1.0f, 1.0f }; // No tint
                    }

                    // Check each face
                    for (int face = 0; face < 6; face++) {
                        if (shouldRenderFace(x, y, z, face, type)) {
                            // Select buffers based on transparency
                            List<Float> positions;
                            List<Float> texCoords;
                            List<Float> normals;
                            List<Float> colors;
                            List<Integer> indices;
                            int vCount;

                            if (type.isTransparent()) {
                                positions = transPositions;
                                texCoords = transTexCoords;
                                normals = transNormals;
                                colors = transColors;
                                indices = transIndices;
                                vCount = transVertexCount;
                            } else {
                                positions = opaquePositions;
                                texCoords = opaqueTexCoords;
                                normals = opaqueNormals;
                                colors = opaqueColors;
                                indices = opaqueIndices;
                                vCount = opaqueVertexCount;
                            }

                            // Determine block height (surface water is lower)
                            float height = 1.0f;
                            if (type == BlockType.WATER) {
                                // Check block above locally
                                boolean waterAbove = false;
                                if (y < HEIGHT - 1) {
                                    waterAbove = (getBlock(x, y + 1, z) == BlockType.WATER);
                                }
                                if (!waterAbove) {
                                    height = 0.875f; // 7/8 height for surface water
                                }
                            }

                            // Add vertices
                            float[] faceVerts = Block.getFaceVertices(face, worldX, y, worldZ, height);
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

                            // Add colors (4 vertices per face)
                            // Apply Minecraft-style face shading and SMOOTH sky light
                            float faceShade = getFaceShade(face);

                            // Get light sampling position (adjacent block where light comes from)
                            int lx = x, ly = y, lz = z;
                            switch (face) {
                                case Block.FACE_TOP -> ly++;
                                case Block.FACE_BOTTOM -> ly--;
                                case Block.FACE_NORTH -> lz--;
                                case Block.FACE_SOUTH -> lz++;
                                case Block.FACE_EAST -> lx++;
                                case Block.FACE_WEST -> lx--;
                            }

                            // For grass blocks, only tint the TOP face
                            float[] faceColor = blockColor;
                            if (type == BlockType.GRASS && face != Block.FACE_TOP) {
                                faceColor = new float[] { 1.0f, 1.0f, 1.0f };
                            }

                            // SMOOTH LIGHTING: sample each vertex corner separately
                            for (int v = 0; v < 4; v++) {
                                // Get averaged light level for this vertex corner
                                int vertexLight = getVertexLight(lx, ly, lz, face, v);
                                float skyBrightness = getSkyLightBrightness(vertexLight);
                                float brightness = faceShade * skyBrightness;

                                colors.add(faceColor[0] * brightness);
                                colors.add(faceColor[1] * brightness);
                                colors.add(faceColor[2] * brightness);
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
                    toFloatArray(opaqueColors),
                    toIntArray(opaqueIndices));
        }

        // Create transparent mesh
        if (!transPositions.isEmpty()) {
            transparentMesh = new Mesh(
                    toFloatArray(transPositions),
                    toFloatArray(transTexCoords),
                    toFloatArray(transNormals),
                    toFloatArray(transColors),
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

    // Alias for compatibility
    public int getX() {
        return chunkX;
    }

    public int getZ() {
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

    // ============ CHUNK STATE MANAGEMENT ============

    public ChunkState getState() {
        return state;
    }

    public void setState(ChunkState state) {
        this.state = state;
    }

    /**
     * Check if all 4 neighbors exist and are at least at the given state.
     */
    public boolean hasAllNeighborsAtLeast(ChunkState minState) {
        return northNeighbor != null && northNeighbor.getState().ordinal() >= minState.ordinal()
                && southNeighbor != null && southNeighbor.getState().ordinal() >= minState.ordinal()
                && eastNeighbor != null && eastNeighbor.getState().ordinal() >= minState.ordinal()
                && westNeighbor != null && westNeighbor.getState().ordinal() >= minState.ordinal();
    }

    /**
     * Check if all 4 neighbors exist (regardless of state).
     */
    public boolean hasAllNeighbors() {
        return northNeighbor != null && southNeighbor != null
                && eastNeighbor != null && westNeighbor != null;
    }

    // ============ SKY LIGHT SYSTEM ============

    /**
     * Get sky light level for a block (0-15).
     */
    public int getSkyLight(int x, int y, int z) {
        if (!isInBounds(x, y, z))
            return 15; // Default to full light outside bounds
        int index = getIndex(x, y, z);
        return getNibble(skyLight, index);
    }

    /**
     * Set sky light level for a block (0-15).
     */
    private void setSkyLight(int x, int y, int z, int level) {
        if (!isInBounds(x, y, z))
            return;
        int index = getIndex(x, y, z);
        setNibble(skyLight, index, level);
    }

    /**
     * Get nibble (4-bit value) from byte array.
     */
    private int getNibble(byte[] array, int index) {
        int byteIndex = index >> 1;
        if (byteIndex >= array.length)
            return 15;
        if ((index & 1) == 0) {
            return array[byteIndex] & 0x0F;
        } else {
            return (array[byteIndex] >> 4) & 0x0F;
        }
    }

    /**
     * Set nibble (4-bit value) in byte array.
     */
    private void setNibble(byte[] array, int index, int value) {
        int byteIndex = index >> 1;
        if (byteIndex >= array.length)
            return;
        value = Math.max(0, Math.min(15, value));
        if ((index & 1) == 0) {
            array[byteIndex] = (byte) ((array[byteIndex] & 0xF0) | (value & 0x0F));
        } else {
            array[byteIndex] = (byte) ((array[byteIndex] & 0x0F) | ((value << 4) & 0xF0));
        }
    }

    /**
     * Calculate sky light for the entire chunk.
     * Uses height map + BFS flood fill for proper horizontal propagation.
     */
    public void calculateSkyLight() {
        if (!lightDirty)
            return;

        // Clear all sky light first
        java.util.Arrays.fill(skyLight, (byte) 0);

        // Queue for BFS flood fill: each entry is {x, y, z, lightLevel}
        java.util.Queue<int[]> lightQueue = new java.util.LinkedList<>();

        // Step 1: Calculate height map and seed ALL sky-exposed blocks
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                // Find highest opaque block
                int height = -1;
                for (int y = HEIGHT - 1; y >= 0; y--) {
                    BlockType block = getBlock(x, y, z);
                    if (!block.isAir() && !block.isTransparent()) {
                        height = y;
                        break;
                    }
                }
                heightMap[x + z * WIDTH] = height;

                // Set full sky light for ALL air blocks above height map
                // AND add them all to the queue so light propagates everywhere
                for (int y = HEIGHT - 1; y > height; y--) {
                    setSkyLight(x, y, z, 15);
                    // Add ALL sky-exposed air to queue - this ensures light
                    // can propagate into caves from any opening
                    lightQueue.add(new int[] { x, y, z, 15 });
                }
            }
        }

        // Step 1.5: Seed light from neighbor chunks at borders
        // This allows light to propagate from lit areas in neighbors into this chunk
        seedLightFromNeighbors(lightQueue);

        // Step 2: BFS Flood Fill - spread light in all 6 directions
        int[][] directions = {
                { 1, 0, 0 }, { -1, 0, 0 }, // East, West
                { 0, 1, 0 }, { 0, -1, 0 }, // Up, Down
                { 0, 0, 1 }, { 0, 0, -1 } // South, North
        };

        while (!lightQueue.isEmpty()) {
            int[] current = lightQueue.poll();
            int cx = current[0], cy = current[1], cz = current[2], currentLight = current[3];

            // Spread to all 6 neighbors
            for (int[] dir : directions) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];
                int nz = cz + dir[2];

                // Bounds check
                if (!isInBounds(nx, ny, nz))
                    continue;

                BlockType neighborBlock = getBlock(nx, ny, nz);

                // Calculate light reaching neighbor
                int opacity = 1; // Default: light decreases by 1
                if (!neighborBlock.isAir()) {
                    if (neighborBlock.isTransparent()) {
                        opacity = 2; // Leaves, water absorb more light
                    } else {
                        continue; // Solid blocks stop light completely
                    }
                }

                int newLight = currentLight - opacity;
                if (newLight <= 0)
                    continue;

                // Only update if we're bringing MORE light
                int existingLight = getSkyLight(nx, ny, nz);
                if (newLight > existingLight) {
                    setSkyLight(nx, ny, nz, newLight);
                    lightQueue.add(new int[] { nx, ny, nz, newLight });
                }
            }
        }

        lightDirty = false;
    }

    /**
     * Get height of highest solid block at column (cached).
     */
    public int getHeightAt(int x, int z) {
        if (x < 0 || x >= WIDTH || z < 0 || z >= DEPTH)
            return 0;
        return heightMap[x + z * WIDTH];
    }

    /**
     * Seed light from neighbor chunks at chunk borders.
     * This allows light to propagate from lit neighbor areas into this chunk.
     */
    private void seedLightFromNeighbors(java.util.Queue<int[]> lightQueue) {
        // Check each border and pull light from neighbors

        // North border (z = 0, check neighbor at z-1)
        if (northNeighbor != null) {
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    int neighborLight = northNeighbor.getSkyLight(x, y, DEPTH - 1);
                    if (neighborLight > 1) {
                        BlockType block = getBlock(x, y, 0);
                        if (block.isAir() || block.isTransparent()) {
                            int newLight = neighborLight - 1;
                            if (newLight > getSkyLight(x, y, 0)) {
                                setSkyLight(x, y, 0, newLight);
                                lightQueue.add(new int[] { x, y, 0, newLight });
                            }
                        }
                    }
                }
            }
        }

        // South border (z = DEPTH-1, check neighbor at z+1)
        if (southNeighbor != null) {
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    int neighborLight = southNeighbor.getSkyLight(x, y, 0);
                    if (neighborLight > 1) {
                        BlockType block = getBlock(x, y, DEPTH - 1);
                        if (block.isAir() || block.isTransparent()) {
                            int newLight = neighborLight - 1;
                            if (newLight > getSkyLight(x, y, DEPTH - 1)) {
                                setSkyLight(x, y, DEPTH - 1, newLight);
                                lightQueue.add(new int[] { x, y, DEPTH - 1, newLight });
                            }
                        }
                    }
                }
            }
        }

        // East border (x = WIDTH-1, check neighbor at x+1)
        if (eastNeighbor != null) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 0; y < HEIGHT; y++) {
                    int neighborLight = eastNeighbor.getSkyLight(0, y, z);
                    if (neighborLight > 1) {
                        BlockType block = getBlock(WIDTH - 1, y, z);
                        if (block.isAir() || block.isTransparent()) {
                            int newLight = neighborLight - 1;
                            if (newLight > getSkyLight(WIDTH - 1, y, z)) {
                                setSkyLight(WIDTH - 1, y, z, newLight);
                                lightQueue.add(new int[] { WIDTH - 1, y, z, newLight });
                            }
                        }
                    }
                }
            }
        }

        // West border (x = 0, check neighbor at x-1)
        if (westNeighbor != null) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 0; y < HEIGHT; y++) {
                    int neighborLight = westNeighbor.getSkyLight(WIDTH - 1, y, z);
                    if (neighborLight > 1) {
                        BlockType block = getBlock(0, y, z);
                        if (block.isAir() || block.isTransparent()) {
                            int newLight = neighborLight - 1;
                            if (newLight > getSkyLight(0, y, z)) {
                                setSkyLight(0, y, z, newLight);
                                lightQueue.add(new int[] { 0, y, z, newLight });
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Mark light as needing recalculation.
     */
    public void markLightDirty() {
        this.lightDirty = true;
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

    // ============ ASYNC MESH BUILDING SUPPORT ============

    /**
     * Get block at coordinates, looking up from neighbors if out of bounds.
     * Used by ChunkMeshBuilder for thread-safe cross-chunk lookups.
     */
    public BlockType getBlockWithNeighbors(int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) {
            return BlockType.AIR;
        }

        if (x < 0) {
            return westNeighbor != null ? westNeighbor.getBlock(WIDTH + x, y, z) : BlockType.AIR;
        } else if (x >= WIDTH) {
            return eastNeighbor != null ? eastNeighbor.getBlock(x - WIDTH, y, z) : BlockType.AIR;
        } else if (z < 0) {
            return northNeighbor != null ? northNeighbor.getBlock(x, y, DEPTH + z) : BlockType.AIR;
        } else if (z >= DEPTH) {
            return southNeighbor != null ? southNeighbor.getBlock(x, y, z - DEPTH) : BlockType.AIR;
        }

        return getBlock(x, y, z);
    }

    /**
     * Get sky light at coordinates, looking up from neighbors if out of bounds.
     * Used by ChunkMeshBuilder for thread-safe cross-chunk lookups.
     */
    public int getSkyLightWithNeighbors(int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) {
            return y >= HEIGHT ? 15 : 0;
        }

        if (x < 0) {
            return westNeighbor != null ? westNeighbor.getSkyLight(WIDTH + x, y, z) : 15;
        } else if (x >= WIDTH) {
            return eastNeighbor != null ? eastNeighbor.getSkyLight(x - WIDTH, y, z) : 15;
        } else if (z < 0) {
            return northNeighbor != null ? northNeighbor.getSkyLight(x, y, DEPTH + z) : 15;
        } else if (z >= DEPTH) {
            return southNeighbor != null ? southNeighbor.getSkyLight(x, y, z - DEPTH) : 15;
        }

        return getSkyLight(x, y, z);
    }

    /**
     * Apply pre-built mesh data to this chunk.
     * MUST be called on the main thread (OpenGL context).
     *
     * @param data The mesh data built by ChunkMeshBuilder
     */
    public void applyMeshData(ChunkMeshData data) {
        // Clean up old meshes
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        if (transparentMesh != null) {
            transparentMesh.cleanup();
            transparentMesh = null;
        }

        // Create opaque mesh
        if (data.hasOpaqueMesh()) {
            mesh = new com.craftzero.graphics.Mesh(
                    data.opaquePositions,
                    data.opaqueTexCoords,
                    data.opaqueNormals,
                    data.opaqueColors,
                    data.opaqueIndices);
        }

        // Create transparent mesh
        if (data.hasTransparentMesh()) {
            transparentMesh = new com.craftzero.graphics.Mesh(
                    data.transPositions,
                    data.transTexCoords,
                    data.transNormals,
                    data.transColors,
                    data.transIndices);
        }

        empty = data.empty;
        dirty = false;
    }
}
