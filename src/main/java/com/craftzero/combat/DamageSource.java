package com.craftzero.combat;

import com.craftzero.entity.Entity;

/**
 * Shared source metadata for damage, knockback, and invulnerability comparisons.
 */
public record DamageSource(Type type, Entity entity, boolean hasPosition, float x, float y, float z,
        float horizontalKnockback, float verticalKnockback, int lootingLevel, boolean playerCredit,
        String playerId) {
    public DamageSource {
        type = type == null ? Type.GENERIC : type;
        if (!hasPosition || !allFinite(x, y, z)) {
            hasPosition = false;
            x = 0.0f;
            y = 0.0f;
            z = 0.0f;
        }
        horizontalKnockback = Math.max(0.0f, finiteOrZero(horizontalKnockback));
        verticalKnockback = Math.max(0.0f, finiteOrZero(verticalKnockback));
        lootingLevel = Math.max(0, lootingLevel);
        playerId = playerId == null ? "" : playerId.trim();
    }

    public DamageSource(Type type, Entity entity, boolean hasPosition, float x, float y, float z,
            float horizontalKnockback, float verticalKnockback, int lootingLevel, boolean playerCredit) {
        this(type, entity, hasPosition, x, y, z, horizontalKnockback, verticalKnockback,
                lootingLevel, playerCredit, "");
    }

    public enum Type {
        GENERIC,
        PLAYER_ATTACK,
        MOB_MELEE,
        ARROW,
        EXPLOSION,
        MAGIC,
        FIRE,
        LIGHTNING,
        DROWN,
        SUFFOCATION,
        FALL
    }

    public static DamageSource generic() {
        return new DamageSource(Type.GENERIC, null, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, false);
    }

    public static DamageSource magic() {
        return new DamageSource(Type.MAGIC, null, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, false);
    }

    public static DamageSource suffocation(float x, float y, float z) {
        return new DamageSource(Type.SUFFOCATION, null, true, x, y, z, 0.0f, 0.0f, 0, false);
    }

    public static DamageSource entity(Type type, Entity entity) {
        return entity(type, entity, 0.0f, 0.0f);
    }

    public static DamageSource entity(Type type, Entity entity, float horizontalKnockback, float verticalKnockback) {
        return new DamageSource(type == null ? Type.GENERIC : type, entity, entity != null,
                entity != null ? entity.getX() : 0.0f,
                entity != null ? entity.getY() : 0.0f,
                entity != null ? entity.getZ() : 0.0f,
                horizontalKnockback, verticalKnockback, 0, false);
    }

    public static DamageSource thrownProjectile(Entity projectile, boolean playerCredit) {
        return new DamageSource(Type.GENERIC, projectile, projectile != null,
                projectile != null ? projectile.getX() : 0.0f,
                projectile != null ? projectile.getY() : 0.0f,
                projectile != null ? projectile.getZ() : 0.0f,
                0.0f, 0.0f, 0, playerCredit);
    }

    public static DamageSource point(Type type, float x, float y, float z,
            float horizontalKnockback, float verticalKnockback) {
        return new DamageSource(type == null ? Type.GENERIC : type, null, true, x, y, z,
                horizontalKnockback, verticalKnockback, 0, false);
    }

    public static DamageSource playerAttack(float x, float y, float z, int lootingLevel) {
        return new DamageSource(Type.PLAYER_ATTACK, null, true, x, y, z,
                0.0f, 0.0f, Math.max(0, lootingLevel), true);
    }

    public static DamageSource remotePlayerAttack(String playerId, float x, float y, float z, int lootingLevel) {
        return new DamageSource(Type.PLAYER_ATTACK, null, true, x, y, z,
                0.0f, 0.0f, Math.max(0, lootingLevel), true, playerId);
    }

    public DamageSource withPlayerId(String playerId) {
        return new DamageSource(type, entity, hasPosition, x, y, z,
                horizontalKnockback, verticalKnockback, lootingLevel, playerCredit, playerId);
    }

    public DamageSource withPlayerCredit(boolean playerCredit) {
        return new DamageSource(type, entity, hasPosition, x, y, z,
                horizontalKnockback, verticalKnockback, lootingLevel, playerCredit, playerId);
    }

    public float sourceX() {
        return finiteOrZero(entity != null ? entity.getX() : x);
    }

    public float sourceY() {
        return finiteOrZero(entity != null ? entity.getY() : y);
    }

    public float sourceZ() {
        return finiteOrZero(entity != null ? entity.getZ() : z);
    }

    public boolean hasKnockback() {
        return horizontalKnockback > 0.0f || verticalKnockback > 0.0f;
    }

    public boolean bypassesArmor() {
        return type == Type.MAGIC || type == Type.SUFFOCATION;
    }

    public boolean isUnblockable() {
        return switch (type) {
            case MAGIC, SUFFOCATION, DROWN, FALL -> true;
            default -> false;
        };
    }

    public boolean canBeBlockedBySword() {
        return !isUnblockable();
    }

    public boolean scalesWithDifficulty() {
        return switch (type) {
            case MAGIC, FALL, SUFFOCATION -> false;
            default -> true;
        };
    }

    public boolean usesHalfHurtResistanceWindow() {
        return type == Type.SUFFOCATION;
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }
}
