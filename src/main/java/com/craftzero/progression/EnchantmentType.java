package com.craftzero.progression;

public enum EnchantmentType {
    PROTECTION(0),
    FIRE_PROTECTION(1),
    FEATHER_FALLING(2),
    BLAST_PROTECTION(3),
    PROJECTILE_PROTECTION(4),
    RESPIRATION(5),
    AQUA_AFFINITY(6),
    SHARPNESS(16),
    SMITE(17),
    BANE_OF_ARTHROPODS(18),
    KNOCKBACK(19),
    FIRE_ASPECT(20),
    LOOTING(21),
    EFFICIENCY(32),
    SILK_TOUCH(33),
    UNBREAKING(34),
    FORTUNE(35),
    POWER(48),
    PUNCH(49),
    FLAME(50),
    INFINITY(51);

    private final int id;

    EnchantmentType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
