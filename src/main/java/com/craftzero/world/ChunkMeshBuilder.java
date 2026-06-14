package com.craftzero.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds ChunkMeshData from a Chunk on a background thread.
 * Thread-safe: only reads chunk data, no writes.
 * All OpenGL-sensitive operations are deferred to the main thread.
 */
public class ChunkMeshBuilder {
    private static volatile boolean smoothLightingEnabled = true;

    public static void setSmoothLightingEnabled(boolean enabled) {
        smoothLightingEnabled = enabled;
    }

    public static boolean isSmoothLightingEnabled() {
        return smoothLightingEnabled;
    }

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
        int[] opaqueVertexCount = { 0 };

        // Cutout buffers (alpha-tested blocks that still write depth)
        List<Float> cutoutPositions = new ArrayList<>();
        List<Float> cutoutTexCoords = new ArrayList<>();
        List<Float> cutoutNormals = new ArrayList<>();
        List<Float> cutoutColors = new ArrayList<>();
        List<Integer> cutoutIndices = new ArrayList<>();
        int[] cutoutVertexCount = { 0 };

        // Transparent buffers
        List<Float> transPositions = new ArrayList<>();
        List<Float> transTexCoords = new ArrayList<>();
        List<Float> transNormals = new ArrayList<>();
        List<Float> transColors = new ArrayList<>();
        List<Integer> transIndices = new ArrayList<>();
        int[] transVertexCount = { 0 };

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

                    if (type.isAir() || type == BlockType.CHEST) {
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
                    } else if (type.isWater()) {
                        blockColor = waterColor;
                    } else {
                        blockColor = new float[] { 1.0f, 1.0f, 1.0f }; // No tint
                    }

                    int metadata = chunk.getBlockMetadata(x, y, z);
                    BlockShape.BlockContext context = contextFor(chunk, x, y, z);
                    if (BlockShape.usesCrossedSprite(type)) {
                        BlockRenderLayer layer = type.getRenderLayer();
                        addCrossedSprite(type, metadata, worldX, y, worldZ, blockColor,
                                positionsFor(layer, opaquePositions, cutoutPositions, transPositions),
                                positionsFor(layer, opaqueTexCoords, cutoutTexCoords, transTexCoords),
                                positionsFor(layer, opaqueNormals, cutoutNormals, transNormals),
                                positionsFor(layer, opaqueColors, cutoutColors, transColors),
                                indicesFor(layer, opaqueIndices, cutoutIndices, transIndices),
                                vertexCountFor(layer, opaqueVertexCount, cutoutVertexCount, transVertexCount),
                                chunk, x, y, z);
                        continue;
                    }
                    List<BlockShape.Cuboid> boxes = BlockShape.getRenderBoxes(type, metadata, context);
                    for (BlockShape.Cuboid box : boxes) {
                        for (int face = 0; face < 6; face++) {
                            if (shouldRenderCuboidFace(chunk, x, y, z, face, type, box)) {
                                BlockRenderLayer layer = type.getRenderLayer();
                                addFace(type, metadata, face, worldX, y, worldZ, box, blockColor,
                                        positionsFor(layer, opaquePositions, cutoutPositions, transPositions),
                                        positionsFor(layer, opaqueTexCoords, cutoutTexCoords, transTexCoords),
                                        positionsFor(layer, opaqueNormals, cutoutNormals, transNormals),
                                        positionsFor(layer, opaqueColors, cutoutColors, transColors),
                                        indicesFor(layer, opaqueIndices, cutoutIndices, transIndices),
                                        vertexCountFor(layer, opaqueVertexCount, cutoutVertexCount, transVertexCount),
                                        chunk, x, y, z);
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
                toFloatArray(cutoutPositions),
                toFloatArray(cutoutTexCoords),
                toFloatArray(cutoutNormals),
                toFloatArray(cutoutColors),
                toIntArray(cutoutIndices),
                toFloatArray(transPositions),
                toFloatArray(transTexCoords),
                toFloatArray(transNormals),
                toFloatArray(transColors),
                toIntArray(transIndices));
    }

    private static void addCrossedSprite(BlockType type, int metadata, int worldX, int y, int worldZ,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        float inset = type == BlockType.FIRE ? 0.0f : 0.1464466f;
        float minX = worldX + inset;
        float maxX = worldX + 1.0f - inset;
        float minZ = worldZ + inset;
        float maxZ = worldZ + 1.0f - inset;
        float y0 = y;
        float y1 = y + (type == BlockType.DEAD_BUSH ? 0.8f : 1.0f);

        float[][] quads = {
                { minX, y1, minZ, minX, y0, minZ, maxX, y0, maxZ, maxX, y1, maxZ },
                { maxX, y1, maxZ, maxX, y0, maxZ, minX, y0, minZ, minX, y1, minZ },
                { minX, y1, maxZ, minX, y0, maxZ, maxX, y0, minZ, maxX, y1, minZ },
                { maxX, y1, minZ, maxX, y0, minZ, minX, y0, maxZ, minX, y1, maxZ }
        };

        float[] uv = Block.getFaceTexCoords(type, Block.FACE_NORTH, metadata);
        int light = chunk.getCombinedLightWithNeighbors(x, localY, z);
        float brightness = getLightBrightness(light);

        for (float[] quad : quads) {
            for (float v : quad) {
                positions.add(v);
            }
            for (float t : uv) {
                texCoords.add(t);
            }
            for (int i = 0; i < 4; i++) {
                normals.add(0.0f);
                normals.add(1.0f);
                normals.add(0.0f);
                colors.add(blockColor[0] * brightness);
                colors.add(blockColor[1] * brightness);
                colors.add(blockColor[2] * brightness);
            }
            int[] faceIndices = Block.getFaceIndices(vertexCount[0]);
            for (int idx : faceIndices) {
                indices.add(idx);
            }
            vertexCount[0] += 4;
        }
    }

    private static <T> List<T> positionsFor(BlockRenderLayer layer, List<T> opaque, List<T> cutout, List<T> trans) {
        return switch (layer) {
            case CUTOUT -> cutout;
            case TRANSLUCENT -> trans;
            default -> opaque;
        };
    }

    private static List<Integer> indicesFor(BlockRenderLayer layer, List<Integer> opaque, List<Integer> cutout,
            List<Integer> trans) {
        return positionsFor(layer, opaque, cutout, trans);
    }

    private static int[] vertexCountFor(BlockRenderLayer layer, int[] opaque, int[] cutout, int[] trans) {
        return switch (layer) {
            case CUTOUT -> cutout;
            case TRANSLUCENT -> trans;
            default -> opaque;
        };
    }

    private static BlockShape.BlockContext contextFor(Chunk chunk, int x, int y, int z) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return chunk.getBlockWithNeighbors(x + dx, y + dy, z + dz);
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return chunk.getBlockMetadataWithNeighbors(x + dx, y + dy, z + dz);
            }
        };
    }

    private static void addFace(BlockType type, int metadata, int face, int worldX, int y, int worldZ,
            BlockShape.Cuboid box, float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        float[] faceVerts = Block.getCuboidFaceVertices(face, worldX, y, worldZ, box);
        for (float v : faceVerts) {
            positions.add(v);
        }

        float[] faceTexCoords = Block.getFaceTexCoords(type, face, metadata);
        for (float t : faceTexCoords) {
            texCoords.add(t);
        }

        float[] faceNormals = Block.getFaceNormals(face);
        for (float n : faceNormals) {
            normals.add(n);
        }

        float faceShade = getFaceShade(face);
        int lx = x, ly = localY, lz = z;
        switch (face) {
            case Block.FACE_TOP -> ly++;
            case Block.FACE_BOTTOM -> ly--;
            case Block.FACE_NORTH -> lz--;
            case Block.FACE_SOUTH -> lz++;
            case Block.FACE_EAST -> lx++;
            case Block.FACE_WEST -> lx--;
        }

        float[] faceColor = blockColor;
        if (type == BlockType.GRASS && face != Block.FACE_TOP) {
            faceColor = new float[] { 1.0f, 1.0f, 1.0f };
        }

        for (int v = 0; v < 4; v++) {
            int vertexLight = getVertexLight(chunk, lx, ly, lz, face, v);
            float lightBrightness = getLightBrightness(vertexLight);
            float brightness = faceShade * lightBrightness;
            colors.add(faceColor[0] * brightness);
            colors.add(faceColor[1] * brightness);
            colors.add(faceColor[2] * brightness);
        }

        int[] faceIndices = Block.getFaceIndices(vertexCount[0]);
        for (int idx : faceIndices) {
            indices.add(idx);
        }
        vertexCount[0] += 4;
    }

    private static boolean shouldRenderCuboidFace(Chunk chunk, int x, int y, int z, int face,
            BlockType currentBlock, BlockShape.Cuboid box) {
        boolean boundary = switch (face) {
            case Block.FACE_TOP -> box.maxY() >= 1.0f;
            case Block.FACE_BOTTOM -> box.minY() <= 0.0f;
            case Block.FACE_NORTH -> box.minZ() <= 0.0f;
            case Block.FACE_SOUTH -> box.maxZ() >= 1.0f;
            case Block.FACE_EAST -> box.maxX() >= 1.0f;
            case Block.FACE_WEST -> box.minX() <= 0.0f;
            default -> false;
        };
        return !boundary || shouldRenderFace(chunk, x, y, z, face, currentBlock);
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

        // Fancy leaves are cutout blocks: neighboring leaf faces must remain so
        // leaf clusters keep the speckled, see-through Release 1.0 look.
        if (currentBlock == BlockType.LEAVES && neighbor == BlockType.LEAVES && currentBlock.isTransparent()) {
            return true;
        }

        // Never render face between identical opaque blocks.
        if (currentBlock == neighbor) {
            return false;
        }
        if ((currentBlock.isWater() && neighbor.isWater()) || (currentBlock.isLava() && neighbor.isLava())) {
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
    private static float getLightBrightness(int lightLevel) {
        float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
        float gamma = f / (3.0f - 2.0f * f);
        return Math.max(0.08f, gamma);
    }

    // Get smooth vertex light
    private static int getVertexLight(Chunk chunk, int lx, int ly, int lz, int face, int vertexIndex) {
        if (!smoothLightingEnabled) {
            return chunk.getCombinedLightWithNeighbors(lx, ly, lz);
        }
        int[][] offsets = getVertexLightOffsets(face, vertexIndex);

        int totalLight = 0;
        int count = 0;

        for (int[] offset : offsets) {
            int sx = lx + offset[0];
            int sy = ly + offset[1];
            int sz = lz + offset[2];

            BlockType block = chunk.getBlockWithNeighbors(sx, sy, sz);
            if (block.isAir() || block.isTransparent() || !block.occludesFace()) {
                totalLight += chunk.getCombinedLightWithNeighbors(sx, sy, sz);
                count++;
            }
        }

        if (count == 0) {
            return chunk.getCombinedLightWithNeighbors(lx, ly, lz);
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
