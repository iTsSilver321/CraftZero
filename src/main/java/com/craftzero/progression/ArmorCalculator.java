package com.craftzero.progression;

import com.craftzero.combat.DamageSource;
import com.craftzero.inventory.ItemStack;

/**
 * Release 1.0-style armor and protection-enchantment damage math.
 */
public final class ArmorCalculator {
    private ArmorCalculator() {
    }

    public static int armorPoints(ItemStack[] armor) {
        if (armor == null) {
            return 0;
        }
        int total = 0;
        ArmorSlot[] slots = ArmorSlot.values();
        for (int i = 0; i < armor.length && i < slots.length; i++) {
            ItemStack stack = armor[i];
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ArmorMaterial material = ArmorMaterial.materialOf(stack.getType());
            if (material != null) {
                total += material.getProtection(slots[i]);
            }
        }
        return total;
    }

    public static float reduceDamage(float damage, ItemStack[] armor, DamageSource source) {
        if (damage <= 0.0f) {
            return 0.0f;
        }
        float armorReduction = Math.min(0.8f, armorPoints(armor) * 0.04f);
        float afterArmor = damage * (1.0f - armorReduction);
        int epf = protectionFactor(armor, source);
        if (epf <= 0) {
            return afterArmor;
        }
        float enchantReduction = Math.min(0.8f, epf * 0.04f);
        return afterArmor * (1.0f - enchantReduction);
    }

    public static int protectionFactor(ItemStack[] armor, DamageSource source) {
        if (armor == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : armor) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            total += EnchantmentResolver.getLevel(stack, EnchantmentType.PROTECTION);
            if (source != null) {
                total += switch (source.type()) {
                    case FIRE -> EnchantmentResolver.getLevel(stack, EnchantmentType.FIRE_PROTECTION) * 2;
                    case EXPLOSION -> EnchantmentResolver.getLevel(stack, EnchantmentType.BLAST_PROTECTION) * 2;
                    case ARROW -> EnchantmentResolver.getLevel(stack, EnchantmentType.PROJECTILE_PROTECTION) * 2;
                    default -> 0;
                };
            }
        }
        return Math.min(25, total);
    }
}
