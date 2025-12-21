package com.craftzero.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds ChunkMeshData from a Chunk on a background thread.
 * Thread-safe: only reads chunk data, no writes.
 * All OpenGL-sensitive operations are deferred to the main thread.
 */
public class ChunkMeshBuilder {

    /**
     * Build mesh data for a chunk. This method is thread-safe.
     * 
     * @param chunk The chunk to build mesh data for
     * @return ChunkMeshData containing all mesh arrays
     */
    public static ChunkMeshData buildMeshData(Chunk chunk) {
        // First calculate sky lighting
        chunk.calculateSkyLight();

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

        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    BlockType type = chunk.getBlock(x, y, z);

                    if (type.isAir()) {
                        continue;
                    }

                    // World position for vertex generation
                    int worldX = chunkX * Chunk.WIDTH + x;
                    int worldZ = chunkZ * Chunk.DEPTH + z;

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
                        if (shouldRenderFace(chunk, x, y, z, face, type)) {
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
                                boolean waterAbove = (y < Chunk.HEIGHT - 1) &&
                                        (chunk.getBlock(x, y + 1, z) == BlockType.WATER);
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
                            float faceShade = getFaceShade(face);

                            // Get light sampling position
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
                                int vertexLight = getVertexLight(chunk, lx, ly, lz, face, v);
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

        return new ChunkMeshData(
                toFloatArray(opaquePositions),
                toFloatArray(opaqueTexCoords),
                toFloatArray(opaqueNormals),
                toFloatArray(opaqueColors),
                toIntArray(opaqueIndices),
                toFloatArray(transPositions),
                toFloatArray(transTexCoords),
                toFloatArray(transNormals),
                toFloatArray(transColors),
                toIntArray(transIndices));
    }

    // Helper to check if face should be rendered
    private static boolean shouldRenderFace(Chunk chunk, int x, int y, int z, int face, BlockType currentBlock) {
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
        if (ny < 0 || ny >= Chunk.HEIGHT) {
            return face == Block.FACE_TOP;
        }

        // Get neighbor from chunk (handles cross-chunk lookups internally)
        neighbor = chunk.getBlockWithNeighbors(nx, ny, nz);

        // Never render face between identical blocks
        if (currentBlock == neighbor) {
            return false;
        }

        if (neighbor.isAir()) {
            return true;
        }
        if (neighbor.isTransparent() && !currentBlock.isTransparent()) {
            return true;
        }
        if (currentBlock.isTransparent()) {
            return !neighbor.occludesFace();
        }

        return false;
    }

    // Get face shading multiplier
    private static float getFaceShade(int face) {
        return switch (face) {
            case Block.FACE_TOP -> 1.0f;
            case Block.FACE_BOTTOM -> 0.5f;
            case Block.FACE_NORTH, Block.FACE_SOUTH -> 0.8f;
            case Block.FACE_EAST, Block.FACE_WEST -> 0.6f;
            default -> 1.0f;
        };
    }

    // Convert sky light level to brightness
    private static float getSkyLightBrightness(int lightLevel) {
        float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
        float gamma = f / (3.0f - 2.0f * f);
        return Math.max(0.08f, gamma);
    }

    // Get smooth vertex light
    private static int getVertexLight(Chunk chunk, int lx, int ly, int lz, int face, int vertexIndex) {
        int[][] offsets = getVertexLightOffsets(face, vertexIndex);

        int totalLight = 0;
        int count = 0;

        for (int[] offset : offsets) {
            int sx = lx + offset[0];
            int sy = ly + offset[1];
            int sz = lz + offset[2];

            BlockType block = chunk.getBlockWithNeighbors(sx, sy, sz);
            if (block.isAir() || block.isTransparent()) {
                totalLight += chunk.getSkyLightWithNeighbors(sx, sy, sz);
                count++;
            }
        }

        if (count == 0) {
            return chunk.getSkyLightWithNeighbors(lx, ly, lz);
        }

        return totalLight / count;
    }

    // Get vertex light sampling offsets
    private static int[][] getVertexLightOffsets(int face, int vertexIndex) {
        switch (face) {
            case Block.FACE_TOP:
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, -1 }, { -1, 0, -1 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 1, 0, -1 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 1 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_BOTTOM:
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 1 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 1, 0, -1 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 0, -1 }, { -1, 0, -1 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_NORTH:
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 1, -1, 0 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { -1, -1, 0 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { -1, 1, 0 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_SOUTH:
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { -1, -1, 0 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 1, -1, 0 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { -1, 1, 0 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_EAST:
                return switch (vertexIndex) {
                    case 0 -> new int[][] { { 0, 0, 0 }, { 0, 0, -1 }, { 0, -1, 0 }, { 0, -1, -1 } };
                    case 1 -> new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, -1, 0 }, { 0, -1, 1 } };
                    case 2 -> new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 1 } };
                    case 3 -> new int[][] { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 1, 0 }, { 0, 1, -1 } };
                    default -> new int[][] { { 0, 0, 0 } };
                };
            case Block.FACE_WEST:
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

    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
