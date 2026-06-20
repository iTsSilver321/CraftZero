package com.craftzero.inventory;

import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.PotionData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a stack of items in the inventory.
 * Supports durability plus Release 1.0-era structured metadata such as
 * enchantments and potion identity.
 */
public class ItemStack {
    private ItemType type;
    private int count;
    private int maxStackSize;
    private int durability; // Current durability (-1 = not a tool)
    private int maxDurability; // Max durability (-1 = not a tool)
    private String customName;
    private final List<EnchantmentInstance> enchantments;
    private PotionData potionData;
    private final Map<String, String> metadata;

    public ItemStack(ItemType type, int count) {
        this.type = type;
        this.count = count;
        this.enchantments = new ArrayList<>();
        this.metadata = new LinkedHashMap<>();

        // Damageable items don't stack and have durability
        if (type != null && type.isDamageable()) {
            this.maxStackSize = 1;
            this.maxDurability = type.getMaxDurability();
            this.durability = this.maxDurability;
        } else {
            this.maxStackSize = type != null ? type.getMaxStackSize() : 64;
            this.maxDurability = -1;
            this.durability = -1;
        }
    }

    /**
     * Create an ItemStack with specific durability (for tools).
     */
    public ItemStack(ItemType type, int count, int durability) {
        this(type, count);
        if (type != null && type.isDamageable()) {
            this.durability = durability;
        }
    }

    public ItemType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
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

    // ===== Tool/Durability Methods =====

    public boolean isTool() {
        return type != null && type.isTool();
    }

    public boolean isDamageable() {
        return type != null && type.isDamageable();
    }

    public ItemStack copy() {
        ItemStack copy = new ItemStack(type, count, durability);
        copy.customName = customName;
        copy.enchantments.addAll(enchantments);
        copy.potionData = potionData;
        copy.metadata.putAll(metadata);
        return copy;
    }

    public String getCustomName() {
        return customName;
    }

    public List<EnchantmentInstance> getEnchantments() {
        return Collections.unmodifiableList(enchantments);
    }

    public void setEnchantments(Collection<EnchantmentInstance> values) {
        enchantments.clear();
        if (values != null) {
            for (EnchantmentInstance value : values) {
                if (value != null) {
                    enchantments.add(value);
                }
            }
        }
    }

    public void addEnchantment(EnchantmentInstance enchantment) {
        if (enchantment != null) {
            enchantments.add(enchantment);
        }
    }

    public PotionData getPotionData() {
        return potionData;
    }

    public void setPotionData(PotionData potionData) {
        this.potionData = potionData;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void setMetadata(Map<String, String> values) {
        metadata.clear();
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    metadata.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void putMetadata(String key, String value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
    }

    public boolean canMergeWith(ItemStack other) {
        if (other == null) {
            return false;
        }
        return type == other.type
                && durability == other.durability
                && Objects.equals(customName, other.customName)
                && Objects.equals(enchantments, other.enchantments)
                && Objects.equals(potionData, other.potionData)
                && Objects.equals(metadata, other.metadata);
    }

    /**
     * Use durability (reduces by 1).
     * 
     * @return true if the item broke (durability reached 0)
     */
    public boolean useDurability() {
        if (durability > 0) {
            durability--;
            return durability <= 0;
        }
        return false;
    }

    public void setDurability(int durability) {
        if (!isDamageable()) {
            this.durability = -1;
            return;
        }
        this.durability = Math.max(0, Math.min(maxDurability, durability));
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
