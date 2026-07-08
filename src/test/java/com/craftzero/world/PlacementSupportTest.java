package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacementSupportTest {
    @Test
    @DisplayName("Plants should break and drop when their support is removed")
    void plantsBreakWhenSupportIsRemoved() {
        World world = new World(51L);
        try {
            world.setBlock(0, 70, 0, BlockType.DIRT);
            world.setBlock(0, 71, 0, BlockType.YELLOW_FLOWER);

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.YELLOW_FLOWER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cactus should break when adjacent solid blocks invalidate placement")
    void cactusBreaksWhenAdjacentSolidAppears() {
        World world = new World(52L);
        try {
            world.setBlock(0, 70, 0, BlockType.SAND);
            world.setBlock(1, 71, 0, BlockType.AIR);
            world.setBlock(0, 71, 0, BlockType.CACTUS);
            assertSame(BlockType.CACTUS, world.getBlock(0, 71, 0));

            world.setBlock(1, 71, 0, BlockType.STONE);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.CACTUS));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flowers should use Release 1.0 farmland support at runtime")
    void flowersUseFarmlandSupportAtRuntime() {
        World world = new World(5101L);
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, World.FARMLAND_MAX_MOISTURE);

            assertTrue(world.canPlaceBlockAt(0, 71, 0, BlockType.RED_ROSE, 0, null));
            world.setBlock(0, 71, 0, BlockType.RED_ROSE);

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.RED_ROSE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mushrooms should use Release 1.0 mycelium support at runtime")
    void mushroomsUseMyceliumSupportAtRuntime() {
        World world = new World(5102L);
        try {
            world.setBlock(0, 70, 0, BlockType.MYCELIUM);

            assertTrue(world.canPlaceBlockAt(0, 71, 0, BlockType.BROWN_MUSHROOM, 0, null));
            world.setBlock(0, 71, 0, BlockType.BROWN_MUSHROOM);

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.BROWN_MUSHROOM));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mushrooms should require covered low-light opaque support away from mycelium")
    void mushroomsRequireCoveredLowLightOpaqueSupportAwayFromMycelium() {
        World world = new World(5103L);
        try {
            world.setBlock(0, 70, 0, BlockType.GRAVEL);
            assertFalse(world.canPlaceBlockAt(0, 71, 0, BlockType.BROWN_MUSHROOM, 0, null));

            world.setBlock(2, 70, 0, BlockType.GRAVEL);
            world.setBlock(2, 72, 0, BlockType.STONE);
            assertTrue(world.canPlaceBlockAt(2, 71, 0, BlockType.BROWN_MUSHROOM, 0, null));

            world.setBlock(4, 70, 0, BlockType.GRAVEL);
            world.setBlock(4, 72, 0, BlockType.STONE);
            world.setBlock(5, 71, 0, BlockType.TORCH, Block.FACE_WEST);
            assertTrue(world.getBlockLight(4, 71, 0) >= 13);
            assertFalse(world.canPlaceBlockAt(4, 71, 0, BlockType.RED_MUSHROOM, 0, null));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mushrooms on opaque support should pop when neighbor light becomes too bright")
    void mushroomsOnOpaqueSupportPopWhenNeighborLightBecomesTooBright() {
        World world = new World(5104L);
        try {
            world.setBlock(0, 70, 0, BlockType.GRAVEL);
            world.setBlock(0, 72, 0, BlockType.STONE);
            world.setBlock(0, 71, 0, BlockType.BROWN_MUSHROOM);

            world.setBlock(1, 71, 0, BlockType.TORCH, Block.FACE_WEST);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.BROWN_MUSHROOM));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cactus should use source material blocking, not collision solidity")
    void cactusUsesSourceMaterialBlocking() {
        World world = new World(521L);
        try {
            world.setBlock(0, 70, 0, BlockType.SAND);
            world.setBlock(0, 71, 0, BlockType.CACTUS);
            world.setBlock(1, 70, 0, BlockType.STONE);

            world.setBlock(1, 71, 0, BlockType.REDSTONE_WIRE);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0),
                    "Release 1.0 cactus checks neighboring material, so redstone wire still invalidates it");
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.CACTUS));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small non-solid blocks should not act as top placement anchors")
    void smallBlocksDoNotAnchorTopPlacement() {
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.TORCH, Block.FACE_TOP));
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.YELLOW_FLOWER, Block.FACE_TOP));
        assertTrue(BlockShape.blocksPlacementAgainst(BlockType.FIRE, Block.FACE_TOP));
        assertFalse(BlockShape.blocksPlacementAgainst(BlockType.STONE, Block.FACE_TOP));
    }

    @Test
    @DisplayName("Redstone floor devices should require source-style normal top support")
    void redstoneFloorDevicesRequireNormalTopSupport() {
        BlockType[] floorDevices = {
                BlockType.RAIL,
                BlockType.POWERED_RAIL,
                BlockType.DETECTOR_RAIL,
                BlockType.REDSTONE_REPEATER_OFF,
                BlockType.REDSTONE_REPEATER_ON,
                BlockType.STONE_PRESSURE_PLATE,
                BlockType.WOODEN_PRESSURE_PLATE
        };

        assertTrue(BlockShape.canPlaceAt(BlockType.REDSTONE_WIRE, 0, contextWithBlockAt(0, -1, 0, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.REDSTONE_WIRE, 0,
                contextWithBlockAt(0, -1, 0, BlockType.GLOWSTONE)));
        assertFalse(BlockShape.canPlaceAt(BlockType.REDSTONE_WIRE, 0,
                contextWithBlockAt(0, -1, 0, BlockType.GLASS)));
        assertFalse(BlockShape.canPlaceAt(BlockType.REDSTONE_WIRE, 0,
                contextWithBlockAt(0, -1, 0, BlockType.CHEST)));

        for (BlockType type : floorDevices) {
            assertTrue(BlockShape.canPlaceAt(type, 0, contextWithBlockAt(0, -1, 0, BlockType.STONE)),
                    type + " should accept a normal opaque block below");
            assertTrue(BlockShape.canPlaceAt(type, 0, contextWithBlockAt(0, -1, 0, BlockType.FURNACE)),
                    type + " should accept a normal furnace block below");
            assertFalse(BlockShape.canPlaceAt(type, 0, contextWithBlockAt(0, -1, 0, BlockType.GLASS)),
                    type + " should reject transparent glass support");
            assertFalse(BlockShape.canPlaceAt(type, 0, contextWithBlockAt(0, -1, 0, BlockType.CHEST)),
                    type + " should reject non-normal chest support");
        }
    }

    @Test
    @DisplayName("Fire should require opaque support below or a flammable neighbor")
    void fireRequiresSupportOrFlammableNeighbor() {
        assertFalse(BlockShape.canPlaceAt(BlockType.FIRE, 0, emptyContext()));
        assertTrue(BlockShape.canPlaceAt(BlockType.FIRE, 0, emptyContextWithStoneBelow()));
        assertTrue(BlockShape.canPlaceAt(BlockType.FIRE, 0,
                contextWithBlockAt(1, 0, 0, BlockType.OAK_PLANKS)));
    }

    @Test
    @DisplayName("Torches should use Release 1.0 source wall metadata")
    void torchesUseReleaseOneSourceWallMetadata() {
        assertEquals(1, BlockShape.torchMetadataFromFace(Block.FACE_EAST));
        assertEquals(2, BlockShape.torchMetadataFromFace(Block.FACE_WEST));
        assertEquals(3, BlockShape.torchMetadataFromFace(Block.FACE_SOUTH));
        assertEquals(4, BlockShape.torchMetadataFromFace(Block.FACE_NORTH));
        assertEquals(5, BlockShape.torchMetadataFromFace(Block.FACE_TOP));
        assertEquals(-1, BlockShape.torchMetadataFromFace(Block.FACE_BOTTOM));

        assertTrue(BlockShape.canPlaceAt(BlockType.TORCH, 1,
                contextWithBlockAt(-1, 0, 0, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.TORCH, 2,
                contextWithBlockAt(1, 0, 0, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.TORCH, 3,
                contextWithBlockAt(0, 0, -1, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.TORCH, 4,
                contextWithBlockAt(0, 0, 1, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.TORCH, 5, emptyContextWithStoneBelow()));
        assertFalse(BlockShape.canPlaceAt(BlockType.TORCH, 1,
                contextWithBlockAt(1, 0, 0, BlockType.STONE)));
    }

    @Test
    @DisplayName("Wall torches should drop when their source-metadata support is removed")
    void wallTorchesDropFromSourceMetadataSupportRemoval() {
        World world = new World(522L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(1, 70, 0, BlockType.TORCH, 1);
            assertSame(BlockType.TORCH, world.getBlock(1, 70, 0));

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(1, 70, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.TORCH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ladders and wall signs should use Release 1.0 source wall metadata")
    void laddersAndWallSignsUseReleaseOneSourceWallMetadata() {
        assertEquals(2, BlockShape.wallAttachmentMetadataFromFace(Block.FACE_NORTH));
        assertEquals(3, BlockShape.wallAttachmentMetadataFromFace(Block.FACE_SOUTH));
        assertEquals(4, BlockShape.wallAttachmentMetadataFromFace(Block.FACE_WEST));
        assertEquals(5, BlockShape.wallAttachmentMetadataFromFace(Block.FACE_EAST));
        assertEquals(-1, BlockShape.wallAttachmentMetadataFromFace(Block.FACE_TOP));

        assertEquals(Block.FACE_WEST, BlockShape.wallAttachmentFaceFromMetadata(4));
        assertEquals(Block.FACE_EAST, BlockShape.wallAttachmentFaceFromMetadata(5));
        assertTrue(BlockShape.canPlaceAt(BlockType.LADDER, 4,
                contextWithBlockAt(1, 0, 0, BlockType.STONE)));
        assertTrue(BlockShape.canPlaceAt(BlockType.WALL_SIGN, 5,
                contextWithBlockAt(-1, 0, 0, BlockType.STONE)));
        assertFalse(BlockShape.canPlaceAt(BlockType.LADDER, 4,
                contextWithBlockAt(-1, 0, 0, BlockType.STONE)));

        VoxelShape westLadder = BlockShape.selectionShape(new BlockState(BlockType.LADDER, 4), emptyContextWithStoneBelow());
        VoxelShape eastSign = BlockShape.selectionShape(new BlockState(BlockType.WALL_SIGN, 5), emptyContextWithStoneBelow());

        assertEquals(7.0f / 8.0f, westLadder.boxes().get(0).minX(), 0.0001f);
        assertEquals(0.0f, eastSign.boxes().get(0).minX(), 0.0001f);
        assertEquals(1.0f / 8.0f, eastSign.boxes().get(0).maxX(), 0.0001f);
    }

    @Test
    @DisplayName("Doors should require Release-style normal top support")
    void doorsRequireReleaseOneNormalTopSupport() {
        World world = new World(529L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            world.setBlock(2, 100, 0, BlockType.FURNACE, 0);
            world.setBlock(4, 100, 0, BlockType.GLASS, 0);
            world.setBlock(6, 100, 0, BlockType.CHEST, 0);
            world.setBlock(8, 100, 0, BlockType.OAK_STAIRS, 0);

            assertTrue(BlockShape.canSupportDoor(BlockType.STONE));
            assertTrue(BlockShape.canSupportDoor(BlockType.FURNACE));
            assertFalse(BlockShape.canSupportDoor(BlockType.GLASS));
            assertFalse(BlockShape.canSupportDoor(BlockType.CHEST));
            assertFalse(BlockShape.canSupportDoor(BlockType.OAK_STAIRS));

            assertTrue(world.placeDoor(0, 101, 0, BlockType.WOODEN_DOOR, 0, null));
            assertTrue(world.placeDoor(2, 101, 0, BlockType.IRON_DOOR, 0, null));
            assertFalse(world.placeDoor(4, 101, 0, BlockType.WOODEN_DOOR, 0, null));
            assertFalse(world.placeDoor(6, 101, 0, BlockType.WOODEN_DOOR, 0, null));
            assertFalse(world.placeDoor(8, 101, 0, BlockType.IRON_DOOR, 0, null));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Doors should break when their normal top support becomes invalid")
    void doorsBreakWhenNormalTopSupportBecomesInvalid() {
        World world = new World(530L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            assertTrue(world.placeDoor(0, 101, 0, BlockType.WOODEN_DOOR, 0, null));

            world.setBlock(0, 100, 0, BlockType.GLASS, 0);

            assertSame(BlockType.AIR, world.getBlock(0, 101, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 102, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.WOODEN_DOOR));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Trapdoors should use Release-style side anchors")
    void trapdoorsUseReleaseOneSideAnchors() {
        World world = new World(53L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(2, 70, 0, BlockType.STONE_SLAB);
            world.setBlock(4, 70, 0, BlockType.OAK_STAIRS);
            world.setBlock(6, 70, 0, BlockType.GLOWSTONE);
            world.setBlock(8, 70, 0, BlockType.CHEST);
            world.setBlock(10, 70, 0, BlockType.GLASS);

            assertFalse(world.placeTrapdoor(0, 71, 0, Block.FACE_TOP, null));
            assertFalse(world.placeTrapdoor(0, 69, 0, Block.FACE_BOTTOM, null));
            assertFalse(world.placeTrapdoor(12, 70, 0, Block.FACE_NORTH, null));

            assertTrue(world.placeTrapdoor(0, 70, -1, Block.FACE_NORTH, null));
            assertTrue(world.placeTrapdoor(2, 70, -1, Block.FACE_NORTH, null));
            assertTrue(world.placeTrapdoor(4, 70, -1, Block.FACE_NORTH, null));
            assertTrue(world.placeTrapdoor(6, 70, -1, Block.FACE_NORTH, null));
            assertFalse(world.placeTrapdoor(8, 70, -1, Block.FACE_NORTH, null));
            assertFalse(world.placeTrapdoor(10, 70, -1, Block.FACE_NORTH, null));

            int metadata = world.getBlockMetadata(0, 70, -1);
            assertSame(BlockType.TRAPDOOR, world.getBlock(0, 70, -1));
            assertEquals(0, metadata & 3);
            assertEquals(0, metadata & 8);

            world.breakBlock(0, 70, 0, false);
            assertSame(BlockType.AIR, world.getBlock(0, 70, -1));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.TRAPDOOR));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fence gates should require buildable support only when placed")
    void fenceGatesRequireBuildableSupportOnlyAtPlacement() {
        World world = new World(5301L);
        try {
            assertFalse(world.canPlaceBlockAt(0, 70, 0, BlockType.FENCE_GATE, 0, null));

            world.setBlock(0, 69, 0, BlockType.STONE);
            world.setBlock(2, 69, 0, BlockType.WATER);
            world.setBlock(4, 69, 0, BlockType.GLASS);

            assertTrue(world.canPlaceBlockAt(0, 70, 0, BlockType.FENCE_GATE, 0, null));
            assertFalse(world.canPlaceBlockAt(2, 70, 0, BlockType.FENCE_GATE, 0, null));
            assertTrue(world.canPlaceBlockAt(4, 70, 0, BlockType.FENCE_GATE, 0, null));

            world.setBlock(0, 70, 0, BlockType.FENCE_GATE, 0);
            world.breakBlock(0, 69, 0, false);

            assertSame(BlockType.FENCE_GATE, world.getBlock(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stone buttons should place only on side anchors")
    void stoneButtonsUseReleaseOneSideAnchors() {
        World world = new World(54L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(2, 70, 0, BlockType.GLASS);
            world.setBlock(4, 70, 0, BlockType.CHEST);
            world.setBlock(6, 70, 0, BlockType.FURNACE);

            assertEquals(1, BlockShape.buttonMetadataFromFace(Block.FACE_EAST));
            assertEquals(2, BlockShape.buttonMetadataFromFace(Block.FACE_WEST));
            assertEquals(3, BlockShape.buttonMetadataFromFace(Block.FACE_SOUTH));
            assertEquals(4, BlockShape.buttonMetadataFromFace(Block.FACE_NORTH));
            assertEquals(-1, BlockShape.buttonMetadataFromFace(Block.FACE_TOP));
            assertEquals(-1, BlockShape.buttonMetadataFromFace(Block.FACE_BOTTOM));
            assertFalse(BlockShape.canPlaceAt(BlockType.STONE_BUTTON, 5, emptyContextWithStoneBelow()));
            assertFalse(world.placeStoneButton(0, 71, 0, Block.FACE_TOP, null));
            assertFalse(world.placeStoneButton(0, 69, 0, Block.FACE_BOTTOM, null));
            assertFalse(world.placeStoneButton(8, 70, 0, Block.FACE_NORTH, null));

            assertTrue(world.placeStoneButton(0, 70, -1, Block.FACE_NORTH, null));
            assertFalse(world.placeStoneButton(2, 70, -1, Block.FACE_NORTH, null));
            assertFalse(world.placeStoneButton(4, 70, -1, Block.FACE_NORTH, null));
            assertTrue(world.placeStoneButton(6, 70, -1, Block.FACE_NORTH, null));

            int metadata = world.getBlockMetadata(0, 70, -1);
            assertSame(BlockType.STONE_BUTTON, world.getBlock(0, 70, -1));
            assertEquals(4, metadata & 7);
            assertEquals(0, metadata & RedstoneEngine.POWERED_BIT);

            world.breakBlock(0, 70, 0, false);
            assertSame(BlockType.AIR, world.getBlock(0, 70, -1));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.STONE_BUTTON));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Levers should place on walls, floors, and ceilings")
    void leversUseReleaseOneWallAndFloorAnchors() {
        World world = new World(55L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(2, 70, 0, BlockType.GLASS);
            world.setBlock(4, 70, 0, BlockType.CHEST);
            world.setBlock(6, 70, 0, BlockType.FURNACE);

            assertEquals(5, BlockShape.leverMetadataFromFace(Block.FACE_TOP, false));
            assertEquals(6, BlockShape.leverMetadataFromFace(Block.FACE_TOP, true));
            assertEquals(0, BlockShape.leverMetadataFromFace(Block.FACE_BOTTOM, false));
            assertEquals(7, BlockShape.leverMetadataFromFace(Block.FACE_BOTTOM, true));
            assertTrue(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_TOP),
                    emptyContextWithStoneBelow()));
            assertTrue(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_BOTTOM),
                    contextWithBlockAt(0, 1, 0, BlockType.STONE)));
            assertTrue(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_BOTTOM, true),
                    contextWithBlockAt(0, 1, 0, BlockType.STONE)));
            assertFalse(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_TOP),
                    contextWithBlockAt(0, -1, 0, BlockType.GLASS)));
            assertFalse(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_BOTTOM),
                    contextWithBlockAt(0, 1, 0, BlockType.CHEST)));
            assertTrue(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_WEST),
                    contextWithBlockAt(1, 0, 0, BlockType.FURNACE)));
            assertFalse(BlockShape.canPlaceAt(
                    BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_WEST),
                    emptyContextWithStoneBelow()));
            assertFalse(world.placeLever(4, 70, 0, Block.FACE_NORTH, null));
            assertFalse(world.placeLever(2, 71, 0, Block.FACE_TOP, null));
            assertFalse(world.placeLever(4, 71, 0, Block.FACE_TOP, null));
            assertTrue(world.placeLever(6, 71, 0, Block.FACE_TOP, null));

            world.setBlock(20, 70, 0, BlockType.STONE);
            world.setBlock(22, 70, 0, BlockType.GLASS);
            world.setBlock(24, 70, 0, BlockType.CHEST);
            world.setBlock(26, 70, 0, BlockType.FURNACE);
            assertTrue(world.placeLever(20, 69, 0, Block.FACE_BOTTOM, null));
            assertFalse(world.placeLever(22, 69, 0, Block.FACE_BOTTOM, null));
            assertFalse(world.placeLever(24, 69, 0, Block.FACE_BOTTOM, null));
            assertTrue(world.placeLever(26, 69, 0, Block.FACE_BOTTOM, null));
            int ceilingMetadata = world.getBlockMetadata(20, 69, 0) & 7;
            assertTrue(ceilingMetadata == 0 || ceilingMetadata == 7);

            world.breakBlock(20, 70, 0, false);
            assertSame(BlockType.AIR, world.getBlock(20, 69, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.LEVER));

            assertTrue(world.placeLever(0, 71, 0, Block.FACE_TOP, null));
            int floorMetadata = world.getBlockMetadata(0, 71, 0);
            assertSame(BlockType.LEVER, world.getBlock(0, 71, 0));
            assertTrue((floorMetadata & 7) == 5 || (floorMetadata & 7) == 6);

            world.breakBlock(0, 70, 0, false);
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.LEVER));

            world.setBlock(10, 70, 0, BlockType.STONE);
            world.setBlock(12, 70, 0, BlockType.GLASS);
            world.setBlock(14, 70, 0, BlockType.CHEST);
            world.setBlock(16, 70, 0, BlockType.FURNACE);
            assertTrue(world.placeLever(9, 70, 0, Block.FACE_WEST, null));
            assertFalse(world.placeLever(11, 70, 0, Block.FACE_WEST, null));
            assertFalse(world.placeLever(13, 70, 0, Block.FACE_WEST, null));
            assertTrue(world.placeLever(15, 70, 0, Block.FACE_WEST, null));
            int wallMetadata = world.getBlockMetadata(9, 70, 0);
            assertSame(BlockType.LEVER, world.getBlock(9, 70, 0));
            assertEquals(BlockShape.leverMetadataFromFace(Block.FACE_WEST), wallMetadata & 7);
            assertNotEquals(5, wallMetadata & 7);

            world.breakBlock(10, 70, 0, false);
            assertSame(BlockType.AIR, world.getBlock(9, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered levers should strongly power only their attachment block")
    void poweredLeversStronglyPowerOnlyTheirAnchor() {
        World world = new World(56L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            assertTrue(world.placeLever(0, 71, 0, Block.FACE_TOP, null));
            world.setBlock(0, 71, 0, BlockType.LEVER, world.getBlockMetadata(0, 71, 0) | RedstoneEngine.POWERED_BIT);

            assertEquals(15, RedstoneEngine.getStrongPower(world, 0, 71, 0, Block.FACE_BOTTOM));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 0, 71, 0, Block.FACE_EAST));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_EAST));

            world.setBlock(2, 72, 0, BlockType.STONE);
            world.setBlock(2, 71, 0, BlockType.AIR);
            assertTrue(world.placeLever(2, 71, 0, Block.FACE_BOTTOM, null));
            world.setBlock(2, 71, 0, BlockType.LEVER, world.getBlockMetadata(2, 71, 0) | RedstoneEngine.POWERED_BIT);

            assertEquals(15, RedstoneEngine.getStrongPower(world, 2, 71, 0, Block.FACE_TOP));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 2, 71, 0, Block.FACE_BOTTOM));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 2, 71, 0, Block.FACE_EAST));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Buttons, pressure plates, and detector rails should strongly power only their vanilla anchor blocks")
    void poweredControlsStronglyPowerOnlyTheirAnchorBlocks() {
        World world = new World(57L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(0, 70, -1, BlockType.AIR);
            assertTrue(world.placeStoneButton(0, 70, -1, Block.FACE_NORTH, null));
            world.toggleBlock(0, 70, -1);

            assertEquals(15, RedstoneEngine.getStrongPower(world, 0, 70, -1, Block.FACE_SOUTH));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 0, 70, -1, Block.FACE_NORTH));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 0, 70, -1, Block.FACE_EAST));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 70, -1, Block.FACE_EAST));

            world.setBlock(2, 70, 0, BlockType.STONE);
            world.setBlock(2, 71, 0, BlockType.STONE_PRESSURE_PLATE, 1);

            assertEquals(15, RedstoneEngine.getStrongPower(world, 2, 71, 0, Block.FACE_BOTTOM));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 2, 71, 0, Block.FACE_TOP));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 2, 71, 0, Block.FACE_EAST));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 2, 71, 0, Block.FACE_EAST));

            world.setBlock(4, 70, 0, BlockType.STONE);
            world.setBlock(4, 71, 0, BlockType.DETECTOR_RAIL, RedstoneEngine.RAIL_POWERED_BIT);

            assertEquals(15, RedstoneEngine.getStrongPower(world, 4, 71, 0, Block.FACE_BOTTOM));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 4, 71, 0, Block.FACE_TOP));
            assertEquals(0, RedstoneEngine.getStrongPower(world, 4, 71, 0, Block.FACE_EAST));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 4, 71, 0, Block.FACE_EAST));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone and rail devices should be selectable but not full collision cubes")
    void redstoneAndRailDevicesUseThinNonCollidingShapes() {
        BlockShape.BlockContext context = emptyContextWithStoneBelow();
        BlockType[] thinBlocks = {
                BlockType.REDSTONE_WIRE,
                BlockType.RAIL,
                BlockType.POWERED_RAIL,
                BlockType.DETECTOR_RAIL,
                BlockType.REDSTONE_REPEATER_OFF,
                BlockType.REDSTONE_REPEATER_ON,
                BlockType.STONE_PRESSURE_PLATE,
                BlockType.WOODEN_PRESSURE_PLATE,
                BlockType.LEVER,
                BlockType.STONE_BUTTON
        };

        for (BlockType type : thinBlocks) {
            assertTrue(BlockShape.getCollisionBoxes(type, 5, context).isEmpty(), type + " should not block movement");
            assertFalse(BlockShape.getSelectionBoxes(type, 5, context).isEmpty(), type + " should be selectable");
            assertFalse(BlockShape.isFullCube(type, 5), type + " should not render as a full cube");
        }
    }

    private static BlockShape.BlockContext emptyContextWithStoneBelow() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dy == -1 ? BlockType.STONE : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
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

    private static BlockShape.BlockContext contextWithBlockAt(int supportDx, int supportDy, int supportDz,
            BlockType support) {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return dx == supportDx && dy == supportDy && dz == supportDz ? support : BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }
}
