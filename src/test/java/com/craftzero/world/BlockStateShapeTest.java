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
        BlockShape.Cuboid openNorth = BlockShape.renderShape(new BlockState(BlockType.TRAPDOOR, 4), emptyContext())
                .boxes().get(0);

        assertEquals(0.0f, bottom.minY(), 0.0001f);
        assertEquals(3.0f / 16.0f, bottom.maxY(), 0.0001f);
        assertEquals(13.0f / 16.0f, top.minY(), 0.0001f);
        assertEquals(1.0f, top.maxY(), 0.0001f);
        assertEquals(0.0f, openNorth.minZ(), 0.0001f);
        assertEquals(3.0f / 16.0f, openNorth.maxZ(), 0.0001f);
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
}
