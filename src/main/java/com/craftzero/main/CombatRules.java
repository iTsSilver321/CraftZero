package com.craftzero.main;

/**
 * Shared Release 1.0 Easy-difficulty combat values.
 */
public final class CombatRules {
    public static final int PLAYER_HURT_INVULNERABILITY_TICKS = 20;

    public static final float EASY_ZOMBIE_DAMAGE = 2.0f;
    public static final float EASY_SPIDER_DAMAGE = 2.0f;
    public static final float EASY_SKELETON_ARROW_DAMAGE = 2.0f;
    public static final float EASY_ENDERMAN_DAMAGE = 4.0f;

    public static final float PLAYER_ATTACK_KNOCKBACK = 0.4f;
    public static final float PLAYER_ATTACK_SPRINT_BONUS = 0.4f;
    public static final float PLAYER_ATTACK_VERTICAL_KNOCKBACK = 0.25f;

    public static final float MOB_MELEE_HORIZONTAL_KNOCKBACK = 0.4f;
    public static final float MOB_MELEE_VERTICAL_KNOCKBACK = 0.3f;
    public static final float ARROW_HORIZONTAL_KNOCKBACK = 0.35f;
    public static final float ARROW_VERTICAL_KNOCKBACK = 0.12f;

    private CombatRules() {
    }

    public static float easyExplosionDamage(float rawDamage) {
        if (rawDamage <= 0.0f) {
            return 0.0f;
        }
        return Math.min(rawDamage, rawDamage * 0.5f + 1.0f);
    }
}
