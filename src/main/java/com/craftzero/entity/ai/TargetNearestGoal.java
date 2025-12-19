package com.craftzero.entity.ai;

import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;

/**
 * AI Goal: Target the nearest player within range.
 */
public class TargetNearestGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float range;
    private final boolean requireSight; // TODO: implement line of sight check

    private int checkCooldown;
    private static final int CHECK_INTERVAL = 10; // Check every 0.5 seconds

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range) {
        this(mob, ai, range, false);
    }

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range, boolean requireSight) {
        this.mob = mob;
        this.ai = ai;
        this.range = range;
        this.requireSight = requireSight;
        this.checkCooldown = 0;
    }

    @Override
    public int getPriority() {
        return 2; // High priority
    }

    @Override
    public boolean canUse() {
        if (checkCooldown > 0) {
            checkCooldown--;
            return ai.hasTarget(); // Keep current target if we have one
        }

        checkCooldown = CHECK_INTERVAL;
        return findTarget();
    }

    @Override
    public boolean canContinue() {
        LivingEntity target = ai.getTarget();

        if (target == null || target.isDead() || target.isRemoved()) {
            return false;
        }

        // Check if target is still in range (with some buffer)
        float dist = mob.distanceTo(target);
        return dist <= range * 1.5f;
    }

    @Override
    public void start() {
        // Target already set in canUse
    }

    @Override
    public void tick() {
        LivingEntity target = ai.getTarget();
        if (target != null) {
            // Look at target
            mob.lookAt(target.getX(), target.getY() + target.getHeight() * 0.85f, target.getZ());
        }
    }

    @Override
    public void stop() {
        ai.clearTarget();
    }

    /**
     * Find the nearest valid target.
     */
    private boolean findTarget() {
        if (mob.getWorld() == null)
            return false;

        // For now, we'll need to get the player from the world
        // This will be properly implemented when we add entity lists to World
        Player player = mob.getWorld().getPlayer();
        if (player == null)
            return false;

        // Check if player is in range
        float dist = distanceToPlayer(player);
        if (dist > range)
            return false;

        // Check if player is alive
        if (player.getStats().getHealth() <= 0)
            return false;

        // Set as target (we need to handle Player differently since it's not a
        // LivingEntity yet)
        // For now, we'll store the target position
        ai.setMoveTarget(player.getPosition().x, player.getPosition().z);

        return true;
    }

    private float distanceToPlayer(Player player) {
        float dx = player.getPosition().x - mob.getX();
        float dy = player.getPosition().y - mob.getY();
        float dz = player.getPosition().z - mob.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean isExclusive() {
        return false; // Targeting doesn't prevent other goals
    }
}
