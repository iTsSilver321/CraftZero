package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.ToolType;

/**
 * Player harvest rules for Release 1.0 block drops.
 */
public final class BlockHarvestRules {
    private BlockHarvestRules() {
    }

    public static boolean canHarvest(BlockType blockType, ItemType heldType) {
        ToolType toolType = heldType == null ? ToolType.NONE : heldType.getToolType();
        return canHarvest(blockType, heldType, toolType);
    }

    public static boolean canHarvest(BlockType blockType, ItemType heldType, ToolType toolType) {
        if (blockType == null) {
            return false;
        }
        if (blockType == BlockType.COBWEB) {
            return heldType == ItemType.SHEARS || isSword(heldType);
        }
        if (blockType == BlockType.SNOW || blockType == BlockType.SNOW_LAYER) {
            ToolType effectiveTool = toolType == null ? ToolType.NONE : toolType;
            return effectiveTool.getCategory() == ToolType.Category.SHOVEL;
        }
        if (blockType.getHarvestLevel() <= 0) {
            return true;
        }
        ToolType effectiveTool = toolType == null ? ToolType.NONE : toolType;
        return effectiveTool.isEffectiveAgainst(blockType.getPreferredTool())
                && effectiveTool.getMiningLevel() >= blockType.getHarvestLevel();
    }

    private static boolean isSword(ItemType type) {
        return type != null
                && type.isTool()
                && type.getToolType().getCategory() == ToolType.Category.SWORD;
    }
}
