package com.craftzero.progression;

public record EnchantmentInstance(EnchantmentType type, int level) {
    public EnchantmentInstance {
        if (type == null) {
            throw new IllegalArgumentException("enchantment type cannot be null");
        }
        level = Math.max(1, level);
    }
}
