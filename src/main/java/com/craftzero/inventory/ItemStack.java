package com.craftzero.inventory;

import com.craftzero.world.BlockType;

/**
 * Represents a stack of items in the inventory.
 * Supports durability for tools.
 */
public class ItemStack {
    private BlockType type;
    private int count;
    private int maxStackSize;
    private int durability; // Current durability (-1 = not a tool)
    private int maxDurability; // Max durability (-1 = not a tool)

    public ItemStack(BlockType type, int count) {
        this.type = type;
        this.count = count;

        // Tools don't stack and have durability
        if (type.isTool()) {
            this.maxStackSize = 1;
            ToolType toolType = type.getToolType();
            this.maxDurability = toolType.getMaxDurability();
            this.durability = this.maxDurability;
        } else {
            this.maxStackSize = 64;
            this.maxDurability = -1;
            this.durability = -1;
        }
    }

    /**
     * Create an ItemStack with specific durability (for tools).
     */
    public ItemStack(BlockType type, int count, int durability) {
        this(type, count);
        if (type.isTool()) {
            this.durability = durability;
        }
    }

    public BlockType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void add(int amount) {
        this.count += amount;
    }

    public void remove(int amount) {
        this.count -= amount;
        if (this.count < 0)
            this.count = 0;
    }

    public boolean isEmpty() {
        return count <= 0 || type == null;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    // ===== Tool/Durability Methods =====

    public boolean isTool() {
        return type != null && type.isTool();
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    /**
     * Use durability (reduces by 1).
     * 
     * @return true if tool broke (durability reached 0)
     */
    public boolean useDurability() {
        if (durability > 0) {
            durability--;
            return durability <= 0;
        }
        return false;
    }

    /**
     * Get durability as a percentage (0.0 - 1.0).
     */
    public float getDurabilityPercent() {
        if (maxDurability <= 0)
            return 1.0f;
        return (float) durability / (float) maxDurability;
    }
}
