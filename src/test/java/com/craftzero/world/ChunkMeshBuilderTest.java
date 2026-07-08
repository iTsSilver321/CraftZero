package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("Generated Release 1.0 dimension chunks should build valid render buffers")
    void generatedReleaseOneChunksBuildValidMeshes() {
        assertGeneratedMeshIsValid(Dimension.OVERWORLD, 424242L, 0, 0);
        assertGeneratedMeshIsValid(Dimension.NETHER, 515151L, 0, 0);
        assertGeneratedMeshIsValid(Dimension.THE_END, 8002L, 0, 0);
    }

    @Test
    @DisplayName("Generated Release 1.0 neighbor meshes should not add seam faces")
    void generatedReleaseOneNeighborMeshesDoNotAddSeamFaces() {
        assertGeneratedNeighborMeshDoesNotAddSeamFaces(Dimension.OVERWORLD, 424242L, 20, -13);
        assertGeneratedNeighborMeshDoesNotAddSeamFaces(Dimension.NETHER, 515151L, -16, -6);
        assertGeneratedNeighborMeshDoesNotAddSeamFaces(Dimension.THE_END, 8002L, 4, 0);
    }

    @Test
    @DisplayName("Structure-heavy Release 1.0 chunks should build valid render buffers")
    void structureHeavyReleaseOneChunksBuildValidMeshes() {
        assertGeneratedMeshIsValid(Dimension.OVERWORLD, 1L, -45, 38);
        assertGeneratedMeshIsValid(Dimension.NETHER, 1L, 7, -8);
        assertGeneratedMeshIsValid(Dimension.THE_END, 8002L, 4, 0);
    }

    @Test
    @DisplayName("Chunk mesh should cull shared solid faces across chunk borders")
    void chunkMeshCullsSolidFacesAcrossChunkBorders() {
        Chunk center = new Chunk(0, 0);
        Chunk east = new Chunk(1, 0);
        center.setNeighbors(null, null, east, null);
        east.setNeighbors(null, null, null, center);
        center.setBlock(15, 64, 8, BlockType.STONE);
        east.setBlock(0, 64, 8, BlockType.STONE);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(center);

        assertEquals(5, data.opaqueIndices.length / 6);
    }

    @Test
    @DisplayName("Chunk mesh should cull shared solid faces inside a chunk")
    void chunkMeshCullsSolidFacesInsideChunk() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.STONE);
        chunk.setBlock(2, 64, 1, BlockType.STONE);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(10, data.opaqueIndices.length / 6);
        assertEquals(0, data.cutoutIndices.length);
        assertEquals(0, data.transIndices.length);
    }

    @Test
    @DisplayName("Alpha-tested utility blocks should render on the cutout layer")
    void alphaTestedUtilityBlocksUseCutoutLayer() {
        assertCutout(BlockType.RAIL);
        assertCutout(BlockType.POWERED_RAIL);
        assertCutout(BlockType.DETECTOR_RAIL);
        assertCutout(BlockType.LADDER);
        assertCutout(BlockType.TORCH);
        assertCutout(BlockType.REDSTONE_TORCH_ON);
        assertCutout(BlockType.REDSTONE_WIRE);
        assertCutout(BlockType.REDSTONE_REPEATER_OFF);
        assertCutout(BlockType.WOODEN_DOOR);
        assertCutout(BlockType.TRAPDOOR);
        assertCutout(BlockType.IRON_BARS);
        assertCutout(BlockType.GLASS_PANE);
        assertCutout(BlockType.BREWING_STAND);
        assertCutout(BlockType.STANDING_SIGN);
        assertCutout(BlockType.WALL_SIGN);
    }

    @Test
    @DisplayName("Cutout utility block meshes should not be routed to the opaque buffer")
    void cutoutUtilityBlockMeshesAvoidOpaqueBuffer() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.RAIL);
        chunk.setBlock(3, 64, 1, BlockType.LADDER, 2);
        chunk.setBlock(5, 64, 1, BlockType.TORCH, 5);
        chunk.setBlock(7, 64, 1, BlockType.REDSTONE_WIRE);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(0, data.opaquePositions.length);
        assertTrue(data.cutoutPositions.length > 0);
        assertUvInsideAtlas(data.cutoutTexCoords);
    }

    @Test
    @DisplayName("Redstone wire should render a flat connected dust shape with power tint")
    void redstoneWireRendersFlatConnectedDustShapeWithPowerTint() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.REDSTONE_WIRE, 15);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(0, data.opaquePositions.length);
        assertEquals(0, data.transPositions.length);
        assertEquals(5 * 6, data.cutoutIndices.length);
        assertEquals(5 * 4 * 3, data.cutoutPositions.length);
        assertEquals(1.0f, minAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(2.0f, maxAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(1.0f, minAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(2.0f, maxAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(64.0f + 1.0f / 64.0f, minAxis(data.cutoutPositions, 1), 1.0E-6f);
        assertEquals(64.0f + 1.0f / 64.0f, maxAxis(data.cutoutPositions, 1), 1.0E-6f);
        float[] poweredColor = ChunkMeshBuilder.redstoneWireColor(15);
        assertEquals(poweredColor[0], maxColor(data.cutoutColors, 0), 1.0E-6f);
        assertEquals(poweredColor[1], maxColor(data.cutoutColors, 1), 1.0E-6f);
        assertEquals(poweredColor[2], maxColor(data.cutoutColors, 2), 1.0E-6f);
        assertUvInsideAtlas(data.cutoutTexCoords);
    }

    @Test
    @DisplayName("Redstone wire render connections should follow Release-style flat and stepped neighbors")
    void redstoneWireRenderConnectionsFollowReleaseStepRules() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.REDSTONE_WIRE, 15);
        chunk.setBlock(2, 64, 1, BlockType.REDSTONE_WIRE, 1);

        ChunkMeshBuilder.RedstoneWireConnections flat = ChunkMeshBuilder.redstoneWireConnections(chunk, 1, 64, 1);

        assertTrue(flat.east());
        assertFalse(flat.west());
        assertFalse(flat.north());
        assertFalse(flat.south());

        chunk.setBlock(2, 64, 1, BlockType.STONE);
        chunk.setBlock(2, 65, 1, BlockType.REDSTONE_WIRE, 1);

        ChunkMeshBuilder.RedstoneWireConnections steppedUp = ChunkMeshBuilder.redstoneWireConnections(chunk, 1, 64, 1);

        assertTrue(steppedUp.east());

        chunk.setBlock(1, 65, 1, BlockType.STONE);

        ChunkMeshBuilder.RedstoneWireConnections blockedUp = ChunkMeshBuilder.redstoneWireConnections(chunk, 1, 64, 1);

        assertFalse(blockedUp.east());

        chunk.setBlock(1, 65, 1, BlockType.AIR);
        chunk.setBlock(2, 64, 1, BlockType.AIR);
        chunk.setBlock(2, 65, 1, BlockType.AIR);
        chunk.setBlock(2, 63, 1, BlockType.REDSTONE_WIRE, 1);

        ChunkMeshBuilder.RedstoneWireConnections steppedDown = ChunkMeshBuilder.redstoneWireConnections(chunk, 1, 64,
                1);

        assertTrue(steppedDown.east());
    }

    @Test
    @DisplayName("Levers should render a small base plus a powered handle arm")
    void leversRenderBaseAndPoweredHandleArm() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(0, data.opaquePositions.length);
        assertEquals(0, data.transPositions.length);
        assertEquals(12 * 6, data.cutoutIndices.length);
        assertEquals(12 * 4 * 3, data.cutoutPositions.length);
        assertUvInsideAtlas(data.cutoutTexCoords);

        ChunkMeshBuilder.LeverHandleSegment unpowered = ChunkMeshBuilder.leverHandleSegment(5, 1, 64, 1);
        ChunkMeshBuilder.LeverHandleSegment powered = ChunkMeshBuilder.leverHandleSegment(
                5 | RedstoneEngine.POWERED_BIT, 1, 64, 1);
        assertEquals(1.5f, powered.start().x(), 1.0E-6f);
        assertEquals(64.125f, powered.start().y(), 1.0E-6f);
        assertEquals(1.5f, powered.start().z(), 1.0E-6f);
        assertEquals(64.75f, powered.end().y(), 1.0E-6f);
        assertTrue(unpowered.end().z() < unpowered.start().z());
        assertTrue(powered.end().z() > powered.start().z());
    }

    @Test
    @DisplayName("Standing signs should render source-sized board and post geometry")
    void standingSignsRenderSourceSizedBoardAndPost() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.STANDING_SIGN, 0);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(0, data.opaquePositions.length);
        assertEquals(0, data.transPositions.length);
        assertEquals(12 * 6, data.cutoutIndices.length);
        assertEquals(12 * 4 * 3, data.cutoutPositions.length);
        assertEquals(0.75f, minAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(2.25f, maxAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(1.4375f, minAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(1.5625f, maxAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(64.0f, minAxis(data.cutoutPositions, 1), 1.0E-6f);
        assertEquals(65.125f, maxAxis(data.cutoutPositions, 1), 1.0E-6f);
        assertUvInsideAtlas(data.cutoutTexCoords);
    }

    @Test
    @DisplayName("Standing sign mesh should honor all 16 metadata rotations")
    void standingSignMeshUsesSixteenWayRotation() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.STANDING_SIGN, 2);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertTrue(minAxis(data.cutoutPositions, 0) < 1.0f);
        assertTrue(maxAxis(data.cutoutPositions, 0) > 2.0f);
        assertTrue(minAxis(data.cutoutPositions, 2) < 1.0f);
        assertTrue(maxAxis(data.cutoutPositions, 2) > 2.0f);
        assertUvInsideAtlas(data.cutoutTexCoords);
    }

    @Test
    @DisplayName("Brewing stand bottle render bits should match occupied bottle slots")
    void brewingStandBottleRenderBitsMatchOccupiedSlots() {
        assertBrewingStandFaces(0, 12);
        assertBrewingStandFaces(1, 18);
        assertBrewingStandFaces(5, 24);
        assertBrewingStandFaces(7, 30);
    }

    @Test
    @DisplayName("Crossed plant sprites should use Release-style 0.45 half-width")
    void crossedPlantSpritesUseReleaseHalfWidth() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.YELLOW_FLOWER);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(4 * 4 * 3, data.cutoutPositions.length);
        assertEquals(1.05f, minAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(1.95f, maxAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(1.05f, minAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(1.95f, maxAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(64.0f, minAxis(data.cutoutPositions, 1), 1.0E-6f);
        assertEquals(65.0f, maxAxis(data.cutoutPositions, 1), 1.0E-6f);
    }

    @Test
    @DisplayName("Cobwebs should use the Release-style crossed sprite mesh")
    void cobwebsUseReleaseCrossedSpriteMesh() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.COBWEB);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(4 * 4 * 3, data.cutoutPositions.length);
        assertEquals(1.05f, minAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(1.95f, maxAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(1.05f, minAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(1.95f, maxAxis(data.cutoutPositions, 2), 1.0E-6f);
    }

    @Test
    @DisplayName("Tall grass crossed sprites should use Release-style position jitter")
    void tallGrassSpritesUseReleasePositionJitter() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(0, 64, 0, BlockType.TALL_GRASS, 1);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(4 * 4 * 3, data.cutoutPositions.length);
        assertEquals(-0.13333334f, minAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(0.76666665f, maxAxis(data.cutoutPositions, 0), 1.0E-6f);
        assertEquals(0.16666667f, minAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(1.0666667f, maxAxis(data.cutoutPositions, 2), 1.0E-6f);
        assertEquals(63.906666f, minAxis(data.cutoutPositions, 1), 1.0E-6f);
        assertEquals(64.90667f, maxAxis(data.cutoutPositions, 1), 1.0E-6f);
    }

    @Test
    @DisplayName("Full-height snow layer should still render its top face")
    void fullHeightSnowLayerKeepsTopFaceUnderSolidBlock() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.SNOW_LAYER, 7);
        chunk.setBlock(1, 65, 1, BlockType.STONE);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(6, data.cutoutIndices.length / 6);
    }

    @Test
    @DisplayName("Filled cauldrons should render Release-style inset water levels")
    void filledCauldronsRenderInsetWaterSurface() {
        Chunk empty = new Chunk(0, 0);
        empty.setBlock(1, 64, 1, BlockType.CAULDRON, 0);

        ChunkMeshData emptyData = ChunkMeshBuilder.buildMeshData(empty);

        assertEquals(0, emptyData.transIndices.length);

        Chunk filled = new Chunk(0, 0);
        filled.setBlock(1, 64, 1, BlockType.CAULDRON, 3);

        ChunkMeshData filledData = ChunkMeshBuilder.buildMeshData(filled);

        assertEquals(6, filledData.transIndices.length);
        assertEquals(4 * 3, filledData.transPositions.length);
        assertEquals(1.125f, minAxis(filledData.transPositions, 0), 1.0E-6f);
        assertEquals(1.875f, maxAxis(filledData.transPositions, 0), 1.0E-6f);
        assertEquals(1.125f, minAxis(filledData.transPositions, 2), 1.0E-6f);
        assertEquals(1.875f, maxAxis(filledData.transPositions, 2), 1.0E-6f);
        assertEquals(64.0f + ChunkMeshBuilder.cauldronWaterHeight(3), minAxis(filledData.transPositions, 1),
                1.0E-6f);
        assertEquals(64.0f + ChunkMeshBuilder.cauldronWaterHeight(3), maxAxis(filledData.transPositions, 1),
                1.0E-6f);
        assertUvInsideAtlas(filledData.transTexCoords);
    }

    private static Chunk adjacentLeavesChunk() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.LEAVES);
        chunk.setBlock(2, 64, 1, BlockType.LEAVES);
        return chunk;
    }

    private static void assertGeneratedMeshIsValid(Dimension dimension, long seed, int chunkX, int chunkZ) {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(seed, dimension);
        Chunk center = generatedChunk(generator, chunkX, chunkZ);
        Chunk north = generatedChunk(generator, chunkX, chunkZ - 1);
        Chunk south = generatedChunk(generator, chunkX, chunkZ + 1);
        Chunk east = generatedChunk(generator, chunkX + 1, chunkZ);
        Chunk west = generatedChunk(generator, chunkX - 1, chunkZ);
        center.setNeighbors(north, south, east, west);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(center);

        assertFalse(data.empty, dimension + " mesh should not be empty");
        assertLayerIsConsistent(data.opaquePositions, data.opaqueTexCoords, data.opaqueNormals,
                data.opaqueColors, data.opaqueIndices, chunkX, chunkZ, 0.0f, 0.0f);
        assertLayerIsConsistent(data.cutoutPositions, data.cutoutTexCoords, data.cutoutNormals,
                data.cutoutColors, data.cutoutIndices, chunkX, chunkZ, 0.25f, 0.2f);
        assertLayerIsConsistent(data.transPositions, data.transTexCoords, data.transNormals,
                data.transColors, data.transIndices, chunkX, chunkZ, 0.0f, 0.0f);
    }

    private static void assertGeneratedNeighborMeshDoesNotAddSeamFaces(Dimension dimension, long seed,
            int chunkX, int chunkZ) {
        ReleaseOneWorldGenerator openGenerator = new ReleaseOneWorldGenerator(seed, dimension);
        Chunk open = generatedChunk(openGenerator, chunkX, chunkZ);
        int openFaces = totalFaces(ChunkMeshBuilder.buildMeshData(open));

        ReleaseOneWorldGenerator joinedGenerator = new ReleaseOneWorldGenerator(seed, dimension);
        Chunk center = generatedChunk(joinedGenerator, chunkX, chunkZ);
        Chunk north = generatedChunk(joinedGenerator, chunkX, chunkZ - 1);
        Chunk south = generatedChunk(joinedGenerator, chunkX, chunkZ + 1);
        Chunk east = generatedChunk(joinedGenerator, chunkX + 1, chunkZ);
        Chunk west = generatedChunk(joinedGenerator, chunkX - 1, chunkZ);
        center.setNeighbors(north, south, east, west);

        ChunkMeshData joined = ChunkMeshBuilder.buildMeshData(center);

        assertTrue(totalFaces(joined) <= openFaces,
                dimension + " neighbor-aware mesh should not add chunk-border seam faces");
        assertLayerIsConsistent(joined.opaquePositions, joined.opaqueTexCoords, joined.opaqueNormals,
                joined.opaqueColors, joined.opaqueIndices, chunkX, chunkZ, 0.0f, 0.0f);
        assertLayerIsConsistent(joined.cutoutPositions, joined.cutoutTexCoords, joined.cutoutNormals,
                joined.cutoutColors, joined.cutoutIndices, chunkX, chunkZ, 0.25f, 0.2f);
        assertLayerIsConsistent(joined.transPositions, joined.transTexCoords, joined.transNormals,
                joined.transColors, joined.transIndices, chunkX, chunkZ, 0.0f, 0.0f);
    }

    private static Chunk generatedChunk(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        generator.generateChunk(null, chunk, chunkX, chunkZ);
        return chunk;
    }

    private static void assertBrewingStandFaces(int metadata, int expectedFaces) {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 64, 1, BlockType.BREWING_STAND, metadata);

        ChunkMeshData data = ChunkMeshBuilder.buildMeshData(chunk);

        assertEquals(0, data.opaquePositions.length);
        assertEquals(0, data.transPositions.length);
        assertEquals(expectedFaces, data.cutoutIndices.length / 6);
        assertEquals(expectedFaces * 4 * 3, data.cutoutPositions.length);
        assertUvInsideAtlas(data.cutoutTexCoords);
    }

    private static int totalFaces(ChunkMeshData data) {
        return (data.opaqueIndices.length + data.cutoutIndices.length + data.transIndices.length) / 6;
    }

    private static float minAxis(float[] positions, int axis) {
        float min = Float.POSITIVE_INFINITY;
        for (int i = axis; i < positions.length; i += 3) {
            min = Math.min(min, positions[i]);
        }
        return min;
    }

    private static float maxAxis(float[] positions, int axis) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = axis; i < positions.length; i += 3) {
            max = Math.max(max, positions[i]);
        }
        return max;
    }

    private static float maxColor(float[] colors, int channel) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = channel; i < colors.length; i += 3) {
            max = Math.max(max, colors[i]);
        }
        return max;
    }

    private static void assertLayerIsConsistent(float[] positions, float[] texCoords, float[] normals,
            float[] colors, int[] indices, int chunkX, int chunkZ, float horizontalMargin, float lowerYMargin) {
        assertEquals(0, positions.length % 12);
        int vertices = positions.length / 3;
        assertEquals(vertices * 2, texCoords.length);
        assertEquals(positions.length, normals.length);
        assertEquals(positions.length, colors.length);
        assertEquals(0, indices.length % 6);

        for (int index : indices) {
            assertTrue(index >= 0 && index < vertices);
        }
        assertFinite(positions);
        assertFinite(texCoords);
        assertFinite(normals);
        assertFinite(colors);
        assertUvInsideAtlas(texCoords);
        assertPositionsInsideChunk(positions, chunkX, chunkZ, horizontalMargin, lowerYMargin);
    }

    private static void assertCutout(BlockType type) {
        assertEquals(BlockRenderLayer.CUTOUT, type.getRenderLayer(), type + " should render as alpha-tested cutout");
    }

    private static void assertFinite(float[] values) {
        for (float value : values) {
            assertTrue(Float.isFinite(value));
        }
    }

    private static void assertUvInsideAtlas(float[] texCoords) {
        for (float coord : texCoords) {
            assertTrue(coord >= 0.0f && coord <= 1.0f, "UV coordinate outside the terrain atlas: " + coord);
        }
    }

    private static void assertPositionsInsideChunk(float[] positions, int chunkX, int chunkZ,
            float horizontalMargin, float lowerYMargin) {
        float minX = chunkX * Chunk.WIDTH - horizontalMargin;
        float maxX = minX + Chunk.WIDTH + horizontalMargin * 2.0f;
        float minZ = chunkZ * Chunk.DEPTH - horizontalMargin;
        float maxZ = minZ + Chunk.DEPTH + horizontalMargin * 2.0f;
        for (int i = 0; i < positions.length; i += 3) {
            assertTrue(positions[i] >= minX && positions[i] <= maxX);
            assertTrue(positions[i + 1] >= -lowerYMargin && positions[i + 1] <= Chunk.HEIGHT);
            assertTrue(positions[i + 2] >= minZ && positions[i + 2] <= maxZ);
        }
    }
}
