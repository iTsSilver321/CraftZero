package com.craftzero.registry;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockRenderLayer;
import com.craftzero.world.BlockBehavior;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseOneRegistryTest {

    @Test
    @DisplayName("Block registry should have unique canonical IDs")
    void blockRegistryHasUniqueIds() {
        Set<Integer> ids = new HashSet<>();

        for (BlockType type : BlockType.values()) {
            assertTrue(ids.add(type.getId()), "Duplicate block id " + type.getId() + " for " + type);
            assertSame(type, BlockType.fromId(type.getId()));
            assertTrue(BlockType.hasId(type.getId()));
        }
    }

    @Test
    @DisplayName("Item registry should have unique canonical IDs")
    void itemRegistryHasUniqueIds() {
        Set<Long> ids = new HashSet<>();

        for (ItemType type : ItemType.values()) {
            long key = (((long) type.getId()) << 32) ^ (type.getDataValue() & 0xFFFFFFFFL);
            assertTrue(ids.add(key), "Duplicate item id/data " + type.getId() + ":" + type.getDataValue());
            assertSame(type, ItemType.fromId(type.getId(), type.getDataValue()));
            assertTrue(ItemType.hasId(type.getId(), type.getDataValue()));
        }
    }

    @Test
    @DisplayName("Metadata variants should resolve as distinct item identities")
    void metadataVariantsResolveDistinctly() {
        assertSame(ItemType.COAL, ItemType.fromId(263, 0));
        assertSame(ItemType.CHARCOAL, ItemType.fromId(263, 1));
        assertSame(ItemType.ROSE_RED, ItemType.fromId(351, 1));
        assertSame(ItemType.CACTUS_GREEN, ItemType.fromId(351, 2));
        assertSame(ItemType.COCOA_BEANS, ItemType.fromId(351, 3));
        assertSame(ItemType.LAPIS_LAZULI, ItemType.fromId(351, 4));
        assertSame(ItemType.BONE_MEAL, ItemType.fromId(351, 15));
        assertSame(ItemType.BLACK_WOOL, ItemType.fromId(35, 15));
        assertSame(ItemType.DOUBLE_STONE_SLAB, ItemType.fromId(43, 0));
        assertSame(ItemType.DOUBLE_SANDSTONE_SLAB, ItemType.fromId(43, 1));
        assertSame(ItemType.DOUBLE_WOODEN_SLAB, ItemType.fromId(43, 2));
        assertSame(ItemType.DOUBLE_COBBLESTONE_SLAB, ItemType.fromId(43, 3));
        assertSame(ItemType.DOUBLE_BRICK_SLAB, ItemType.fromId(43, 4));
        assertSame(ItemType.DOUBLE_STONE_BRICK_SLAB, ItemType.fromId(43, 5));
        assertSame(ItemType.COBBLESTONE_SLAB, ItemType.fromId(44, 3));
        assertSame(ItemType.SPRUCE_SAPLING, ItemType.fromId(6, 1));
        assertSame(ItemType.BIRCH_SAPLING, ItemType.fromId(6, 2));
        assertSame(ItemType.SPRUCE_LOG, ItemType.fromId(17, 1));
        assertSame(ItemType.BIRCH_LOG, ItemType.fromId(17, 2));
        assertSame(ItemType.SPRUCE_LEAVES, ItemType.fromId(18, 1));
        assertSame(ItemType.BIRCH_LEAVES, ItemType.fromId(18, 2));
        assertSame(ItemType.SHRUB, ItemType.fromId(31, 0));
        assertSame(ItemType.TALL_GRASS, ItemType.fromId(31, 1));
        assertSame(ItemType.FERN, ItemType.fromId(31, 2));
        assertSame(ItemType.MOSSY_STONE_BRICK, ItemType.fromId(98, 1));
        assertSame(ItemType.CRACKED_STONE_BRICK, ItemType.fromId(98, 2));
        assertSame(ItemType.CHISELED_STONE_BRICK, ItemType.fromId(98, 3));
        assertSame(ItemType.INFESTED_STONE, ItemType.fromId(97, 0));
        assertSame(ItemType.INFESTED_COBBLESTONE, ItemType.fromId(97, 1));
        assertSame(ItemType.INFESTED_STONE_BRICK, ItemType.fromId(97, 2));
    }

    @Test
    @DisplayName("Block items should map back to their placed blocks")
    void blockItemsMapToBlocks() {
        for (ItemType type : ItemType.values()) {
            if (!type.isBlockItem()) {
                continue;
            }

            assertSame(type, ItemType.fromBlock(type.getPlacedBlock(), type.getPlacedBlockMetadata()));
            if (type.getPlacedBlockMetadata() == 0) {
                assertSame(type, ItemType.fromBlock(type.getPlacedBlock()));
            }
            assertTrue(type.isPlaceable(), type + " should place its mapped block");
        }

        assertNull(ItemType.fromBlock(BlockType.AIR));
        assertNull(ItemType.fromBlock(BlockType.WATER));
        assertNull(ItemType.fromBlock(BlockType.LAVA));
        assertSame(ItemType.FURNACE, ItemType.fromBlock(BlockType.FURNACE));
        assertNull(ItemType.fromBlock(BlockType.LIT_FURNACE));
    }

    @Test
    @DisplayName("Release 1.0 interaction blocks should use canonical IDs")
    void interactionBlocksUseCanonicalIds() {
        assertEquals(50, BlockType.TORCH.getId());
        assertEquals(8, BlockType.FLOWING_WATER.getId());
        assertEquals(9, BlockType.WATER.getId());
        assertEquals(10, BlockType.FLOWING_LAVA.getId());
        assertEquals(11, BlockType.LAVA.getId());
        assertEquals(49, BlockType.OBSIDIAN.getId());
        assertEquals(51, BlockType.FIRE.getId());
        assertEquals(41, BlockType.GOLD_BLOCK.getId());
        assertEquals(42, BlockType.IRON_BLOCK.getId());
        assertEquals(6, BlockType.SAPLING.getId());
        assertEquals(37, BlockType.YELLOW_FLOWER.getId());
        assertEquals(38, BlockType.RED_ROSE.getId());
        assertEquals(26, BlockType.BED.getId());
        assertEquals(63, BlockType.STANDING_SIGN.getId());
        assertEquals(68, BlockType.WALL_SIGN.getId());
        assertEquals(64, BlockType.WOODEN_DOOR.getId());
        assertEquals(71, BlockType.IRON_DOOR.getId());
        assertEquals(65, BlockType.LADDER.getId());
        assertEquals(96, BlockType.TRAPDOOR.getId());
        assertEquals(85, BlockType.FENCE.getId());
        assertEquals(107, BlockType.FENCE_GATE.getId());
        assertEquals(53, BlockType.OAK_STAIRS.getId());
        assertEquals(67, BlockType.COBBLESTONE_STAIRS.getId());
        assertEquals(19, BlockType.SPONGE.getId());
        assertEquals(78, BlockType.SNOW_LAYER.getId());
        assertEquals(80, BlockType.SNOW.getId());
        assertEquals(95, BlockType.LOCKED_CHEST.getId());

        assertEquals(19, ItemType.SPONGE.getId());
        assertEquals(78, ItemType.SNOW_LAYER.getId());
        assertEquals(80, ItemType.SNOW.getId());
        assertEquals(95, ItemType.LOCKED_CHEST.getId());
        assertEquals(52, ItemType.MOB_SPAWNER.getId());
        assertSame(ItemType.MOB_SPAWNER, ItemType.fromId(52));
        assertSame(ItemType.MOB_SPAWNER, ItemType.fromBlock(BlockType.MOB_SPAWNER));
        assertEquals(43, ItemType.DOUBLE_STONE_SLAB.getId());
        assertEquals(43, ItemType.DOUBLE_STONE_BRICK_SLAB.getId());
        assertEquals(5, ItemType.DOUBLE_STONE_BRICK_SLAB.getDataValue());
        assertSame(ItemType.DOUBLE_STONE_SLAB, ItemType.fromBlock(BlockType.DOUBLE_STONE_SLAB));
        assertSame(ItemType.DOUBLE_WOODEN_SLAB, ItemType.fromBlock(BlockType.DOUBLE_STONE_SLAB, 2));
        assertSame(ItemType.DOUBLE_STONE_BRICK_SLAB, ItemType.fromBlock(BlockType.DOUBLE_STONE_SLAB, 5));
        assertEquals(97, ItemType.INFESTED_STONE.getId());
        assertEquals(97, ItemType.INFESTED_COBBLESTONE.getId());
        assertEquals(1, ItemType.INFESTED_COBBLESTONE.getDataValue());
        assertEquals(97, ItemType.INFESTED_STONE_BRICK.getId());
        assertEquals(2, ItemType.INFESTED_STONE_BRICK.getDataValue());
        assertSame(ItemType.INFESTED_STONE, ItemType.fromBlock(BlockType.INFESTED_STONE));
        assertSame(ItemType.INFESTED_COBBLESTONE, ItemType.fromBlock(BlockType.INFESTED_STONE, 1));
        assertSame(ItemType.INFESTED_STONE_BRICK, ItemType.fromBlock(BlockType.INFESTED_STONE, 2));
        assertEquals(6, ItemType.SPRUCE_SAPLING.getId());
        assertEquals(1, ItemType.SPRUCE_SAPLING.getDataValue());
        assertEquals(6, ItemType.BIRCH_SAPLING.getId());
        assertEquals(2, ItemType.BIRCH_SAPLING.getDataValue());
        assertEquals(31, ItemType.SHRUB.getId());
        assertEquals(0, ItemType.SHRUB.getDataValue());
        assertEquals(31, ItemType.TALL_GRASS.getId());
        assertEquals(1, ItemType.TALL_GRASS.getDataValue());
        assertEquals(31, ItemType.FERN.getId());
        assertEquals(2, ItemType.FERN.getDataValue());
        assertEquals(98, ItemType.MOSSY_STONE_BRICK.getId());
        assertEquals(1, ItemType.MOSSY_STONE_BRICK.getDataValue());
        assertEquals(98, ItemType.CRACKED_STONE_BRICK.getId());
        assertEquals(2, ItemType.CRACKED_STONE_BRICK.getDataValue());
        assertEquals(98, ItemType.CHISELED_STONE_BRICK.getId());
        assertEquals(3, ItemType.CHISELED_STONE_BRICK.getDataValue());
        assertEquals(323, ItemType.SIGN.getId());
        assertEquals(324, ItemType.WOODEN_DOOR.getId());
        assertEquals(330, ItemType.IRON_DOOR.getId());
        assertEquals(355, ItemType.BED.getId());
        assertEquals(261, ItemType.BOW.getId());
        assertEquals(318, ItemType.FLINT.getId());
        assertEquals(325, ItemType.BUCKET.getId());
        assertEquals(326, ItemType.WATER_BUCKET.getId());
        assertEquals(327, ItemType.LAVA_BUCKET.getId());
        assertEquals(16, ItemType.SIGN.getMaxStackSize());
        assertEquals(64, ItemType.GOLDEN_APPLE.getMaxStackSize());
        assertEquals(1, ItemType.WOODEN_DOOR.getMaxStackSize());
        assertEquals(1, ItemType.BED.getMaxStackSize());
        assertEquals(1, ItemType.BOW.getMaxStackSize());
        assertEquals(385, ItemType.BOW.getMaxDurability());
    }

    @Test
    @DisplayName("Post-Release 1.0 registry IDs should stay absent")
    void postReleaseOneRegistryIdsStayAbsent() {
        assertFalse(ItemType.hasId(383), "Spawn eggs were added after Java Release 1.0");
        assertFalse(ItemType.hasId(388), "Emeralds are post-Release 1.0");

        assertFalse(BlockType.hasId(123), "Redstone lamps are post-Release 1.0");
        assertFalse(BlockType.hasId(137), "Command blocks are post-Release 1.0");
        assertFalse(BlockType.hasId(145), "Anvils are post-Release 1.0");
        assertFalse(BlockType.hasId(146), "Trapped chests are post-Release 1.0");
        assertFalse(BlockType.hasId(149), "Comparators are post-Release 1.0");
        assertFalse(BlockType.hasId(150), "Comparators are post-Release 1.0");
        assertFalse(BlockType.hasId(154), "Hoppers are post-Release 1.0");
    }

    @Test
    @DisplayName("Block behavior categories should identify simulation-heavy blocks")
    void blockBehaviorCategoriesIdentifySimulationBlocks() {
        assertSame(BlockBehavior.FLUID, BlockBehavior.of(BlockType.WATER));
        assertSame(BlockBehavior.FALLING, BlockBehavior.of(BlockType.SAND));
        assertSame(BlockBehavior.REDSTONE_DUST, BlockBehavior.of(BlockType.REDSTONE_WIRE));
        assertSame(BlockBehavior.REDSTONE_POWER_SOURCE, BlockBehavior.of(BlockType.LEVER));
        assertSame(BlockBehavior.REDSTONE_REPEATER, BlockBehavior.of(BlockType.REDSTONE_REPEATER_ON));
        assertSame(BlockBehavior.RAIL, BlockBehavior.of(BlockType.POWERED_RAIL));
        assertSame(BlockBehavior.PISTON, BlockBehavior.of(BlockType.STICKY_PISTON));
        assertSame(BlockBehavior.CONTAINER, BlockBehavior.of(BlockType.BREWING_STAND));
        assertSame(BlockBehavior.PORTAL, BlockBehavior.of(BlockType.END_PORTAL_FRAME));
        assertSame(BlockBehavior.NORMAL, BlockBehavior.of(BlockType.SPONGE));
        assertSame(BlockBehavior.SPECIAL, BlockBehavior.of(BlockType.LOCKED_CHEST));
    }

    @Test
    @DisplayName("Render layers should separate cutout leaves from translucent blocks")
    void renderLayersSeparateCutoutAndTranslucentBlocks() {
        assertSame(BlockRenderLayer.CUTOUT, BlockType.LEAVES.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.FIRE.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.YELLOW_FLOWER.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.SAPLING.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.SNOW_LAYER.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.FLOWING_WATER.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.WATER.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.FLOWING_LAVA.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.LAVA.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.GLASS.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.ICE.getRenderLayer());
        assertSame(BlockRenderLayer.OPAQUE, BlockType.STONE.getRenderLayer());
        assertSame(BlockRenderLayer.OPAQUE, BlockType.SPONGE.getRenderLayer());
        assertSame(BlockRenderLayer.OPAQUE, BlockType.LOCKED_CHEST.getRenderLayer());
    }

    @Test
    @DisplayName("Release 1.0 shatterable light blocks should not behave as opaque cubes")
    void glowstoneUsesShatterableTransparency() {
        assertTrue(BlockType.GLOWSTONE.isSolid());
        assertTrue(BlockType.GLOWSTONE.isTransparent());
        assertFalse(BlockType.GLOWSTONE.occludesFace());
        assertFalse(BlockType.GLOWSTONE.blocksAmbientOcclusion());
    }

    @Test
    @DisplayName("Release 1.0 light-emitting blocks should use old block light levels")
    void lightEmissionMatchesReleaseOneLevels() {
        assertEquals(15, BlockType.FIRE.getLightEmission());
        assertEquals(15, BlockType.FLOWING_LAVA.getLightEmission());
        assertEquals(15, BlockType.LAVA.getLightEmission());
        assertEquals(15, BlockType.GLOWSTONE.getLightEmission());
        assertEquals(15, BlockType.JACK_O_LANTERN.getLightEmission());
        assertEquals(15, BlockType.END_PORTAL.getLightEmission());
        assertEquals(15, BlockType.LOCKED_CHEST.getLightEmission());
        assertEquals(14, BlockType.TORCH.getLightEmission());
        assertEquals(13, BlockType.LIT_FURNACE.getLightEmission());
        assertEquals(11, BlockType.PORTAL.getLightEmission());
        assertEquals(9, BlockType.GLOWING_REDSTONE_ORE.getLightEmission());
        assertEquals(9, BlockType.REDSTONE_REPEATER_ON.getLightEmission());
        assertEquals(7, BlockType.REDSTONE_TORCH_ON.getLightEmission());
        assertEquals(1, BlockType.BROWN_MUSHROOM.getLightEmission());
        assertEquals(1, BlockType.BREWING_STAND.getLightEmission());
        assertEquals(1, BlockType.END_PORTAL_FRAME.getLightEmission());
        assertEquals(1, BlockType.DRAGON_EGG.getLightEmission());

        assertEquals(0, BlockType.RED_MUSHROOM.getLightEmission());
        assertEquals(0, BlockType.REDSTONE_TORCH_OFF.getLightEmission());
        assertEquals(0, BlockType.REDSTONE_REPEATER_OFF.getLightEmission());
    }

    @Test
    @DisplayName("Block drops should return item identities, not block IDs")
    void blockDropsReturnItems() {
        assertSame(ItemType.COBBLESTONE, BlockType.STONE.getDroppedItem());
        assertSame(ItemType.DIRT, BlockType.GRASS.getDroppedItem());
        assertSame(ItemType.COAL, BlockType.COAL_ORE.getDroppedItem());
        assertSame(ItemType.DIAMOND, BlockType.DIAMOND_ORE.getDroppedItem());
        assertSame(ItemType.REDSTONE, BlockType.REDSTONE_ORE.getDroppedItem());
        assertSame(ItemType.LAPIS_LAZULI, BlockType.LAPIS_ORE.getDroppedItem());
        assertSame(ItemType.CHEST, BlockType.CHEST.getDroppedItem());
        assertSame(ItemType.FURNACE, BlockType.LIT_FURNACE.getDroppedItem());
        assertSame(ItemType.SIGN, BlockType.WALL_SIGN.getDroppedItem());
        assertSame(ItemType.WOODEN_DOOR, BlockType.WOODEN_DOOR.getDroppedItem());
        assertSame(ItemType.BED, BlockType.BED.getDroppedItem());
        assertSame(ItemType.OBSIDIAN, BlockType.OBSIDIAN.getDroppedItem());
        assertSame(ItemType.YELLOW_FLOWER, BlockType.YELLOW_FLOWER.getDroppedItem());
        assertSame(ItemType.OAK_PLANKS, BlockType.OAK_PLANKS.getDroppedItem());
        assertSame(ItemType.SPONGE, BlockType.SPONGE.getDroppedItem());
        assertSame(ItemType.LOCKED_CHEST, BlockType.LOCKED_CHEST.getDroppedItem());
        assertNull(BlockType.MOB_SPAWNER.getDroppedItem());
        assertNull(BlockType.INFESTED_STONE.getDroppedItem());
        assertNull(BlockType.AIR.getDroppedItem());
        assertNull(BlockType.BEDROCK.getDroppedItem());
        assertNull(BlockType.LEAVES.getDroppedItem());
        assertNull(BlockType.FIRE.getDroppedItem());
        assertNull(BlockType.END_PORTAL_FRAME.getDroppedItem());
    }
}
