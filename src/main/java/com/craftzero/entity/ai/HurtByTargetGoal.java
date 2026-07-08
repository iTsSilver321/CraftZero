package com.craftzero.entity.ai;

import com.craftzero.combat.CombatTargetResolver;
import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.world.World;

/**
 * AI Goal: Target the entity that hurt us (revenge).
 * Used by neutral mobs (wolves, zombie pigmen) to become hostile when attacked.
 */
public class HurtByTargetGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private LivingEntity attacker;
    private String remoteAttackerId = "";
    private int revengeTimer;

    private static final int REVENGE_DURATION = 200; // 10 seconds of aggression

    public HurtByTargetGoal(LivingEntity mob, MobAI ai) {
        this.mob = mob;
        this.ai = ai;
        this.revengeTimer = 0;
    }

    @Override
    public int getPriority() {
        return 1; // High priority - revenge is important
    }

    @Override
    public boolean canUse() {
        // Check if we were just hurt
        if (mob.getHurtTime() == 10) { // Just got hit (hurt time starts at 10)
            DamageSource source = mob.getLastDamageDetails();
            String remotePlayerId = CombatTargetResolver.remotePlayerId(source);
            if (!remotePlayerId.isBlank() && remoteTargetById(remotePlayerId) != null) {
                attacker = null;
                remoteAttackerId = remotePlayerId;
                revengeTimer = REVENGE_DURATION;
                return true;
            }

            Entity sourceEntity = mob.getLastDamageSource();
            LivingEntity livingSource = source != null
                    ? CombatTargetResolver.validLivingAttacker(source, mob)
                    : CombatTargetResolver.validLivingAttacker(sourceEntity, mob);
            if (livingSource != null) {
                attacker = livingSource;
                remoteAttackerId = "";
                revengeTimer = REVENGE_DURATION;
                return true;
            }
        }

        // Continue if we already have a revenge target
        return revengeTimer > 0
                && ((attacker != null && !attacker.isDead() && !attacker.isRemoved())
                        || currentRemoteAttacker() != null);
    }

    @Override
    public boolean canContinue() {
        if (revengeTimer <= 0)
            return false;
        World.RemotePlayerTarget remoteTarget = currentRemoteAttacker();
        if (remoteTarget != null) {
            return distanceTo(remoteTarget.x(), remoteTarget.y(), remoteTarget.z()) < 32.0f;
        }
        if (attacker == null || attacker.isDead() || attacker.isRemoved())
            return false;

        // Check distance - give up if too far
        float dist = mob.distanceTo(attacker);
        return dist < 32.0f;
    }

    @Override
    public void start() {
        // Set the attacker as our target
        World.RemotePlayerTarget remoteTarget = currentRemoteAttacker();
        if (remoteTarget != null) {
            ai.setRemotePlayerTarget(remoteTarget);
            ai.setMoveTarget(remoteTarget.x(), remoteTarget.y(), remoteTarget.z());
        } else if (attacker != null) {
            ai.setTarget(attacker);
            ai.setMoveTarget(attacker.getX(), attacker.getY(), attacker.getZ());
        }
    }

    @Override
    public void tick() {
        revengeTimer--;

        World.RemotePlayerTarget remoteTarget = currentRemoteAttacker();
        if (remoteTarget != null) {
            ai.setRemotePlayerTarget(remoteTarget);
            ai.setMoveTarget(remoteTarget.x(), remoteTarget.y(), remoteTarget.z());
            mob.lookAt(remoteTarget.x(), remoteTarget.eyeY(), remoteTarget.z());
        } else if (attacker != null && !attacker.isDead() && !attacker.isRemoved()) {
            // Keep updating target position
            ai.setMoveTarget(attacker.getX(), attacker.getY(), attacker.getZ());

            // Look at attacker
            mob.lookAt(attacker.getX(), attacker.getY() + attacker.getHeight() * 0.85f, attacker.getZ());
        }
    }

    @Override
    public void stop() {
        attacker = null;
        remoteAttackerId = "";
        revengeTimer = 0;
        ai.clearTarget();
        ai.clearMoveTarget();
    }

    private World.RemotePlayerTarget currentRemoteAttacker() {
        if (remoteAttackerId == null || remoteAttackerId.isBlank()) {
            return null;
        }
        return remoteTargetById(remoteAttackerId);
    }

    private World.RemotePlayerTarget remoteTargetById(String playerId) {
        World world = mob.getWorld();
        if (world == null || playerId == null || playerId.isBlank()) {
            return null;
        }
        World.RemotePlayerTarget target = world.remotePlayerTargetById(playerId);
        return target != null && target.valid() ? target : null;
    }

    private float distanceTo(float targetX, float targetY, float targetZ) {
        float dx = targetX - mob.getX();
        float dy = targetY - mob.getY();
        float dz = targetZ - mob.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean isExclusive() {
        return true; // Revenge overrides other targeting
    }
}
