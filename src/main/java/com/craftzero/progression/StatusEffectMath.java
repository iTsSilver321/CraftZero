package com.craftzero.progression;

/**
 * Shared Release-era status effect formulas used by gameplay systems.
 */
public final class StatusEffectMath {
    private StatusEffectMath() {
    }

    public static float applyResistanceReduction(float damage, int amplifier) {
        if (damage <= 0.0f) {
            return 0.0f;
        }
        if (amplifier < 0) {
            return damage;
        }
        float multiplier = Math.max(0.0f, 1.0f - 0.2f * (amplifier + 1));
        return damage * multiplier;
    }
}
