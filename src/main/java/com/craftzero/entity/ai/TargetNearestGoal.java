package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;
import java.util.function.BooleanSupplier;

/**
 * AI Goal: Target the nearest player within range.
 * Requires line-of-sight if configured.
 */
public class TargetNearestGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float range;
    private final boolean requireSight;
    private final BooleanSupplier canTargetPredicate;

    private int checkCooldown;
    private int sightLostTicks; // How long we've been without sight
    private int targetRefreshCooldown;
    private static final int CHECK_INTERVAL = 10; // Check every 0.5 seconds
    private static final int SIGHT_MEMORY = 40; // Remember target for 2 seconds without sight

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range) {
        this(mob, ai, range, true); // Default: require sight
    }

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range, boolean requireSight) {
        this(mob, ai, range, requireSight, () -> true);
    }

    public TargetNearestGoal(LivingEntity mob, MobAI ai, float range, boolean requireSight,
            BooleanSupplier canTargetPredicate) {
        this.mob = mob;
        this.ai = ai;
        this.range = range;
        this.requireSight = requireSight;
        this.canTargetPredicate = canTargetPredicate;
        this.checkCooldown = 0;
        this.sightLostTicks = 0;
        this.targetRefreshCooldown = 0;
    }

    @Override
    public int getPriority() {
        return 2; // High priority
    }

    @Override
    public boolean canUse() {
        if (checkCooldown > 0) {
            checkCooldown--;
            return ai.hasMoveTarget(); // Keep current target if we have one
        }

        if (!canTargetPredicate.getAsBoolean()) {
            ai.clearMoveTarget();
            ai.clearTarget();
            return false;
        }

        checkCooldown = CHECK_INTERVAL;
        return findTarget();
    }

    @Override
    public boolean canContinue() {
        if (!ai.hasMoveTarget() || !canTargetPredicate.getAsBoolean()) {
            return false;
        }

        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        if (player == null || player.getStats().getHealth() <= 0 || player.isCreative()
                || !player.getDifficulty().allowsHostileSpawns()) {
            return false;
        }

        // Check distance
        float dist = distanceToPlayer(player);
        if (dist > range * 1.5f) {
            return false; // Too far
        }

        // Check line of sight
        if (requireSight) {
            if (hasLineOfSight(player)) {
                sightLostTicks = 0; // Reset sight timer
            } else {
                sightLostTicks++;
                if (sightLostTicks > SIGHT_MEMORY) {
                    return false; // Lost sight for too long
                }
            }
        }

        // Update target position
        targetRefreshCooldown--;
        if (targetRefreshCooldown <= 0) {
            ai.setMoveTarget(player.getPosition().x, player.getPosition().z);
            targetRefreshCooldown = CHECK_INTERVAL;
        }
        return true;
    }

    @Override
    public void start() {
        sightLostTicks = 0;
        targetRefreshCooldown = 0;
    }

    @Override
    public void tick() {
        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        if (player != null) {
            // Look at player
            mob.lookAt(player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z);
        }
    }

    @Override
    public void stop() {
        ai.clearMoveTarget();
        sightLostTicks = 0;
    }

    /**
     * Find the nearest valid target.
     */
    private boolean findTarget() {
        if (mob.getWorld() == null)
            return false;

        if (!canTargetPredicate.getAsBoolean()) {
            return false;
        }

        Player player = mob.getWorld().getPlayer();
        if (player == null)
            return false;

        // Check if player is in range
        float dist = distanceToPlayer(player);
        if (dist > range)
            return false;

        // Check if player is alive
        if (player.getStats().getHealth() <= 0 || player.isCreative() || !player.getDifficulty().allowsHostileSpawns())
            return false;

        // Check line of sight if required
        if (requireSight && !hasLineOfSight(player)) {
            return false;
        }

        // Set as target
        ai.setMoveTarget(player.getPosition().x, player.getPosition().z);
        return true;
    }

    /**
     * Check if we can see the player (no solid blocks in the way).
     */
    private boolean hasLineOfSight(Player player) {
        if (mob.getWorld() == null)
            return false;

        // Eye position of mob
        float eyeY = mob.getY() + mob.getHeight() * 0.85f;

        // Target position (player eye level)
        float targetY = player.getPosition().y + 1.6f;

        return LineOfSightUtil.hasLineOfSight(
                mob.getWorld(),
                mob.getX(), eyeY, mob.getZ(),
                player.getPosition().x, targetY, player.getPosition().z);
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
