package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TreeMetadataInteractionTest {

    @Test
    @DisplayName("Tree-family block textures should honor Release 1.0 metadata variants")
    void treeFamilyTexturesHonorMetadataVariants() {
        assertEquals(15, textureIndex(BlockType.SAPLING, Block.FACE_NORTH, 0));
        assertEquals(63, textureIndex(BlockType.SAPLING, Block.FACE_NORTH, 1));
        assertEquals(79, textureIndex(BlockType.SAPLING, Block.FACE_NORTH, 2));

        assertEquals(20, textureIndex(BlockType.OAK_LOG, Block.FACE_NORTH, 0));
        assertEquals(116, textureIndex(BlockType.OAK_LOG, Block.FACE_NORTH, 1));
        assertEquals(117, textureIndex(BlockType.OAK_LOG, Block.FACE_NORTH, 2));
        assertEquals(21, textureIndex(BlockType.OAK_LOG, Block.FACE_TOP, 2));

        assertEquals(53, textureIndex(BlockType.LEAVES, Block.FACE_NORTH, 0));
        assertEquals(133, textureIndex(BlockType.LEAVES, Block.FACE_NORTH, 1));
        assertEquals(53, textureIndex(BlockType.LEAVES, Block.FACE_NORTH, 2));
    }

    @Test
    @DisplayName("Bone meal should carry sapling metadata into grown tree logs and leaves")
    void boneMealCarriesSaplingMetadataIntoTreeBlocks() {
        assertGrownTreeMetadata(0);
        assertGrownTreeMetadata(1);
        assertGrownTreeMetadata(2);
    }

    @Test
    @DisplayName("Spruce sapling bone meal should use the Release 1.0 taiga tree generator")
    void spruceSaplingBoneMealUsesTaigaConiferShape() {
        World world = new World(5910L);
        try {
            prepareSaplingFixture(world, 1);
            world.getRandom().setSeed(2L);

            assertTrue(world.applyBoneMealToPlant(8, 71, 8));

            assertSame(BlockType.OAK_LOG, world.getBlock(8, 78, 8));
            assertEquals(1, world.getBlockMetadata(8, 78, 8) & 3);
            assertSame(BlockType.LEAVES, world.getBlock(8, 79, 8));
            assertEquals(1, world.getBlockMetadata(8, 79, 8) & 3);
            assertTrue(hasTreeBlockWithMetadata(world, BlockType.LEAVES, 1,
                    5, 72, 5, 11, 79, 11));
        } finally {
            world.cleanup();
        }
    }

    private static void assertGrownTreeMetadata(int metadata) {
        World world = new World(5900L + metadata);
        try {
            prepareSaplingFixture(world, metadata);

            assertTrue(world.applyBoneMealToPlant(8, 71, 8));

            assertSame(BlockType.OAK_LOG, world.getBlock(8, 71, 8));
            assertEquals(metadata, world.getBlockMetadata(8, 71, 8) & 3);
            assertTrue(hasTreeBlockWithMetadata(world, BlockType.LEAVES, metadata,
                    6, 73, 6, 10, 81, 10));
        } finally {
            world.cleanup();
        }
    }

    private static void prepareSaplingFixture(World world, int metadata) {
        for (int y = 71; y <= 84; y++) {
            for (int z = 4; z <= 12; z++) {
                for (int x = 4; x <= 12; x++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
        world.setBlock(8, 70, 8, BlockType.GRASS, 0);
        world.setBlock(8, 71, 8, BlockType.SAPLING, metadata);
    }

    private static boolean hasTreeBlockWithMetadata(World world, BlockType type, int metadata,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (world.getBlock(x, y, z) == type && (world.getBlockMetadata(x, y, z) & 3) == metadata) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int textureIndex(BlockType type, int face, int metadata) {
        float[] coords = type.getTextureCoords(face, metadata);
        int col = Math.round((coords[0] - 0.001f) / BlockType.TEXTURE_SIZE);
        int row = Math.round((coords[1] - 0.001f) / BlockType.TEXTURE_SIZE);
        return row * BlockType.ATLAS_SIZE + col;
    }
}
