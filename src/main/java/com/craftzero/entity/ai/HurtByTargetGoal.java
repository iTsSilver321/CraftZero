package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;

/**
 * AI Goal: Target the entity that hurt us (revenge).
 * Used by neutral mobs (wolves, zombie pigmen) to become hostile when attacked.
 */
public class HurtByTargetGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private LivingEntity attacker;
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
            com.craftzero.entity.Entity source = mob.getLastDamageSource();
            if (source instanceof LivingEntity livingSource && !livingSource.isDead()) {
                attacker = livingSource;
                revengeTimer = REVENGE_DURATION;
                return true;
            }
        }

        // Continue if we already have a revenge target
        return revengeTimer > 0 && attacker != null && !attacker.isDead();
    }

    @Override
    public boolean canContinue() {
        if (revengeTimer <= 0)
            return false;
        if (attacker == null || attacker.isDead())
            return false;

        // Check distance - give up if too far
        float dist = mob.distanceTo(attacker);
        return dist < 32.0f;
    }

    @Override
    public void start() {
        // Set the attacker as our target
        if (attacker != null) {
            ai.setTarget(attacker);
            ai.setMoveTarget(attacker.getX(), attacker.getZ());
        }
    }

    @Override
    public void tick() {
        revengeTimer--;

        if (attacker != null && !attacker.isDead()) {
            // Keep updating target position
            ai.setMoveTarget(attacker.getX(), attacker.getZ());

            // Look at attacker
            mob.lookAt(attacker.getX(), attacker.getY() + attacker.getHeight() * 0.85f, attacker.getZ());
        }
    }

    @Override
    public void stop() {
        attacker = null;
        revengeTimer = 0;
        ai.clearTarget();
    }

    @Override
    public boolean isExclusive() {
        return true; // Revenge overrides other targeting
    }
}
