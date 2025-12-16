package com.craftzero.inventory;

/**
 * Defines tool types and their tiers with properties for mining.
 */
public enum ToolType {
    // Format: category, tier, speedMultiplier, durability, miningLevel

    // No tool (hand)
    NONE(Category.NONE, Tier.NONE, 1.0f, 0, 0),

    // Pickaxes - mine stone, ores
    WOODEN_PICKAXE(Category.PICKAXE, Tier.WOOD, 2.0f, 59, 1),
    STONE_PICKAXE(Category.PICKAXE, Tier.STONE, 4.0f, 131, 2),
    IRON_PICKAXE(Category.PICKAXE, Tier.IRON, 6.0f, 250, 3),
    DIAMOND_PICKAXE(Category.PICKAXE, Tier.DIAMOND, 8.0f, 1561, 4),

    // Shovels - mine dirt, sand, gravel, snow
    WOODEN_SHOVEL(Category.SHOVEL, Tier.WOOD, 2.0f, 59, 1),
    STONE_SHOVEL(Category.SHOVEL, Tier.STONE, 4.0f, 131, 2),
    IRON_SHOVEL(Category.SHOVEL, Tier.IRON, 6.0f, 250, 3),
    DIAMOND_SHOVEL(Category.SHOVEL, Tier.DIAMOND, 8.0f, 1561, 4),

    // Axes - mine wood, planks
    WOODEN_AXE(Category.AXE, Tier.WOOD, 2.0f, 59, 1),
    STONE_AXE(Category.AXE, Tier.STONE, 4.0f, 131, 2),
    IRON_AXE(Category.AXE, Tier.IRON, 6.0f, 250, 3),
    DIAMOND_AXE(Category.AXE, Tier.DIAMOND, 8.0f, 1561, 4);

    public enum Category {
        NONE,
        PICKAXE, // Stone, ores, brick
        SHOVEL, // Dirt, sand, gravel, snow
        AXE // Wood, planks
    }

    public enum Tier {
        NONE(0),
        WOOD(1),
        STONE(2),
        IRON(3),
        DIAMOND(4);

        private final int level;

        Tier(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    private final Category category;
    private final Tier tier;
    private final float speedMultiplier;
    private final int maxDurability;
    private final int miningLevel;

    ToolType(Category category, Tier tier, float speedMultiplier, int maxDurability, int miningLevel) {
        this.category = category;
        this.tier = tier;
        this.speedMultiplier = speedMultiplier;
        this.maxDurability = maxDurability;
        this.miningLevel = miningLevel;
    }

    public Category getCategory() {
        return category;
    }

    public Tier getTier() {
        return tier;
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getMiningLevel() {
        return miningLevel;
    }

    /**
     * Check if this tool is effective against a given block category.
     */
    public boolean isEffectiveAgainst(Category blockPreferredTool) {
        return this.category == blockPreferredTool && this.category != Category.NONE;
    }
}
