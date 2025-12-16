package com.craftzero.world;

import com.craftzero.inventory.ToolType;

/**
 * Enum defining all block types with their properties.
 * Each block has an ID, solidity, transparency, hardness, texture indices,
 * preferred tool, and harvest level requirements.
 */
public enum BlockType {

    // ID, Solid, Transparent, Hardness, Top, Bottom, Side, PreferredTool,
    // HarvestLevel
    // Hardness based on Minecraft wiki - time to break by hand in seconds
    // HarvestLevel: 0=hand, 1=wood, 2=stone, 3=iron, 4=diamond
    AIR(0, false, true, 0f, -1, -1, -1, ToolType.Category.NONE, 0),
    GRASS(1, true, false, 0.9f, 0, 2, 1, ToolType.Category.SHOVEL, 0),
    DIRT(2, true, false, 0.75f, 2, 2, 2, ToolType.Category.SHOVEL, 0),
    STONE(3, true, false, 3.0f, 3, 3, 3, ToolType.Category.PICKAXE, 1), // Requires wood pickaxe
    COBBLESTONE(4, true, false, 3.5f, 4, 4, 4, ToolType.Category.PICKAXE, 1),
    BEDROCK(5, true, false, -1f, 5, 5, 5, ToolType.Category.NONE, 99), // Unbreakable
    SAND(6, true, false, 0.5f, 6, 6, 6, ToolType.Category.SHOVEL, 0),
    GRAVEL(7, true, false, 0.6f, 7, 7, 7, ToolType.Category.SHOVEL, 0),
    OAK_LOG(8, true, false, 2.0f, 8, 8, 9, ToolType.Category.AXE, 0),
    OAK_PLANKS(9, true, false, 2.0f, 10, 10, 10, ToolType.Category.AXE, 0),
    LEAVES(10, true, false, 0.2f, 11, 11, 11, ToolType.Category.NONE, 0),
    GLASS(11, true, true, 0.3f, 12, 12, 12, ToolType.Category.NONE, 0),
    WATER(12, false, true, 0f, 13, 13, 13, ToolType.Category.NONE, 0),
    BRICK(13, true, false, 2.0f, 14, 14, 14, ToolType.Category.PICKAXE, 1),
    COAL_ORE(14, true, false, 4.5f, 15, 15, 15, ToolType.Category.PICKAXE, 1), // Wood pickaxe
    IRON_ORE(15, true, false, 5.0f, 16, 16, 16, ToolType.Category.PICKAXE, 2), // Stone pickaxe
    GOLD_ORE(16, true, false, 5.0f, 17, 17, 17, ToolType.Category.PICKAXE, 3), // Iron pickaxe
    DIAMOND_ORE(17, true, false, 6.0f, 18, 18, 18, ToolType.Category.PICKAXE, 3), // Iron pickaxe
    SNOW(18, true, false, 0.1f, 19, 2, 20, ToolType.Category.SHOVEL, 0),
    ICE(19, true, true, 0.5f, 21, 21, 21, ToolType.Category.PICKAXE, 0),
    STICK(20, false, true, 0f, 22, 22, 22, ToolType.Category.NONE, 0),
    CRAFTING_TABLE(21, true, false, 2.5f, 23, 10, 24, ToolType.Category.AXE, 0),

    // Tools - all are items (not placeable)
    WOODEN_PICKAXE(22, false, true, 0f, 25, 25, 25, ToolType.Category.NONE, 0),
    STONE_PICKAXE(23, false, true, 0f, 26, 26, 26, ToolType.Category.NONE, 0),
    IRON_PICKAXE(24, false, true, 0f, 27, 27, 27, ToolType.Category.NONE, 0),
    DIAMOND_PICKAXE(25, false, true, 0f, 28, 28, 28, ToolType.Category.NONE, 0),
    WOODEN_SHOVEL(26, false, true, 0f, 29, 29, 29, ToolType.Category.NONE, 0),
    STONE_SHOVEL(27, false, true, 0f, 30, 30, 30, ToolType.Category.NONE, 0),
    IRON_SHOVEL(28, false, true, 0f, 31, 31, 31, ToolType.Category.NONE, 0),
    DIAMOND_SHOVEL(29, false, true, 0f, 32, 32, 32, ToolType.Category.NONE, 0),
    WOODEN_AXE(30, false, true, 0f, 33, 33, 33, ToolType.Category.NONE, 0),
    STONE_AXE(31, false, true, 0f, 34, 34, 34, ToolType.Category.NONE, 0),
    IRON_AXE(32, false, true, 0f, 35, 35, 35, ToolType.Category.NONE, 0),
    DIAMOND_AXE(33, false, true, 0f, 36, 36, 36, ToolType.Category.NONE, 0);

    private final int id;
    private final boolean solid;
    private final boolean transparent;
    private final float hardness;
    private final int topTexture;
    private final int bottomTexture;
    private final int sideTexture;
    private final ToolType.Category preferredTool;
    private final int harvestLevel;

    // Atlas configuration: 16x16 grid
    public static final int ATLAS_SIZE = 16;
    public static final float TEXTURE_SIZE = 1.0f / ATLAS_SIZE;

    BlockType(int id, boolean solid, boolean transparent, float hardness,
            int topTexture, int bottomTexture, int sideTexture,
            ToolType.Category preferredTool, int harvestLevel) {
        this.id = id;
        this.solid = solid;
        this.transparent = transparent;
        this.hardness = hardness;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.sideTexture = sideTexture;
        this.preferredTool = preferredTool;
        this.harvestLevel = harvestLevel;
    }

    public BlockType getDroppedItem() {
        switch (this) {
            case GRASS:
                return DIRT;
            case STONE:
                return COBBLESTONE;
            default:
                return this;
        }
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    /**
     * Returns true if this is an item (not placeable as a block).
     * Items render as 2D sprites when dropped, not 3D cubes.
     */
    public boolean isItem() {
        return this == STICK || isTool();
    }

    /**
     * Check if this is a tool item.
     */
    public boolean isTool() {
        return this == WOODEN_PICKAXE || this == STONE_PICKAXE || this == IRON_PICKAXE || this == DIAMOND_PICKAXE ||
                this == WOODEN_SHOVEL || this == STONE_SHOVEL || this == IRON_SHOVEL || this == DIAMOND_SHOVEL ||
                this == WOODEN_AXE || this == STONE_AXE || this == IRON_AXE || this == DIAMOND_AXE;
    }

    /**
     * Get the ToolType for this item, or NONE if not a tool.
     */
    public ToolType getToolType() {
        switch (this) {
            case WOODEN_PICKAXE:
                return ToolType.WOODEN_PICKAXE;
            case STONE_PICKAXE:
                return ToolType.STONE_PICKAXE;
            case IRON_PICKAXE:
                return ToolType.IRON_PICKAXE;
            case DIAMOND_PICKAXE:
                return ToolType.DIAMOND_PICKAXE;
            case WOODEN_SHOVEL:
                return ToolType.WOODEN_SHOVEL;
            case STONE_SHOVEL:
                return ToolType.STONE_SHOVEL;
            case IRON_SHOVEL:
                return ToolType.IRON_SHOVEL;
            case DIAMOND_SHOVEL:
                return ToolType.DIAMOND_SHOVEL;
            case WOODEN_AXE:
                return ToolType.WOODEN_AXE;
            case STONE_AXE:
                return ToolType.STONE_AXE;
            case IRON_AXE:
                return ToolType.IRON_AXE;
            case DIAMOND_AXE:
                return ToolType.DIAMOND_AXE;
            default:
                return ToolType.NONE;
        }
    }

    public ToolType.Category getPreferredTool() {
        return preferredTool;
    }

    public int getHarvestLevel() {
        return harvestLevel;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isAir() {
        return this == AIR;
    }

    /**
     * Get the hardness value (time to break by hand in seconds).
     * 
     * @return hardness value, or -1 if unbreakable
     */
    public float getHardness() {
        return hardness;
    }

    /**
     * Check if this block is breakable.
     * 
     * @return true if the block can be broken
     */
    public boolean isBreakable() {
        return hardness >= 0 && this != AIR && this != WATER;
    }

    /**
     * Get texture UV coordinates for a specific face.
     * 
     * @param face 0=top, 1=bottom, 2-5=sides (north, south, east, west)
     * @return float[4] = {u1, v1, u2, v2}
     */
    public float[] getTextureCoords(int face) {
        int textureIndex;
        if (face == 0) {
            textureIndex = topTexture;
        } else if (face == 1) {
            textureIndex = bottomTexture;
        } else {
            textureIndex = sideTexture;
        }

        if (textureIndex < 0) {
            return new float[] { 0, 0, 0, 0 };
        }

        int row = textureIndex / ATLAS_SIZE;
        int col = textureIndex % ATLAS_SIZE;

        float u1 = col * TEXTURE_SIZE;
        float v1 = row * TEXTURE_SIZE; // Top of sprite (V=0 in non-flipped texture)
        float u2 = u1 + TEXTURE_SIZE;
        float v2 = v1 + TEXTURE_SIZE; // Bottom of sprite

        // Inset slightly to avoid texture bleeding from neighboring sprites
        float inset = 0.001f;
        u1 += inset;
        v1 += inset;
        u2 -= inset;
        v2 -= inset;

        return new float[] { u1, v1, u2, v2 };
    }

    /**
     * Get BlockType by ID.
     */
    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return AIR;
    }

    /**
     * Check if this block occludes the given face of an adjacent block.
     * Transparent blocks don't occlude other blocks.
     */
    public boolean occludesFace() {
        return solid && !transparent;
    }
}
