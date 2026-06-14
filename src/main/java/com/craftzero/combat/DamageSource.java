package com.craftzero.combat;

import com.craftzero.entity.Entity;

/**
 * Shared source metadata for damage, knockback, and invulnerability comparisons.
 */
public record DamageSource(Type type, Entity entity, boolean hasPosition, float x, float y, float z,
        float horizontalKnockback, float verticalKnockback) {

    public enum Type {
        GENERIC,
        PLAYER_ATTACK,
        MOB_MELEE,
        ARROW,
        EXPLOSION,
        FIRE
    }

    public static DamageSource generic() {
        return new DamageSource(Type.GENERIC, null, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static DamageSource entity(Type type, Entity entity) {
        return entity(type, entity, 0.0f, 0.0f);
    }

    public static DamageSource entity(Type type, Entity entity, float horizontalKnockback, float verticalKnockback) {
        return new DamageSource(type == null ? Type.GENERIC : type, entity, entity != null,
                entity != null ? entity.getX() : 0.0f,
                entity != null ? entity.getY() : 0.0f,
                entity != null ? entity.getZ() : 0.0f,
                horizontalKnockback, verticalKnockback);
    }

    public static DamageSource point(Type type, float x, float y, float z,
            float horizontalKnockback, float verticalKnockback) {
        return new DamageSource(type == null ? Type.GENERIC : type, null, true, x, y, z,
                horizontalKnockback, verticalKnockback);
    }

    public float sourceX() {
        return entity != null ? entity.getX() : x;
    }

    public float sourceY() {
        return entity != null ? entity.getY() : y;
    }

    public float sourceZ() {
        return entity != null ? entity.getZ() : z;
    }

    public boolean hasKnockback() {
        return horizontalKnockback > 0.0f || verticalKnockback > 0.0f;
    }
}
