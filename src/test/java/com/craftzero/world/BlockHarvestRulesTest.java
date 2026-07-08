package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockHarvestRulesTest {
    @Test
    @DisplayName("Release 1.0 pickaxe harvest tiers gate special ores and storage blocks")
    void releaseOnePickaxeHarvestTiersGateSpecialOresAndStorageBlocks() {
        assertTrue(BlockHarvestRules.canHarvest(BlockType.STONE, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.COAL_ORE, ItemType.WOODEN_PICKAXE));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.IRON_ORE, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.IRON_ORE, ItemType.STONE_PICKAXE));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.IRON_BLOCK, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.IRON_BLOCK, ItemType.STONE_PICKAXE));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.LAPIS_ORE, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.LAPIS_ORE, ItemType.STONE_PICKAXE));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.LAPIS_BLOCK, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.LAPIS_BLOCK, ItemType.STONE_PICKAXE));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.GOLD_ORE, ItemType.STONE_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.GOLD_ORE, ItemType.IRON_PICKAXE));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.GOLD_BLOCK, ItemType.STONE_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.GOLD_BLOCK, ItemType.IRON_PICKAXE));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.DIAMOND_ORE, ItemType.STONE_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.DIAMOND_ORE, ItemType.IRON_PICKAXE));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.DIAMOND_BLOCK, ItemType.STONE_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.DIAMOND_BLOCK, ItemType.IRON_PICKAXE));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.REDSTONE_ORE, ItemType.STONE_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.REDSTONE_ORE, ItemType.IRON_PICKAXE));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.GLOWING_REDSTONE_ORE, ItemType.STONE_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.GLOWING_REDSTONE_ORE, ItemType.IRON_PICKAXE));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.OBSIDIAN, ItemType.IRON_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.OBSIDIAN, ItemType.DIAMOND_PICKAXE));
    }

    @Test
    @DisplayName("Ordinary rock blocks only require the pickaxe family in Release 1.0")
    void ordinaryRockBlocksOnlyRequirePickaxeFamily() {
        assertFalse(BlockHarvestRules.canHarvest(BlockType.NETHER_BRICK, null));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.NETHER_BRICK, ItemType.WOODEN_AXE));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.NETHERRACK, null));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.NETHER_BRICK, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.NETHER_BRICK_FENCE, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.NETHER_BRICK_STAIRS, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.NETHERRACK, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.IRON_DOOR, ItemType.WOODEN_PICKAXE));
    }

    @Test
    @DisplayName("Release 1.0 stone controls should require any pickaxe for drops")
    void stoneControlsRequirePickaxeHarvest() {
        for (BlockType blockType : List.of(BlockType.STONE_PRESSURE_PLATE, BlockType.STONE_BUTTON)) {
            assertFalse(BlockHarvestRules.canHarvest(blockType, null), blockType.name());
            assertFalse(BlockHarvestRules.canHarvest(blockType, ItemType.WOODEN_AXE), blockType.name());
            assertTrue(BlockHarvestRules.canHarvest(blockType, ItemType.WOODEN_PICKAXE), blockType.name());
        }

        assertTrue(BlockHarvestRules.canHarvest(BlockType.WOODEN_PRESSURE_PLATE, null));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.LEVER, null));
    }

    @Test
    @DisplayName("Release 1.0 utility blocks should require any pickaxe for drops")
    void utilityBlocksRequirePickaxeHarvest() {
        for (BlockType blockType : List.of(BlockType.ENCHANTING_TABLE, BlockType.BREWING_STAND, BlockType.CAULDRON)) {
            assertFalse(BlockHarvestRules.canHarvest(blockType, null), blockType.name());
            assertFalse(BlockHarvestRules.canHarvest(blockType, ItemType.WOODEN_AXE), blockType.name());
            assertTrue(BlockHarvestRules.canHarvest(blockType, ItemType.WOODEN_PICKAXE), blockType.name());
        }
    }

    @Test
    @DisplayName("Cobweb harvest still requires swords or shears")
    void cobwebHarvestRequiresSwordOrShears() {
        assertFalse(BlockHarvestRules.canHarvest(BlockType.COBWEB, null));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.COBWEB, ItemType.IRON_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.COBWEB, ItemType.WOODEN_SWORD));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.COBWEB, ItemType.SHEARS));
    }

    @Test
    @DisplayName("Snowball drops should require shovels despite zero harvest level")
    void snowballDropsRequireShovelHarvest() {
        assertFalse(BlockHarvestRules.canHarvest(BlockType.SNOW_LAYER, null));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.SNOW_LAYER, ItemType.WOODEN_PICKAXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.SNOW_LAYER, ItemType.WOODEN_SHOVEL));

        assertFalse(BlockHarvestRules.canHarvest(BlockType.SNOW, null));
        assertFalse(BlockHarvestRules.canHarvest(BlockType.SNOW, ItemType.WOODEN_AXE));
        assertTrue(BlockHarvestRules.canHarvest(BlockType.SNOW, ItemType.STONE_SHOVEL));
    }

}
