package com.craftzero.inventory;

/**
 * Defines tool types and their tiers with properties for mining and combat.
 */
public enum ToolType {
    // Format: category, tier, speedMultiplier, durability, miningLevel,
    // attackDamage

    // No tool (hand) - 1 damage (0.5 hearts)
    NONE(Category.NONE, Tier.NONE, 1.0f, 0, 0, 1.0f),

    // Pickaxes - mine stone, ores
    WOODEN_PICKAXE(Category.PICKAXE, Tier.WOOD, 2.0f, 59, 1, 2.0f),
    STONE_PICKAXE(Category.PICKAXE, Tier.STONE, 4.0f, 131, 2, 3.0f),
    IRON_PICKAXE(Category.PICKAXE, Tier.IRON, 6.0f, 250, 3, 4.0f),
    DIAMOND_PICKAXE(Category.PICKAXE, Tier.DIAMOND, 8.0f, 1561, 4, 5.0f),
    GOLD_PICKAXE(Category.PICKAXE, Tier.GOLD, 12.0f, 32, 1, 2.0f),

    // Shovels - mine dirt, sand, gravel, snow
    WOODEN_SHOVEL(Category.SHOVEL, Tier.WOOD, 2.0f, 59, 1, 1.0f),
    STONE_SHOVEL(Category.SHOVEL, Tier.STONE, 4.0f, 131, 2, 2.0f),
    IRON_SHOVEL(Category.SHOVEL, Tier.IRON, 6.0f, 250, 3, 3.0f),
    DIAMOND_SHOVEL(Category.SHOVEL, Tier.DIAMOND, 8.0f, 1561, 4, 4.0f),
    GOLD_SHOVEL(Category.SHOVEL, Tier.GOLD, 12.0f, 32, 1, 1.0f),

    // Axes - mine wood, planks (high damage in Minecraft 1.9+, but we use pre-1.9
    // values)
    WOODEN_AXE(Category.AXE, Tier.WOOD, 2.0f, 59, 1, 3.0f),
    STONE_AXE(Category.AXE, Tier.STONE, 4.0f, 131, 2, 4.0f),
    IRON_AXE(Category.AXE, Tier.IRON, 6.0f, 250, 3, 5.0f),
    DIAMOND_AXE(Category.AXE, Tier.DIAMOND, 8.0f, 1561, 4, 6.0f),
    GOLD_AXE(Category.AXE, Tier.GOLD, 12.0f, 32, 1, 3.0f),

    // Swords - primary combat weapons (Minecraft pre-1.9 damage values)
    WOODEN_SWORD(Category.SWORD, Tier.WOOD, 1.0f, 59, 0, 4.0f),
    STONE_SWORD(Category.SWORD, Tier.STONE, 1.0f, 131, 0, 5.0f),
    IRON_SWORD(Category.SWORD, Tier.IRON, 1.0f, 250, 0, 6.0f),
    DIAMOND_SWORD(Category.SWORD, Tier.DIAMOND, 1.0f, 1561, 0, 7.0f),
    GOLD_SWORD(Category.SWORD, Tier.GOLD, 1.0f, 32, 0, 4.0f),

    // Hoes till dirt/grass into farmland. They are durability items but not a
    // mining speed category in Release 1.0.
    WOODEN_HOE(Category.HOE, Tier.WOOD, 1.0f, 59, 0, 1.0f),
    STONE_HOE(Category.HOE, Tier.STONE, 1.0f, 131, 0, 1.0f),
    IRON_HOE(Category.HOE, Tier.IRON, 1.0f, 250, 0, 1.0f),
    DIAMOND_HOE(Category.HOE, Tier.DIAMOND, 1.0f, 1561, 0, 1.0f),
    GOLD_HOE(Category.HOE, Tier.GOLD, 1.0f, 32, 0, 1.0f);

    public enum Category {
        NONE,
        PICKAXE, // Stone, ores, brick
        SHOVEL, // Dirt, sand, gravel, snow
        AXE, // Wood, planks
        SWORD, // Combat weapon
        HOE
    }

    public enum Tier {
        NONE(0),
        WOOD(1),
        STONE(2),
        IRON(3),
        DIAMOND(4),
        GOLD(1);

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
    private final float attackDamage;

    ToolType(Category category, Tier tier, float speedMultiplier, int maxDurability, int miningLevel,
            float attackDamage) {
        this.category = category;
        this.tier = tier;
        this.speedMultiplier = speedMultiplier;
        this.maxDurability = maxDurability;
        this.miningLevel = miningLevel;
        this.attackDamage = attackDamage;
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
     * Get the attack damage this tool deals.
     * 
     * @return damage value (1.0 = 0.5 hearts)
     */
    public float getAttackDamage() {
        return attackDamage;
    }

    /**
     * Check if this tool is effective against a given block category.
     */
    public boolean isEffectiveAgainst(Category blockPreferredTool) {
        return this.category == blockPreferredTool && this.category != Category.NONE;
    }
}
