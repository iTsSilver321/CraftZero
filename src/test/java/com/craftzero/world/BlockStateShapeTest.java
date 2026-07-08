package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockStateShapeTest {
    @Test
    @DisplayName("BlockState should preserve type and normalize metadata")
    void blockStateNormalizesMetadata() {
        BlockState state = new BlockState(BlockType.OAK_STAIRS, 19);

        assertSame(BlockType.OAK_STAIRS, state.type());
        assertEquals(3, state.metadata());
    }

    @Test
    @DisplayName("VoxelShape should distinguish full, partial, and empty block shapes")
    void voxelShapeClassifiesFullPartialAndEmptyShapes() {
        BlockShape.BlockContext empty = emptyContext();

        assertTrue(BlockShape.renderShape(BlockState.of(BlockType.STONE), empty).isFullCube());
        assertFalse(BlockShape.renderShape(BlockState.of(BlockType.STONE_SLAB), empty).isFullCube());
        assertTrue(BlockShape.collisionShape(BlockState.of(BlockType.YELLOW_FLOWER), empty).isEmpty());
        assertFalse(BlockShape.selectionShape(BlockState.of(BlockType.YELLOW_FLOWER), empty).isEmpty());
    }

    @Test
    @DisplayName("Tall grass should use Release-style soft plant shape semantics")
    void tallGrassUsesSoftPlantShapeSemantics() {
        VoxelShape collision = BlockShape.collisionShape(BlockState.of(BlockType.TALL_GRASS), emptyContext());
        VoxelShape selection = BlockShape.selectionShape(BlockState.of(BlockType.TALL_GRASS), emptyContext());

        assertTrue(collision.isEmpty());
        assertFalse(selection.isEmpty());
        assertFalse(selection.isFullCube());
        assertEquals(0.1f, selection.boxes().get(0).minX(), 0.0001f);
        assertEquals(0.8f, selection.boxes().get(0).maxY(), 0.0001f);
        assertTrue(BlockShape.canPlaceAt(BlockType.TALL_GRASS, 1, contextWithBelow(BlockType.GRASS)));
        assertTrue(BlockShape.canPlaceAt(BlockType.TALL_GRASS, 2, contextWithBelow(BlockType.DIRT)));
        assertTrue(BlockShape.canPlaceAt(BlockType.TALL_GRASS, 1, contextWithBelow(BlockType.FARMLAND)));
        assertFalse(BlockShape.canPlaceAt(BlockType.TALL_GRASS, 1, contextWithBelow(BlockType.SAND)));
        assertTrue(BlockShape.canFallThrough(BlockType.TALL_GRASS));
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.TALL_GRASS, Block.FACE_TOP));
        assertTrue(BlockShape.isReplaceable(BlockType.TALL_GRASS));
        assertTrue(TreeFeature.isReplaceableForTrunk(BlockType.TALL_GRASS));
        assertTrue(TreeFeature.isReplaceableForLeaves(BlockType.TALL_GRASS));
    }

    @Test
    @DisplayName("Ground-cover plants should use their separate source selection bounds")
    void groundCoverPlantsUseSourceSelectionBounds() {
        assertCuboid(BlockShape.selectionShape(BlockState.of(BlockType.YELLOW_FLOWER), emptyContext()).boxes().get(0),
                0.3f, 0.0f, 0.3f,
                0.7f, 0.6f, 0.7f);
        assertCuboid(BlockShape.selectionShape(BlockState.of(BlockType.RED_ROSE), emptyContext()).boxes().get(0),
                0.3f, 0.0f, 0.3f,
                0.7f, 0.6f, 0.7f);
        assertCuboid(BlockShape.selectionShape(BlockState.of(BlockType.BROWN_MUSHROOM), emptyContext()).boxes().get(0),
                0.3f, 0.0f, 0.3f,
                0.7f, 0.4f, 0.7f);
        assertCuboid(BlockShape.selectionShape(BlockState.of(BlockType.RED_MUSHROOM), emptyContext()).boxes().get(0),
                0.3f, 0.0f, 0.3f,
                0.7f, 0.4f, 0.7f);
        assertCuboid(BlockShape.selectionShape(BlockState.of(BlockType.SAPLING), emptyContext()).boxes().get(0),
                0.1f, 0.0f, 0.1f,
                0.9f, 0.8f, 0.9f);
        assertCuboid(BlockShape.selectionShape(BlockState.of(BlockType.DEAD_BUSH), emptyContext()).boxes().get(0),
                0.1f, 0.0f, 0.1f,
                0.9f, 0.8f, 0.9f);
    }

    @Test
    @DisplayName("Release 1.0 ground-cover plants should accept farmland and mycelium supports")
    void groundCoverPlantsAcceptReleaseOneSpecialSoils() {
        assertTrue(BlockShape.canPlaceAt(BlockType.YELLOW_FLOWER, 0, contextWithBelow(BlockType.FARMLAND)));
        assertTrue(BlockShape.canPlaceAt(BlockType.RED_ROSE, 0, contextWithBelow(BlockType.FARMLAND)));
        assertTrue(BlockShape.canPlaceAt(BlockType.SAPLING, 0, contextWithBelow(BlockType.FARMLAND)));
        assertTrue(BlockShape.canPlaceAt(BlockType.BROWN_MUSHROOM, 0, contextWithBelow(BlockType.GRAVEL)));
        assertTrue(BlockShape.canPlaceAt(BlockType.BROWN_MUSHROOM, 0, contextWithBelow(BlockType.MYCELIUM)));
        assertTrue(BlockShape.canPlaceAt(BlockType.RED_MUSHROOM, 0, contextWithBelow(BlockType.MYCELIUM)));
    }

    @Test
    @DisplayName("Chest should keep a smaller partial shape for selection and collision")
    void chestUsesInsetPartialShape() {
        VoxelShape shape = BlockShape.collisionShape(BlockState.of(BlockType.CHEST), emptyContext());

        assertFalse(shape.isEmpty());
        assertFalse(shape.isFullCube());
        assertEquals(1, shape.boxes().size());
        BlockShape.Cuboid box = shape.boxes().get(0);
        assertEquals(1.0f / 16.0f, box.minX(), 0.0001f);
        assertEquals(14.0f / 16.0f, box.maxY(), 0.0001f);
        assertEquals(15.0f / 16.0f, box.maxZ(), 0.0001f);
    }

    @Test
    @DisplayName("Cactus should keep source collision narrow while ray selection stays full")
    void cactusSelectionAndCollisionUseSourceBounds() {
        VoxelShape render = BlockShape.renderShape(BlockState.of(BlockType.CACTUS), emptyContext());
        VoxelShape collision = BlockShape.collisionShape(BlockState.of(BlockType.CACTUS), emptyContext());
        VoxelShape selection = BlockShape.selectionShape(BlockState.of(BlockType.CACTUS), emptyContext());

        assertFalse(render.isFullCube());
        assertFalse(collision.isFullCube());
        assertTrue(selection.isFullCube());
        assertCuboid(collision.boxes().get(0),
                1.0f / 16.0f, 0.0f, 1.0f / 16.0f,
                15.0f / 16.0f, 15.0f / 16.0f, 15.0f / 16.0f);
    }

    @Test
    @DisplayName("Stairs should expose multiple partial cuboids from one metadata-backed state")
    void stairsExposeCompositePartialShape() {
        VoxelShape shape = BlockShape.renderShape(new BlockState(BlockType.OAK_STAIRS, 0), emptyContext());

        assertFalse(shape.isFullCube());
        assertEquals(2, shape.boxes().size());
    }

    @Test
    @DisplayName("Trapdoor metadata should preserve bottom, top, and open hinge shapes")
    void trapdoorMetadataControlsShape() {
        BlockShape.Cuboid bottom = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 0), emptyContext())
                .boxes().get(0);
        BlockShape.Cuboid top = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 8), emptyContext())
                .boxes().get(0);
        BlockShape.Cuboid openHingedSouth = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 4), emptyContext())
                .boxes().get(0);
        BlockShape.Cuboid openHingedWest = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 5), emptyContext())
                .boxes().get(0);
        BlockShape.Cuboid openHingedNorth = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 6), emptyContext())
                .boxes().get(0);
        BlockShape.Cuboid openHingedEast = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 7), emptyContext())
                .boxes().get(0);

        assertEquals(0.0f, bottom.minY(), 0.0001f);
        assertEquals(3.0f / 16.0f, bottom.maxY(), 0.0001f);
        assertEquals(13.0f / 16.0f, top.minY(), 0.0001f);
        assertEquals(1.0f, top.maxY(), 0.0001f);
        assertEquals(13.0f / 16.0f, openHingedSouth.minZ(), 0.0001f);
        assertEquals(1.0f, openHingedSouth.maxZ(), 0.0001f);
        assertEquals(0.0f, openHingedWest.minX(), 0.0001f);
        assertEquals(3.0f / 16.0f, openHingedWest.maxX(), 0.0001f);
        assertEquals(0.0f, openHingedNorth.minZ(), 0.0001f);
        assertEquals(3.0f / 16.0f, openHingedNorth.maxZ(), 0.0001f);
        assertEquals(13.0f / 16.0f, openHingedEast.minX(), 0.0001f);
        assertEquals(1.0f, openHingedEast.maxX(), 0.0001f);
    }

    @Test
    @DisplayName("Door shapes should use source facing and upper-half hinge metadata")
    void doorMetadataControlsSourceShape() {
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 0), doorContext(0, 8))
                .boxes().get(0), 0.0f, 0.0f, 0.0f, 3.0f / 16.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 1), doorContext(1, 8))
                .boxes().get(0), 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 3.0f / 16.0f);
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 2), doorContext(2, 8))
                .boxes().get(0), 13.0f / 16.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 3), doorContext(3, 8))
                .boxes().get(0), 0.0f, 0.0f, 13.0f / 16.0f, 1.0f, 1.0f, 1.0f);

        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 4), doorContext(4, 8))
                .boxes().get(0), 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 3.0f / 16.0f);
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 4), doorContext(4, 9))
                .boxes().get(0), 0.0f, 0.0f, 13.0f / 16.0f, 1.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 8), doorContext(6, 8))
                .boxes().get(0), 0.0f, 0.0f, 13.0f / 16.0f, 1.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.renderShape(new BlockState(BlockType.WOODEN_DOOR, 9), doorContext(6, 9))
                .boxes().get(0), 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 3.0f / 16.0f);
    }

    @Test
    @DisplayName("Open fence gates should render swung leaves while staying non-solid")
    void openFenceGatePreservesRenderAxis() {
        VoxelShape render = BlockShape.renderShape(
                new BlockState(BlockType.FENCE_GATE, 1 | RedstoneEngine.DOOR_OPEN_BIT), emptyContext());
        VoxelShape collision = BlockShape.collisionShape(
                new BlockState(BlockType.FENCE_GATE, 1 | RedstoneEngine.DOOR_OPEN_BIT), emptyContext());

        assertTrue(collision.isEmpty());
        assertEquals(4, render.boxes().size());
        BlockShape.Cuboid firstPost = render.boxes().get(0);
        BlockShape.Cuboid secondPost = render.boxes().get(1);
        assertEquals(6.0f / 16.0f, firstPost.minX(), 0.0001f);
        assertEquals(10.0f / 16.0f, firstPost.maxX(), 0.0001f);
        assertEquals(0.0f, firstPost.minZ(), 0.0001f);
        assertEquals(2.0f / 16.0f, firstPost.maxZ(), 0.0001f);
        assertEquals(14.0f / 16.0f, secondPost.minZ(), 0.0001f);
        assertEquals(1.0f, secondPost.maxZ(), 0.0001f);
        assertCuboid(render.boxes().get(2),
                6.0f / 16.0f, 6.0f / 16.0f, 0.0f,
                1.0f, 18.0f / 16.0f, 2.0f / 16.0f);
        assertCuboid(render.boxes().get(3),
                6.0f / 16.0f, 6.0f / 16.0f, 14.0f / 16.0f,
                1.0f, 18.0f / 16.0f, 1.0f);

        VoxelShape oppositeEastWestRender = BlockShape.renderShape(
                new BlockState(BlockType.FENCE_GATE, 3 | RedstoneEngine.DOOR_OPEN_BIT), emptyContext());
        assertCuboid(oppositeEastWestRender.boxes().get(2),
                0.0f, 6.0f / 16.0f, 0.0f,
                10.0f / 16.0f, 18.0f / 16.0f, 2.0f / 16.0f);

        VoxelShape northSouthRender = BlockShape.renderShape(
                new BlockState(BlockType.FENCE_GATE, RedstoneEngine.DOOR_OPEN_BIT), emptyContext());
        assertCuboid(northSouthRender.boxes().get(2),
                0.0f, 6.0f / 16.0f, 6.0f / 16.0f,
                2.0f / 16.0f, 18.0f / 16.0f, 1.0f);

        VoxelShape oppositeNorthSouthRender = BlockShape.renderShape(
                new BlockState(BlockType.FENCE_GATE, 2 | RedstoneEngine.DOOR_OPEN_BIT), emptyContext());
        assertCuboid(oppositeNorthSouthRender.boxes().get(2),
                0.0f, 6.0f / 16.0f, 0.0f,
                2.0f / 16.0f, 18.0f / 16.0f, 10.0f / 16.0f);
    }

    @Test
    @DisplayName("Closed fence gates should use the source 1.5-block collision strip")
    void closedFenceGateUsesSourceCollisionStrip() {
        VoxelShape northSouthRender = BlockShape.renderShape(new BlockState(BlockType.FENCE_GATE, 0), emptyContext());
        VoxelShape northSouthCollision = BlockShape.collisionShape(
                new BlockState(BlockType.FENCE_GATE, 0), emptyContext());
        VoxelShape eastWestCollision = BlockShape.collisionShape(
                new BlockState(BlockType.FENCE_GATE, 1), emptyContext());

        assertEquals(3, northSouthRender.boxes().size());
        assertEquals(1, northSouthCollision.boxes().size());
        assertCuboid(northSouthCollision.boxes().get(0),
                0.0f, 0.0f, 6.0f / 16.0f,
                1.0f, 1.5f, 10.0f / 16.0f);
        assertEquals(1, eastWestCollision.boxes().size());
        assertCuboid(eastWestCollision.boxes().get(0),
                6.0f / 16.0f, 0.0f, 0.0f,
                10.0f / 16.0f, 1.5f, 1.0f);
    }

    @Test
    @DisplayName("Torches should use Release 1.0 metadata selection bounds")
    void torchMetadataControlsSourceSelectionShape() {
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.TORCH, 1), emptyContext()).boxes().get(0),
                0.0f, 0.2f, 0.35f,
                0.3f, 0.8f, 0.65f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.TORCH, 2), emptyContext()).boxes().get(0),
                0.7f, 0.2f, 0.35f,
                1.0f, 0.8f, 0.65f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.TORCH, 3), emptyContext()).boxes().get(0),
                0.35f, 0.2f, 0.0f,
                0.65f, 0.8f, 0.3f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.TORCH, 4), emptyContext()).boxes().get(0),
                0.35f, 0.2f, 0.7f,
                0.65f, 0.8f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.TORCH, 5), emptyContext()).boxes().get(0),
                0.4f, 0.0f, 0.4f,
                0.6f, 0.6f, 0.6f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.REDSTONE_TORCH_ON, 5), emptyContext())
                        .boxes().get(0),
                0.4f, 0.0f, 0.4f,
                0.6f, 0.6f, 0.6f);
    }

    @Test
    @DisplayName("Ladders and wall signs should use Release 1.0 metadata selection bounds")
    void ladderAndWallSignMetadataControlsSourceSelectionShape() {
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LADDER, 2), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.875f,
                1.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LADDER, 3), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.125f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LADDER, 4), emptyContext()).boxes().get(0),
                0.875f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LADDER, 5), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                0.125f, 1.0f, 1.0f);

        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.WALL_SIGN, 2), emptyContext()).boxes().get(0),
                0.0f, 0.28125f, 0.875f,
                1.0f, 0.78125f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.WALL_SIGN, 3), emptyContext()).boxes().get(0),
                0.0f, 0.28125f, 0.0f,
                1.0f, 0.78125f, 0.125f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.WALL_SIGN, 4), emptyContext()).boxes().get(0),
                0.875f, 0.28125f, 0.0f,
                1.0f, 0.78125f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.WALL_SIGN, 5), emptyContext()).boxes().get(0),
                0.0f, 0.28125f, 0.0f,
                0.125f, 0.78125f, 1.0f);
    }

    @Test
    @DisplayName("Standing signs should use Release 1.0 source selection bounds")
    void standingSignUsesSourceSelectionBounds() {
        VoxelShape collision = BlockShape.collisionShape(new BlockState(BlockType.STANDING_SIGN, 4), emptyContext());
        VoxelShape render = BlockShape.renderShape(new BlockState(BlockType.STANDING_SIGN, 4), emptyContext());
        VoxelShape selection = BlockShape.selectionShape(new BlockState(BlockType.STANDING_SIGN, 4), emptyContext());

        assertTrue(collision.isEmpty());
        assertEquals(2, render.boxes().size());
        assertEquals(1, selection.boxes().size());
        assertCuboid(selection.boxes().get(0),
                0.25f, 0.0f, 0.25f,
                0.75f, 1.0f, 0.75f);
    }

    @Test
    @DisplayName("Vines should use Release 1.0 bitmask selection bounds")
    void vineMetadataControlsSourceBitmaskSelectionShape() {
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.VINES,
                        BlockShape.vineMetadataFromFace(Block.FACE_NORTH)), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 15.0f / 16.0f,
                1.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.VINES,
                        BlockShape.vineMetadataFromFace(Block.FACE_EAST)), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f / 16.0f, 1.0f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.VINES,
                        BlockShape.vineMetadataFromFace(Block.FACE_SOUTH)), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.VINES,
                        BlockShape.vineMetadataFromFace(Block.FACE_WEST)), emptyContext()).boxes().get(0),
                15.0f / 16.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f);

        int twoSided = BlockShape.vineMetadataFromFace(Block.FACE_EAST)
                | BlockShape.vineMetadataFromFace(Block.FACE_WEST);
        assertTrue(BlockShape.selectionShape(new BlockState(BlockType.VINES, twoSided), emptyContext()).isFullCube());

        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.VINES, 0),
                        contextWithBlockAt(0, 1, 0, BlockType.STONE)).boxes().get(0),
                0.0f, 15.0f / 16.0f, 0.0f,
                1.0f, 1.0f, 1.0f);
    }

    @Test
    @DisplayName("Crops, stems, and nether wart should use source selection bounds")
    void cropAndNetherWartSelectionBoundsMatchSource() {
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.CROPS, 0), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f, 0.25f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.CROPS, 7), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f, 0.25f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.NETHER_WART, 0), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f, 0.25f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.NETHER_WART, 3), emptyContext()).boxes().get(0),
                0.0f, 0.0f, 0.0f,
                1.0f, 0.25f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.PUMPKIN_STEM, 0), emptyContext()).boxes().get(0),
                0.375f, 0.0f, 0.375f,
                0.625f, 0.125f, 0.625f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.MELON_STEM, 7), emptyContext()).boxes().get(0),
                0.375f, 0.0f, 0.375f,
                0.625f, 1.0f, 0.625f);
    }

    @Test
    @DisplayName("Levers should use Release 1.0 metadata selection bounds")
    void leverMetadataControlsSourceSelectionShape() {
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 1), emptyContext()).boxes().get(0),
                0.0f, 0.2f, 5.0f / 16.0f,
                6.0f / 16.0f, 0.8f, 11.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 2), emptyContext()).boxes().get(0),
                10.0f / 16.0f, 0.2f, 5.0f / 16.0f,
                1.0f, 0.8f, 11.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 3), emptyContext()).boxes().get(0),
                5.0f / 16.0f, 0.2f, 0.0f,
                11.0f / 16.0f, 0.8f, 6.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 4), emptyContext()).boxes().get(0),
                5.0f / 16.0f, 0.2f, 10.0f / 16.0f,
                11.0f / 16.0f, 0.8f, 1.0f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 5), emptyContext()).boxes().get(0),
                0.25f, 0.0f, 0.25f,
                0.75f, 0.6f, 0.75f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 6), emptyContext()).boxes().get(0),
                0.25f, 0.0f, 0.25f,
                0.75f, 0.6f, 0.75f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 0), emptyContext()).boxes().get(0),
                0.25f, 0.4f, 0.25f,
                0.75f, 1.0f, 0.75f);
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.LEVER, 7), emptyContext()).boxes().get(0),
                0.25f, 0.4f, 0.25f,
                0.75f, 1.0f, 0.75f);
    }

    @Test
    @DisplayName("Powered stone buttons should use the depressed Release 1.0 shape")
    void poweredStoneButtonsUseDepressedSelectionShape() {
        assertCuboid(BlockShape.selectionShape(new BlockState(BlockType.STONE_BUTTON, 1), emptyContext()).boxes().get(0),
                0.0f, 6.0f / 16.0f, 5.0f / 16.0f,
                2.0f / 16.0f, 10.0f / 16.0f, 11.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(
                        new BlockState(BlockType.STONE_BUTTON, 1 | RedstoneEngine.POWERED_BIT), emptyContext())
                .boxes().get(0),
                0.0f, 6.0f / 16.0f, 5.0f / 16.0f,
                1.0f / 16.0f, 10.0f / 16.0f, 11.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(
                        new BlockState(BlockType.STONE_BUTTON, 2 | RedstoneEngine.POWERED_BIT), emptyContext())
                .boxes().get(0),
                15.0f / 16.0f, 6.0f / 16.0f, 5.0f / 16.0f,
                1.0f, 10.0f / 16.0f, 11.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(
                        new BlockState(BlockType.STONE_BUTTON, 3 | RedstoneEngine.POWERED_BIT), emptyContext())
                .boxes().get(0),
                5.0f / 16.0f, 6.0f / 16.0f, 0.0f,
                11.0f / 16.0f, 10.0f / 16.0f, 1.0f / 16.0f);
        assertCuboid(BlockShape.selectionShape(
                        new BlockState(BlockType.STONE_BUTTON, 4 | RedstoneEngine.POWERED_BIT), emptyContext())
                .boxes().get(0),
                5.0f / 16.0f, 6.0f / 16.0f, 15.0f / 16.0f,
                11.0f / 16.0f, 10.0f / 16.0f, 1.0f);
    }

    @Test
    @DisplayName("Piston metadata should expose Release 1.0 base and head shapes")
    void pistonMetadataControlsBaseAndHeadShapes() {
        VoxelShape retractedBase = BlockShape.collisionShape(
                new BlockState(BlockType.PISTON, Block.FACE_EAST), emptyContext());
        assertTrue(retractedBase.isFullCube());

        VoxelShape extendedBase = BlockShape.collisionShape(
                new BlockState(BlockType.PISTON, Block.FACE_EAST | RedstoneEngine.PISTON_EXTENDED_BIT),
                emptyContext());
        assertFalse(extendedBase.isFullCube());
        assertEquals(1, extendedBase.boxes().size());
        BlockShape.Cuboid shortened = extendedBase.boxes().get(0);
        assertEquals(0.0f, shortened.minX(), 0.0001f);
        assertEquals(12.0f / 16.0f, shortened.maxX(), 0.0001f);
        assertEquals(1.0f, shortened.maxY(), 0.0001f);

        VoxelShape eastHead = BlockShape.collisionShape(
                new BlockState(BlockType.PISTON_HEAD, Block.FACE_EAST), emptyContext());
        assertFalse(eastHead.isFullCube());
        assertEquals(2, eastHead.boxes().size());
        BlockShape.Cuboid eastPlate = eastHead.boxes().get(0);
        BlockShape.Cuboid eastArm = eastHead.boxes().get(1);
        assertEquals(12.0f / 16.0f, eastPlate.minX(), 0.0001f);
        assertEquals(1.0f, eastPlate.maxX(), 0.0001f);
        assertEquals(0.0f, eastArm.minX(), 0.0001f);
        assertEquals(12.0f / 16.0f, eastArm.maxX(), 0.0001f);
        assertEquals(4.0f / 16.0f, eastArm.minZ(), 0.0001f);
        assertEquals(12.0f / 16.0f, eastArm.maxZ(), 0.0001f);

        BlockShape.Cuboid upPlate = BlockShape.collisionShape(
                new BlockState(BlockType.PISTON_HEAD, Block.FACE_TOP), emptyContext()).boxes().get(0);
        assertEquals(12.0f / 16.0f, upPlate.minY(), 0.0001f);
        assertEquals(1.0f, upPlate.maxY(), 0.0001f);

        BlockShape.Cuboid downPlate = BlockShape.collisionShape(
                new BlockState(BlockType.PISTON_HEAD, Block.FACE_BOTTOM), emptyContext()).boxes().get(0);
        assertEquals(0.0f, downPlate.minY(), 0.0001f);
        assertEquals(4.0f / 16.0f, downPlate.maxY(), 0.0001f);

        VoxelShape movingHead = BlockShape.collisionShape(
                new BlockState(BlockType.MOVING_PISTON, Block.FACE_EAST), emptyContext());
        assertFalse(movingHead.isFullCube());
        assertEquals(2, movingHead.boxes().size());
    }

    @Test
    @DisplayName("Piston metadata should select Release 1.0 base and extension textures")
    void pistonMetadataControlsBaseAndHeadTextures() {
        assertEquals(107, textureIndex(BlockType.PISTON, Block.FACE_EAST, Block.FACE_EAST));
        assertEquals(110, textureIndex(BlockType.PISTON, Block.FACE_EAST,
                Block.FACE_EAST | RedstoneEngine.PISTON_EXTENDED_BIT));
        assertEquals(109, textureIndex(BlockType.PISTON, Block.FACE_WEST, Block.FACE_EAST));
        assertEquals(108, textureIndex(BlockType.PISTON, Block.FACE_TOP, Block.FACE_EAST));

        assertEquals(106, textureIndex(BlockType.STICKY_PISTON, Block.FACE_EAST, Block.FACE_EAST));
        assertEquals(110, textureIndex(BlockType.STICKY_PISTON, Block.FACE_EAST,
                Block.FACE_EAST | RedstoneEngine.PISTON_EXTENDED_BIT));

        assertEquals(107, textureIndex(BlockType.PISTON_HEAD, Block.FACE_EAST, Block.FACE_EAST));
        assertEquals(106, textureIndex(BlockType.PISTON_HEAD, Block.FACE_EAST,
                Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT));
        assertEquals(107, textureIndex(BlockType.PISTON_HEAD, Block.FACE_WEST,
                Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT));
        assertEquals(108, textureIndex(BlockType.PISTON_HEAD, Block.FACE_TOP,
                Block.FACE_EAST | RedstoneEngine.PISTON_HEAD_STICKY_BIT));
    }

    @Test
    @DisplayName("Release 1.0 utility blocks should use their partial physical shapes")
    void releaseOneUtilityBlocksUsePartialShapes() {
        VoxelShape brewing = BlockShape.collisionShape(BlockState.of(BlockType.BREWING_STAND), emptyContext());
        VoxelShape brewingRender = BlockShape.renderShape(BlockState.of(BlockType.BREWING_STAND), emptyContext());
        VoxelShape fullBrewingRender = BlockShape.renderShape(new BlockState(BlockType.BREWING_STAND, 7),
                emptyContext());
        assertFalse(brewing.isEmpty());
        assertFalse(brewing.isFullCube());
        assertEquals(2, brewing.boxes().size());
        assertEquals(2, brewingRender.boxes().size());
        assertEquals(5, fullBrewingRender.boxes().size());
        assertCuboid(brewing.boxes().get(0),
                7.0f / 16.0f, 0.0f, 7.0f / 16.0f,
                9.0f / 16.0f, 14.0f / 16.0f, 9.0f / 16.0f);
        assertCuboid(brewing.boxes().get(1),
                0.0f, 0.0f, 0.0f,
                1.0f, 2.0f / 16.0f, 1.0f);
        assertEquals(1, BlockType.BREWING_STAND.getLightEmission());

        VoxelShape cauldron = BlockShape.collisionShape(BlockState.of(BlockType.CAULDRON), emptyContext());
        assertFalse(cauldron.isEmpty());
        assertFalse(cauldron.isFullCube());
        assertEquals(5, cauldron.boxes().size());
        assertFalse(cauldron.boxes().stream().anyMatch(box ->
                box.minX() < 0.5f && box.maxX() > 0.5f
                        && box.minZ() < 0.5f && box.maxZ() > 0.5f
                        && box.maxY() > 5.0f / 16.0f));

        VoxelShape enchanting = BlockShape.collisionShape(BlockState.of(BlockType.ENCHANTING_TABLE), emptyContext());
        assertFalse(enchanting.isEmpty());
        assertFalse(enchanting.isFullCube());
        assertEquals(12.0f / 16.0f, enchanting.boxes().get(0).maxY(), 0.0001f);

        VoxelShape frame = BlockShape.collisionShape(BlockState.of(BlockType.END_PORTAL_FRAME), emptyContext());
        assertFalse(frame.isEmpty());
        assertFalse(frame.isFullCube());
        assertEquals(1, frame.boxes().size());
        assertEquals(13.0f / 16.0f, frame.boxes().get(0).maxY(), 0.0001f);

        VoxelShape eyedFrame = BlockShape.collisionShape(
                new BlockState(BlockType.END_PORTAL_FRAME, World.END_PORTAL_FRAME_EYE_BIT), emptyContext());
        assertFalse(eyedFrame.isFullCube());
        assertEquals(2, eyedFrame.boxes().size());
        BlockShape.Cuboid eye = eyedFrame.boxes().get(1);
        assertEquals(5.0f / 16.0f, eye.minX(), 0.0001f);
        assertEquals(13.0f / 16.0f, eye.minY(), 0.0001f);
        assertEquals(11.0f / 16.0f, eye.maxZ(), 0.0001f);
    }

    @Test
    @DisplayName("Glass panes and iron bars should use thin connectable shapes")
    void panesAndBarsUseConnectableShapes() {
        VoxelShape isolated = BlockShape.collisionShape(BlockState.of(BlockType.GLASS_PANE), emptyContext());
        VoxelShape isolatedSelection = BlockShape.selectionShape(BlockState.of(BlockType.GLASS_PANE), emptyContext());
        assertFalse(isolated.isEmpty());
        assertFalse(isolated.isFullCube());
        assertEquals(2, isolated.boxes().size());
        assertCuboid(isolated.boxes().get(0),
                0.0f, 0.0f, 7.0f / 16.0f,
                1.0f, 1.0f, 9.0f / 16.0f);
        assertCuboid(isolated.boxes().get(1),
                7.0f / 16.0f, 0.0f, 0.0f,
                9.0f / 16.0f, 1.0f, 1.0f);
        assertTrue(isolatedSelection.isFullCube());

        VoxelShape connected = BlockShape.collisionShape(BlockState.of(BlockType.GLASS_PANE),
                contextWithNeighbors(BlockType.AIR, BlockType.AIR, BlockType.IRON_BARS, BlockType.STONE));
        assertFalse(connected.isFullCube());
        assertEquals(1, connected.boxes().size());
        assertCuboid(connected.boxes().get(0),
                0.0f, 0.0f, 7.0f / 16.0f,
                1.0f, 1.0f, 9.0f / 16.0f);

        VoxelShape eastOnly = BlockShape.collisionShape(BlockState.of(BlockType.GLASS_PANE),
                contextWithNeighbors(BlockType.AIR, BlockType.AIR, BlockType.STONE, BlockType.AIR));
        VoxelShape eastOnlySelection = BlockShape.selectionShape(BlockState.of(BlockType.GLASS_PANE),
                contextWithNeighbors(BlockType.AIR, BlockType.AIR, BlockType.STONE, BlockType.AIR));
        assertEquals(1, eastOnly.boxes().size());
        assertCuboid(eastOnly.boxes().get(0),
                0.5f, 0.0f, 7.0f / 16.0f,
                1.0f, 1.0f, 9.0f / 16.0f);
        assertCuboid(eastOnlySelection.boxes().get(0),
                7.0f / 16.0f, 0.0f, 7.0f / 16.0f,
                1.0f, 1.0f, 9.0f / 16.0f);

        VoxelShape bars = BlockShape.collisionShape(BlockState.of(BlockType.IRON_BARS),
                contextWithNeighbors(BlockType.GLASS_PANE, BlockType.GLASS, BlockType.AIR, BlockType.AIR));
        assertEquals(1, bars.boxes().size());
        assertCuboid(bars.boxes().get(0),
                7.0f / 16.0f, 0.0f, 0.0f,
                9.0f / 16.0f, 1.0f, 1.0f);
        assertTrue(BlockShape.canPaneConnectTo(BlockType.STONE));
        assertTrue(BlockShape.canPaneConnectTo(BlockType.GLASS));
        assertTrue(BlockShape.canPaneConnectTo(BlockType.GLASS_PANE));
        assertTrue(BlockShape.canPaneConnectTo(BlockType.IRON_BARS));
        assertFalse(BlockShape.canPaneConnectTo(BlockType.AIR));
    }

    private static BlockShape.BlockContext emptyContext() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static BlockShape.BlockContext contextWithBelow(BlockType below) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == 0 && dy == -1 && dz == 0 ? below : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static BlockShape.BlockContext contextWithBlockAt(int blockDx, int blockDy, int blockDz, BlockType type) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == blockDx && dy == blockDy && dz == blockDz ? type : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static BlockShape.BlockContext contextWithNeighbors(BlockType north, BlockType south, BlockType east,
            BlockType west) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                if (dx == 0 && dy == 0 && dz == -1) {
                    return north;
                }
                if (dx == 0 && dy == 0 && dz == 1) {
                    return south;
                }
                if (dx == 1 && dy == 0 && dz == 0) {
                    return east;
                }
                if (dx == -1 && dy == 0 && dz == 0) {
                    return west;
                }
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static BlockShape.BlockContext doorContext(int lowerMetadata, int upperMetadata) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == 0 && dz == 0 && (dy == -1 || dy == 1) ? BlockType.WOODEN_DOOR : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                if (dx == 0 && dy == -1 && dz == 0) {
                    return lowerMetadata;
                }
                if (dx == 0 && dy == 1 && dz == 0) {
                    return upperMetadata;
                }
                return 0;
            }
        };
    }

    private static void assertCuboid(BlockShape.Cuboid box, float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        assertEquals(minX, box.minX(), 0.0001f);
        assertEquals(minY, box.minY(), 0.0001f);
        assertEquals(minZ, box.minZ(), 0.0001f);
        assertEquals(maxX, box.maxX(), 0.0001f);
        assertEquals(maxY, box.maxY(), 0.0001f);
        assertEquals(maxZ, box.maxZ(), 0.0001f);
    }

    private static int textureIndex(BlockType type, int face, int metadata) {
        float[] uv = type.getTextureCoords(face, metadata);
        int col = (int) Math.floor(uv[0] / BlockType.TEXTURE_SIZE);
        int row = (int) Math.floor(uv[1] / BlockType.TEXTURE_SIZE);
        return row * BlockType.ATLAS_SIZE + col;
    }
}
