package com.craftzero.combat;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ThrownItemEntity;

/**
 * Resolves combat ownership from direct hits and projectile wrappers.
 */
public final class CombatTargetResolver {
    private CombatTargetResolver() {
    }

    public static Entity attackerEntity(DamageSource source) {
        return source == null ? null : attackerEntity(source.entity());
    }

    public static Entity attackerEntity(Entity source) {
        if (source instanceof ArrowEntity arrow) {
            return arrow.getShooter();
        }
        if (source instanceof ThrownItemEntity thrownItem) {
            return thrownItem.getShooter();
        }
        if (source instanceof FireballEntity fireball) {
            return fireball.getShooter();
        }
        return source;
    }

    public static LivingEntity livingAttacker(DamageSource source) {
        return livingAttacker(source == null ? null : source.entity());
    }

    public static LivingEntity livingAttacker(Entity source) {
        Entity attacker = attackerEntity(source);
        return attacker instanceof LivingEntity living ? living : null;
    }

    public static LivingEntity validLivingAttacker(DamageSource source, LivingEntity victim) {
        return validLivingAttacker(source == null ? null : source.entity(), victim);
    }

    public static LivingEntity validLivingAttacker(Entity source, LivingEntity victim) {
        LivingEntity attacker = livingAttacker(source);
        if (attacker == null || attacker == victim || attacker.isDead() || attacker.isRemoved()) {
            return null;
        }
        return attacker;
    }

    public static boolean isPlayerAggression(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.type() == DamageSource.Type.PLAYER_ATTACK
                || source.playerCredit()
                || (source.type() == DamageSource.Type.ARROW
                        && source.entity() instanceof ArrowEntity arrow
                        && arrow.isPlayerOwned());
    }

    public static String remotePlayerId(DamageSource source) {
        if (source == null) {
            return "";
        }
        if (source.playerId() != null && !source.playerId().isBlank()) {
            return source.playerId();
        }
        Entity entity = source.entity();
        if (entity instanceof ArrowEntity arrow && arrow.isPlayerOwned()) {
            return cleanedId(arrow.getRemoteShooterPlayerId());
        }
        if (entity instanceof ThrownItemEntity thrownItem && thrownItem.isPlayerOwned()) {
            return cleanedId(thrownItem.getRemoteShooterPlayerId());
        }
        if (entity instanceof FireballEntity fireball) {
            return cleanedId(fireball.getRemoteDeflectorPlayerId());
        }
        return "";
    }

    private static String cleanedId(String playerId) {
        return playerId == null ? "" : playerId.trim();
    }
}
