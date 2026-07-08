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
    private static final float CAULDRON_WATER_INSET = 2.0f / 16.0f;
    private static final float STANDING_SIGN_POST_HALF_WIDTH = 1.0f / 16.0f;
    private static final float STANDING_SIGN_POST_HEIGHT = 14.0f / 16.0f;
    private static final float STANDING_SIGN_BOARD_HALF_WIDTH = 12.0f / 16.0f;
    private static final float STANDING_SIGN_BOARD_HALF_HEIGHT = 6.0f / 16.0f;
    private static final float STANDING_SIGN_BOARD_HALF_THICKNESS = 1.0f / 16.0f;
    private static final float STANDING_SIGN_BOARD_CENTER_Y = 12.0f / 16.0f;

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

                    if (type.isAir() || type == BlockType.CHEST || type == BlockType.MOVING_PISTON) {
                        continue;
                    }

                    // World position for vertex generation
                    int worldX = chunkX * Chunk.WIDTH + x;
                    int worldZ = chunkZ * Chunk.DEPTH + z;

                    // Determine vertex color based on block type
                    float[] blockColor;
                    if (type == BlockType.GRASS) {
                        blockColor = grassColor;
                    } else if (type == BlockType.LEAVES || type == BlockType.TALL_GRASS) {
                        blockColor = foliageColor;
                    } else if (type.isWater()) {
                        blockColor = waterColor;
                    } else {
                        blockColor = new float[] { 1.0f, 1.0f, 1.0f }; // No tint
                    }

                    int metadata = chunk.getBlockMetadata(x, y, z);
                    BlockShape.BlockContext context = contextFor(chunk, x, y, z);
                    if (type.isFluid()) {
                        BlockRenderLayer layer = type.getRenderLayer();
                        addFluidBlock(type, metadata, worldX, y, worldZ, blockColor,
                                positionsFor(layer, opaquePositions, cutoutPositions, transPositions),
                                positionsFor(layer, opaqueTexCoords, cutoutTexCoords, transTexCoords),
                                positionsFor(layer, opaqueNormals, cutoutNormals, transNormals),
                                positionsFor(layer, opaqueColors, cutoutColors, transColors),
                                indicesFor(layer, opaqueIndices, cutoutIndices, transIndices),
                                vertexCountFor(layer, opaqueVertexCount, cutoutVertexCount, transVertexCount),
                                chunk, x, y, z);
                        continue;
                    }
                    if (type == BlockType.REDSTONE_WIRE) {
                        addRedstoneWireBlock(metadata, worldX, y, worldZ, redstoneWireColor(metadata),
                                cutoutPositions, cutoutTexCoords, cutoutNormals, cutoutColors, cutoutIndices,
                                cutoutVertexCount, chunk, x, y, z);
                        continue;
                    }
                    if (type == BlockType.LEVER) {
                        addLeverBlock(metadata, worldX, y, worldZ, blockColor,
                                cutoutPositions, cutoutTexCoords, cutoutNormals, cutoutColors, cutoutIndices,
                                cutoutVertexCount, chunk, x, y, z);
                        continue;
                    }
                    if (type == BlockType.STANDING_SIGN) {
                        addStandingSignBlock(metadata, worldX, y, worldZ, blockColor,
                                cutoutPositions, cutoutTexCoords, cutoutNormals, cutoutColors, cutoutIndices,
                                cutoutVertexCount, chunk, x, y, z);
                        continue;
                    }
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
                    VoxelShape shape = BlockShape.renderShape(new BlockState(type, metadata), context);
                    for (BlockShape.Cuboid box : shape.boxes()) {
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
                    if (type == BlockType.CAULDRON) {
                        addCauldronWaterSurface(metadata, worldX, y, worldZ, waterColor,
                                transPositions, transTexCoords, transNormals, transColors, transIndices,
                                transVertexCount, chunk, x, y, z);
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

    private static void addRedstoneWireBlock(int metadata, int worldX, int y, int worldZ,
            float[] wireColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        RedstoneWireConnections connections = redstoneWireConnections(chunk, x, localY, z);
        boolean north = connections.north();
        boolean south = connections.south();
        boolean east = connections.east();
        boolean west = connections.west();
        if (!north && !south && !east && !west) {
            north = true;
            south = true;
            east = true;
            west = true;
        }

        float min = 5.0f / 16.0f;
        float max = 11.0f / 16.0f;
        float wireY = y + 1.0f / 64.0f;
        addRedstoneWireQuad(metadata, worldX + min, worldX + max, wireY, worldZ + min, worldZ + max,
                wireColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        if (north) {
            addRedstoneWireQuad(metadata, worldX + min, worldX + max, wireY, worldZ, worldZ + min,
                    wireColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
        if (south) {
            addRedstoneWireQuad(metadata, worldX + min, worldX + max, wireY, worldZ + max, worldZ + 1.0f,
                    wireColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
        if (west) {
            addRedstoneWireQuad(metadata, worldX, worldX + min, wireY, worldZ + min, worldZ + max,
                    wireColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
        if (east) {
            addRedstoneWireQuad(metadata, worldX + max, worldX + 1.0f, wireY, worldZ + min, worldZ + max,
                    wireColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
    }

    private static void addRedstoneWireQuad(int metadata, float minX, float maxX, float y, float minZ, float maxZ,
            float[] wireColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        addCustomFace(BlockType.REDSTONE_WIRE, metadata, Block.FACE_TOP, new float[] {
                minX, y, minZ,
                minX, y, maxZ,
                maxX, y, maxZ,
                maxX, y, minZ
        }, wireColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
    }

    static float[] redstoneWireColor(int metadata) {
        int power = Math.max(0, Math.min(15, metadata & 15));
        float strength = power / 15.0f;
        float red = strength * 0.6f + 0.4f;
        if (power == 0) {
            red = 0.3f;
        }
        float green = Math.max(0.0f, strength * strength * 0.7f - 0.5f);
        float blue = Math.max(0.0f, strength * strength * 0.6f - 0.7f);
        return new float[] { red, green, blue };
    }

    static RedstoneWireConnections redstoneWireConnections(Chunk chunk, int x, int y, int z) {
        return new RedstoneWireConnections(
                redstoneWireConnectsTo(chunk, x, y, z, Block.FACE_NORTH),
                redstoneWireConnectsTo(chunk, x, y, z, Block.FACE_SOUTH),
                redstoneWireConnectsTo(chunk, x, y, z, Block.FACE_EAST),
                redstoneWireConnectsTo(chunk, x, y, z, Block.FACE_WEST));
    }

    private static boolean redstoneWireConnectsTo(Chunk chunk, int x, int y, int z, int face) {
        int nx = x + RedstoneEngine.faceToDx(face);
        int nz = z + RedstoneEngine.faceToDz(face);
        int towardWire = RedstoneEngine.opposite(face);

        if (redstoneConnectorAt(chunk, nx, y, nz, towardWire)) {
            return true;
        }
        BlockType neighbor = chunk.getBlockWithNeighbors(nx, y, nz);
        if (!canRedstoneClimbOn(neighbor) && redstoneConnectorAt(chunk, nx, y - 1, nz, towardWire)) {
            return true;
        }
        return !BlockShape.isOpaqueCube(chunk.getBlockWithNeighbors(x, y + 1, z))
                && canRedstoneClimbOn(neighbor)
                && redstoneConnectorAt(chunk, nx, y + 1, nz, towardWire);
    }

    private static boolean canRedstoneClimbOn(BlockType type) {
        return BlockShape.isOpaqueCube(type) || type == BlockType.GLOWSTONE;
    }

    private static boolean redstoneConnectorAt(Chunk chunk, int x, int y, int z, int faceTowardWire) {
        BlockType type = chunk.getBlockWithNeighbors(x, y, z);
        int metadata = chunk.getBlockMetadataWithNeighbors(x, y, z);
        if (type == BlockType.REDSTONE_WIRE) {
            return true;
        }
        if (type == BlockType.REDSTONE_REPEATER_OFF || type == BlockType.REDSTONE_REPEATER_ON) {
            return faceTowardWire == RedstoneEngine.repeaterInputFace(metadata)
                    || faceTowardWire == RedstoneEngine.repeaterOutputFace(metadata);
        }
        return type == BlockType.REDSTONE_TORCH_OFF
                || type == BlockType.REDSTONE_TORCH_ON
                || type == BlockType.LEVER
                || type == BlockType.STONE_BUTTON
                || type == BlockType.STONE_PRESSURE_PLATE
                || type == BlockType.WOODEN_PRESSURE_PLATE
                || type == BlockType.DETECTOR_RAIL;
    }

    record RedstoneWireConnections(boolean north, boolean south, boolean east, boolean west) {
    }

    private static void addStandingSignBlock(int metadata, int worldX, int y, int worldZ,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        addRotatedSignBox(metadata, worldX, y, worldZ,
                -STANDING_SIGN_POST_HALF_WIDTH, 0.0f, -STANDING_SIGN_POST_HALF_WIDTH,
                STANDING_SIGN_POST_HALF_WIDTH, STANDING_SIGN_POST_HEIGHT, STANDING_SIGN_POST_HALF_WIDTH,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);

        addRotatedSignBox(metadata, worldX, y, worldZ,
                -STANDING_SIGN_BOARD_HALF_WIDTH, STANDING_SIGN_BOARD_CENTER_Y - STANDING_SIGN_BOARD_HALF_HEIGHT,
                -STANDING_SIGN_BOARD_HALF_THICKNESS,
                STANDING_SIGN_BOARD_HALF_WIDTH, STANDING_SIGN_BOARD_CENTER_Y + STANDING_SIGN_BOARD_HALF_HEIGHT,
                STANDING_SIGN_BOARD_HALF_THICKNESS,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
    }

    private static void addRotatedSignBox(int metadata, int worldX, int y, int worldZ,
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        Vec3f nnn = standingSignPoint(metadata, worldX, y, worldZ, minX, minY, minZ);
        Vec3f pnn = standingSignPoint(metadata, worldX, y, worldZ, maxX, minY, minZ);
        Vec3f ppn = standingSignPoint(metadata, worldX, y, worldZ, maxX, maxY, minZ);
        Vec3f npn = standingSignPoint(metadata, worldX, y, worldZ, minX, maxY, minZ);
        Vec3f nnp = standingSignPoint(metadata, worldX, y, worldZ, minX, minY, maxZ);
        Vec3f pnp = standingSignPoint(metadata, worldX, y, worldZ, maxX, minY, maxZ);
        Vec3f ppp = standingSignPoint(metadata, worldX, y, worldZ, maxX, maxY, maxZ);
        Vec3f npp = standingSignPoint(metadata, worldX, y, worldZ, minX, maxY, maxZ);

        addCustomQuad(BlockType.STANDING_SIGN, metadata, Block.FACE_NORTH, pnn, nnn, npn, ppn,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(BlockType.STANDING_SIGN, metadata, Block.FACE_SOUTH, nnp, pnp, ppp, npp,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(BlockType.STANDING_SIGN, metadata, Block.FACE_WEST, nnn, nnp, npp, npn,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(BlockType.STANDING_SIGN, metadata, Block.FACE_EAST, pnp, pnn, ppn, ppp,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(BlockType.STANDING_SIGN, metadata, Block.FACE_BOTTOM, nnn, pnn, pnp, nnp,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(BlockType.STANDING_SIGN, metadata, Block.FACE_TOP, npp, ppp, ppn, npn,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
    }

    static Vec3f standingSignPoint(int metadata, int worldX, int y, int worldZ,
            float localX, float localY, float localZ) {
        float angle = (metadata & 15) * (float) (Math.PI * 2.0 / 16.0);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float rotatedX = localX * cos - localZ * sin;
        float rotatedZ = localX * sin + localZ * cos;
        return new Vec3f(worldX + 0.5f + rotatedX, y + localY, worldZ + 0.5f + rotatedZ);
    }

    private static void addLeverBlock(int metadata, int worldX, int y, int worldZ,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        for (BlockShape.Cuboid base : leverBaseBoxes(metadata)) {
            for (int face = 0; face < 6; face++) {
                addFace(BlockType.COBBLESTONE, 0, face, worldX, y, worldZ, base, blockColor,
                        positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
            }
        }

        LeverHandleSegment segment = leverHandleSegment(metadata, worldX, y, worldZ);
        addTexturedPrism(BlockType.LEVER, metadata, segment.start(), segment.end(), 1.0f / 16.0f,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
    }

    private static List<BlockShape.Cuboid> leverBaseBoxes(int metadata) {
        int orientation = metadata & 7;
        return switch (orientation) {
            case 1 -> List.of(new BlockShape.Cuboid(0, 5.0f / 16.0f, 5.0f / 16.0f,
                    2.0f / 16.0f, 11.0f / 16.0f, 11.0f / 16.0f));
            case 2 -> List.of(new BlockShape.Cuboid(14.0f / 16.0f, 5.0f / 16.0f, 5.0f / 16.0f,
                    1, 11.0f / 16.0f, 11.0f / 16.0f));
            case 3 -> List.of(new BlockShape.Cuboid(5.0f / 16.0f, 5.0f / 16.0f, 0,
                    11.0f / 16.0f, 11.0f / 16.0f, 2.0f / 16.0f));
            case 4 -> List.of(new BlockShape.Cuboid(5.0f / 16.0f, 5.0f / 16.0f, 14.0f / 16.0f,
                    11.0f / 16.0f, 11.0f / 16.0f, 1));
            case 0, 7 -> List.of(new BlockShape.Cuboid(5.0f / 16.0f, 14.0f / 16.0f, 5.0f / 16.0f,
                    11.0f / 16.0f, 1, 11.0f / 16.0f));
            default -> List.of(new BlockShape.Cuboid(5.0f / 16.0f, 0, 5.0f / 16.0f,
                    11.0f / 16.0f, 2.0f / 16.0f, 11.0f / 16.0f));
        };
    }

    static LeverHandleSegment leverHandleSegment(int metadata, int worldX, int y, int worldZ) {
        int orientation = metadata & 7;
        boolean powered = (metadata & RedstoneEngine.POWERED_BIT) != 0;
        float centerX = worldX + 0.5f;
        float centerY = y + 0.5f;
        float centerZ = worldZ + 0.5f;
        float vertical = powered ? 5.0f / 16.0f : -5.0f / 16.0f;
        float wallStart = 2.0f / 16.0f;
        float wallEnd = 8.0f / 16.0f;

        return switch (orientation) {
            case 1 -> new LeverHandleSegment(
                    new Vec3f(worldX + wallStart, centerY, centerZ),
                    new Vec3f(worldX + wallEnd, centerY + vertical, centerZ));
            case 2 -> new LeverHandleSegment(
                    new Vec3f(worldX + 1.0f - wallStart, centerY, centerZ),
                    new Vec3f(worldX + 1.0f - wallEnd, centerY + vertical, centerZ));
            case 3 -> new LeverHandleSegment(
                    new Vec3f(centerX, centerY, worldZ + wallStart),
                    new Vec3f(centerX, centerY + vertical, worldZ + wallEnd));
            case 4 -> new LeverHandleSegment(
                    new Vec3f(centerX, centerY, worldZ + 1.0f - wallStart),
                    new Vec3f(centerX, centerY + vertical, worldZ + 1.0f - wallEnd));
            case 6 -> floorLeverHandle(worldX, y, worldZ, powered, true);
            case 0 -> ceilingLeverHandle(worldX, y, worldZ, powered, false);
            case 7 -> ceilingLeverHandle(worldX, y, worldZ, powered, true);
            default -> floorLeverHandle(worldX, y, worldZ, powered, false);
        };
    }

    private static LeverHandleSegment floorLeverHandle(int worldX, int y, int worldZ,
            boolean powered, boolean xAxis) {
        float horizontal = powered ? 4.0f / 16.0f : -4.0f / 16.0f;
        float centerX = worldX + 0.5f;
        float centerZ = worldZ + 0.5f;
        Vec3f start = new Vec3f(centerX, y + 2.0f / 16.0f, centerZ);
        Vec3f end = xAxis
                ? new Vec3f(centerX + horizontal, y + 12.0f / 16.0f, centerZ)
                : new Vec3f(centerX, y + 12.0f / 16.0f, centerZ + horizontal);
        return new LeverHandleSegment(start, end);
    }

    private static LeverHandleSegment ceilingLeverHandle(int worldX, int y, int worldZ,
            boolean powered, boolean xAxis) {
        float horizontal = powered ? 4.0f / 16.0f : -4.0f / 16.0f;
        float centerX = worldX + 0.5f;
        float centerZ = worldZ + 0.5f;
        Vec3f start = new Vec3f(centerX, y + 14.0f / 16.0f, centerZ);
        Vec3f end = xAxis
                ? new Vec3f(centerX + horizontal, y + 4.0f / 16.0f, centerZ)
                : new Vec3f(centerX, y + 4.0f / 16.0f, centerZ + horizontal);
        return new LeverHandleSegment(start, end);
    }

    private static void addTexturedPrism(BlockType type, int metadata, Vec3f start, Vec3f end, float radius,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        Vec3f direction = end.subtract(start).normalize();
        if (direction.lengthSquared() == 0.0f) {
            return;
        }
        Vec3f reference = Math.abs(direction.y()) > 0.9f ? new Vec3f(1.0f, 0.0f, 0.0f)
                : new Vec3f(0.0f, 1.0f, 0.0f);
        Vec3f u = direction.cross(reference).normalize().scale(radius);
        Vec3f v = u.cross(direction).normalize().scale(radius);

        Vec3f s0 = start.add(u).add(v);
        Vec3f s1 = start.subtract(u).add(v);
        Vec3f s2 = start.subtract(u).subtract(v);
        Vec3f s3 = start.add(u).subtract(v);
        Vec3f e0 = end.add(u).add(v);
        Vec3f e1 = end.subtract(u).add(v);
        Vec3f e2 = end.subtract(u).subtract(v);
        Vec3f e3 = end.add(u).subtract(v);

        addCustomQuad(type, metadata, Block.FACE_NORTH, s0, s1, s2, s3,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(type, metadata, Block.FACE_NORTH, e3, e2, e1, e0,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(type, metadata, Block.FACE_NORTH, s0, e0, e1, s1,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(type, metadata, Block.FACE_NORTH, s1, e1, e2, s2,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(type, metadata, Block.FACE_NORTH, s2, e2, e3, s3,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        addCustomQuad(type, metadata, Block.FACE_NORTH, s3, e3, e0, s0,
                blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
    }

    private static void addCustomQuad(BlockType type, int metadata, int textureFace,
            Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        Vec3f normal = p1.subtract(p0).cross(p2.subtract(p0)).normalize();
        addPoint(positions, p0);
        addPoint(positions, p1);
        addPoint(positions, p2);
        addPoint(positions, p3);

        float[] faceTexCoords = Block.getFaceTexCoords(type, textureFace, metadata);
        for (float t : faceTexCoords) {
            texCoords.add(t);
        }

        for (int i = 0; i < 4; i++) {
            normals.add(normal.x());
            normals.add(normal.y());
            normals.add(normal.z());
        }

        float brightness = normalShade(normal) * getLightBrightness(chunk.getCombinedLightWithNeighbors(x, localY, z));
        for (int i = 0; i < 4; i++) {
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

    private static void addPoint(List<Float> positions, Vec3f point) {
        positions.add(point.x());
        positions.add(point.y());
        positions.add(point.z());
    }

    private static float normalShade(Vec3f normal) {
        float ax = Math.abs(normal.x());
        float ay = Math.abs(normal.y());
        float az = Math.abs(normal.z());
        if (ay >= ax && ay >= az) {
            return normal.y() >= 0.0f ? 1.0f : 0.5f;
        }
        return ax >= az ? 0.6f : 0.8f;
    }

    record LeverHandleSegment(Vec3f start, Vec3f end) {
    }

    record Vec3f(float x, float y, float z) {
        Vec3f add(Vec3f other) {
            return new Vec3f(x + other.x, y + other.y, z + other.z);
        }

        Vec3f subtract(Vec3f other) {
            return new Vec3f(x - other.x, y - other.y, z - other.z);
        }

        Vec3f scale(float amount) {
            return new Vec3f(x * amount, y * amount, z * amount);
        }

        Vec3f cross(Vec3f other) {
            return new Vec3f(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x);
        }

        float lengthSquared() {
            return x * x + y * y + z * z;
        }

        Vec3f normalize() {
            float length = (float) Math.sqrt(lengthSquared());
            return length <= 1.0E-6f ? new Vec3f(0.0f, 0.0f, 0.0f) : scale(1.0f / length);
        }
    }

    private static void addCrossedSprite(BlockType type, int metadata, int worldX, int y, int worldZ,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        double renderX = worldX;
        double renderY = y;
        double renderZ = worldZ;
        if (type == BlockType.TALL_GRASS) {
            long seed = (long) (worldX * 3129871) ^ (long) worldZ * 116129781L ^ (long) y;
            seed = seed * seed * 42317861L + seed * 11L;
            renderX += (((float) (seed >> 16 & 15L) / 15.0F) - 0.5D) * 0.5D;
            renderY += (((float) (seed >> 20 & 15L) / 15.0F) - 1.0D) * 0.2D;
            renderZ += (((float) (seed >> 24 & 15L) / 15.0F) - 0.5D) * 0.5D;
        }

        float inset = type == BlockType.FIRE ? 0.0f : 0.05f;
        float minX = (float) (renderX + inset);
        float maxX = (float) (renderX + 1.0D - inset);
        float minZ = (float) (renderZ + inset);
        float maxZ = (float) (renderZ + 1.0D - inset);
        float y0 = (float) renderY;
        float y1 = (float) (renderY + (type == BlockType.DEAD_BUSH ? 0.8D : 1.0D));

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

    private static void addFluidBlock(BlockType type, int metadata, int worldX, int y, int worldZ,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        boolean sameAbove = isSameFluid(chunk.getBlockWithNeighbors(x, localY + 1, z), type);
        float nw = sameAbove ? 1.0f : cornerFluidHeight(chunk, x, localY, z, type, -1, -1);
        float sw = sameAbove ? 1.0f : cornerFluidHeight(chunk, x, localY, z, type, -1, 1);
        float se = sameAbove ? 1.0f : cornerFluidHeight(chunk, x, localY, z, type, 1, 1);
        float ne = sameAbove ? 1.0f : cornerFluidHeight(chunk, x, localY, z, type, 1, -1);

        float x0 = worldX;
        float x1 = worldX + 1.0f;
        float y0 = y;
        float z0 = worldZ;
        float z1 = worldZ + 1.0f;

        if (!sameAbove) {
            addCustomFace(type, metadata, Block.FACE_TOP, new float[] {
                    x0, y0 + nw, z0,
                    x0, y0 + sw, z1,
                    x1, y0 + se, z1,
                    x1, y0 + ne, z0
            }, blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }

        if (!isSameFluid(chunk.getBlockWithNeighbors(x, localY - 1, z), type)
                && shouldRenderFace(chunk, x, localY, z, Block.FACE_BOTTOM, type)) {
            addCustomFace(type, metadata, Block.FACE_BOTTOM, new float[] {
                    x0, y0, z1,
                    x0, y0, z0,
                    x1, y0, z0,
                    x1, y0, z1
            }, blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }

        if (shouldRenderFluidSide(chunk, x, localY, z, Block.FACE_NORTH, type)) {
            addCustomFace(type, metadata, Block.FACE_NORTH, new float[] {
                    x1, y0 + ne, z0,
                    x1, y0, z0,
                    x0, y0, z0,
                    x0, y0 + nw, z0
            }, blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
        if (shouldRenderFluidSide(chunk, x, localY, z, Block.FACE_SOUTH, type)) {
            addCustomFace(type, metadata, Block.FACE_SOUTH, new float[] {
                    x0, y0 + sw, z1,
                    x0, y0, z1,
                    x1, y0, z1,
                    x1, y0 + se, z1
            }, blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
        if (shouldRenderFluidSide(chunk, x, localY, z, Block.FACE_EAST, type)) {
            addCustomFace(type, metadata, Block.FACE_EAST, new float[] {
                    x1, y0 + se, z1,
                    x1, y0, z1,
                    x1, y0, z0,
                    x1, y0 + ne, z0
            }, blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
        if (shouldRenderFluidSide(chunk, x, localY, z, Block.FACE_WEST, type)) {
            addCustomFace(type, metadata, Block.FACE_WEST, new float[] {
                    x0, y0 + nw, z0,
                    x0, y0, z0,
                    x0, y0, z1,
                    x0, y0 + sw, z1
            }, blockColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
        }
    }

    private static void addCauldronWaterSurface(int metadata, int worldX, int y, int worldZ,
            float[] waterColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
        int level = cauldronWaterLevel(metadata);
        if (level <= 0) {
            return;
        }

        float minX = worldX + CAULDRON_WATER_INSET;
        float maxX = worldX + 1.0f - CAULDRON_WATER_INSET;
        float minZ = worldZ + CAULDRON_WATER_INSET;
        float maxZ = worldZ + 1.0f - CAULDRON_WATER_INSET;
        float surfaceY = y + cauldronWaterHeight(level);

        addCustomFace(BlockType.WATER, 0, Block.FACE_TOP, new float[] {
                minX, surfaceY, minZ,
                minX, surfaceY, maxZ,
                maxX, surfaceY, maxZ,
                maxX, surfaceY, minZ
        }, waterColor, positions, texCoords, normals, colors, indices, vertexCount, chunk, x, localY, z);
    }

    static int cauldronWaterLevel(int metadata) {
        return Math.max(0, Math.min(World.CAULDRON_MAX_LEVEL, metadata));
    }

    static float cauldronWaterHeight(int level) {
        return (6.0f + cauldronWaterLevel(level) * 3.0f) / 16.0f;
    }

    private static boolean shouldRenderFluidSide(Chunk chunk, int x, int y, int z, int face, BlockType fluid) {
        int nx = x;
        int nz = z;
        switch (face) {
            case Block.FACE_NORTH -> nz--;
            case Block.FACE_SOUTH -> nz++;
            case Block.FACE_EAST -> nx++;
            case Block.FACE_WEST -> nx--;
            default -> {
            }
        }

        BlockType neighbor = chunk.getBlockWithNeighbors(nx, y, nz);
        if (isSameFluid(neighbor, fluid)) {
            return false;
        }
        if (neighbor.isAir()) {
            return true;
        }
        return neighbor.isTransparent() && !neighbor.occludesFace();
    }

    private static float cornerFluidHeight(Chunk chunk, int x, int y, int z, BlockType fluid, int dx, int dz) {
        float total = 0.0f;
        int count = 0;
        float center = fluidHeightAt(chunk, x, y, z, fluid);
        if (center > 0.0f) {
            total += center;
            count++;
        }
        float sideX = fluidHeightAt(chunk, x + dx, y, z, fluid);
        if (sideX > 0.0f) {
            total += sideX;
            count++;
        }
        float sideZ = fluidHeightAt(chunk, x, y, z + dz, fluid);
        if (sideZ > 0.0f) {
            total += sideZ;
            count++;
        }
        float diagonal = fluidHeightAt(chunk, x + dx, y, z + dz, fluid);
        if (diagonal > 0.0f) {
            total += diagonal;
            count++;
        }
        return count == 0 ? FluidState.height(0) : total / count;
    }

    private static float fluidHeightAt(Chunk chunk, int x, int y, int z, BlockType fluid) {
        BlockType type = chunk.getBlockWithNeighbors(x, y, z);
        if (!isSameFluid(type, fluid)) {
            return 0.0f;
        }
        if (isSameFluid(chunk.getBlockWithNeighbors(x, y + 1, z), fluid)) {
            return 1.0f;
        }
        return FluidState.height(chunk.getBlockMetadataWithNeighbors(x, y, z));
    }

    private static boolean isSameFluid(BlockType a, BlockType b) {
        return (a.isWater() && b.isWater()) || (a.isLava() && b.isLava());
    }

    private static void addCustomFace(BlockType type, int metadata, int face, float[] faceVerts,
            float[] blockColor,
            List<Float> positions, List<Float> texCoords, List<Float> normals, List<Float> colors,
            List<Integer> indices, int[] vertexCount, Chunk chunk, int x, int localY, int z) {
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

        int lx = x;
        int ly = localY;
        int lz = z;
        switch (face) {
            case Block.FACE_TOP -> ly++;
            case Block.FACE_BOTTOM -> ly--;
            case Block.FACE_NORTH -> lz--;
            case Block.FACE_SOUTH -> lz++;
            case Block.FACE_EAST -> lx++;
            case Block.FACE_WEST -> lx--;
            default -> {
            }
        }

        float faceShade = getFaceShade(face);
        for (int v = 0; v < 4; v++) {
            int vertexLight = getVertexLight(chunk, lx, ly, lz, face, v);
            float brightness = faceShade * getLightBrightness(vertexLight);
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
        if (currentBlock == BlockType.SNOW_LAYER && face == Block.FACE_TOP) {
            return true;
        }
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
            if (!block.blocksAmbientOcclusion()) {
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
