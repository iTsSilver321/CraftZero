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
        assertSame(ItemType.CACTUS_GREEN, ItemType.fromId(351, 2));
        assertSame(ItemType.LAPIS_LAZULI, ItemType.fromId(351, 4));
    }

    @Test
    @DisplayName("Block items should map back to their placed blocks")
    void blockItemsMapToBlocks() {
        for (ItemType type : ItemType.values()) {
            if (!type.isBlockItem()) {
                continue;
            }

            assertSame(type, ItemType.fromBlock(type.getPlacedBlock()));
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

        assertEquals(323, ItemType.SIGN.getId());
        assertEquals(324, ItemType.WOODEN_DOOR.getId());
        assertEquals(330, ItemType.IRON_DOOR.getId());
        assertEquals(355, ItemType.BED.getId());
        assertEquals(261, ItemType.BOW.getId());
        assertEquals(318, ItemType.FLINT.getId());
        assertEquals(325, ItemType.BUCKET.getId());
        assertEquals(326, ItemType.WATER_BUCKET.getId());
        assertEquals(327, ItemType.LAVA_BUCKET.getId());
        assertEquals(1, ItemType.SIGN.getMaxStackSize());
        assertEquals(1, ItemType.WOODEN_DOOR.getMaxStackSize());
        assertEquals(1, ItemType.BED.getMaxStackSize());
        assertEquals(1, ItemType.BOW.getMaxStackSize());
        assertEquals(385, ItemType.BOW.getMaxDurability());
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
    }

    @Test
    @DisplayName("Render layers should separate cutout leaves from translucent blocks")
    void renderLayersSeparateCutoutAndTranslucentBlocks() {
        assertSame(BlockRenderLayer.CUTOUT, BlockType.LEAVES.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.FIRE.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.YELLOW_FLOWER.getRenderLayer());
        assertSame(BlockRenderLayer.CUTOUT, BlockType.SAPLING.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.FLOWING_WATER.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.WATER.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.FLOWING_LAVA.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.LAVA.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.GLASS.getRenderLayer());
        assertSame(BlockRenderLayer.TRANSLUCENT, BlockType.ICE.getRenderLayer());
        assertSame(BlockRenderLayer.OPAQUE, BlockType.STONE.getRenderLayer());
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
        assertNull(BlockType.AIR.getDroppedItem());
        assertNull(BlockType.LEAVES.getDroppedItem());
        assertNull(BlockType.FIRE.getDroppedItem());
    }
}
