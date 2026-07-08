package com.craftzero.progression;

import com.craftzero.inventory.ItemType;

public enum ArmorMaterial {
    LEATHER(5, new int[] { 1, 3, 2, 1 }),
    CHAIN(15, new int[] { 2, 5, 4, 1 }),
    IRON(15, new int[] { 2, 6, 5, 2 }),
    DIAMOND(33, new int[] { 3, 8, 6, 3 }),
    GOLD(7, new int[] { 2, 5, 3, 1 });

    private final int enchantability;
    private final int[] protectionBySlot;

    ArmorMaterial(int enchantability, int[] protectionBySlot) {
        this.enchantability = enchantability;
        this.protectionBySlot = protectionBySlot;
    }

    public int getEnchantability() {
        return enchantability;
    }

    public int getProtection(ArmorSlot slot) {
        return protectionBySlot[slot.getIndex()];
    }

    public static ArmorMaterial materialOf(ItemType type) {
        if (type == null) {
            return null;
        }
        String name = type.name();
        if (!isArmorPieceName(name)) {
            return null;
        }
        if (name.startsWith("LEATHER_")) {
            return LEATHER;
        }
        if (name.startsWith("CHAIN_")) {
            return CHAIN;
        }
        if (name.startsWith("IRON_")) {
            return IRON;
        }
        if (name.startsWith("DIAMOND_")) {
            return DIAMOND;
        }
        if (name.startsWith("GOLD_")) {
            return GOLD;
        }
        return null;
    }

    public static ArmorSlot slotOf(ItemType type) {
        if (type == null) {
            return null;
        }
        String name = type.name();
        if (!isArmorPieceName(name) || materialOf(type) == null) {
            return null;
        }
        if (name.endsWith("_HELMET")) {
            return ArmorSlot.HELMET;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return ArmorSlot.CHESTPLATE;
        }
        if (name.endsWith("_LEGGINGS")) {
            return ArmorSlot.LEGGINGS;
        }
        if (name.endsWith("_BOOTS")) {
            return ArmorSlot.BOOTS;
        }
        return null;
    }

    private static boolean isArmorPieceName(String name) {
        return name != null && (name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS"));
    }
}
