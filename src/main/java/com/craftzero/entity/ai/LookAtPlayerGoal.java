package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;
import com.craftzero.main.Player;

/**
 * AI Goal: Look at nearby player.
 * Low priority - just for visual interest when idle.
 * Makes passive mobs feel more alive and aware.
 */
public class LookAtPlayerGoal implements Goal {

    private final LivingEntity mob;
    private final float range;
    private float lookDuration;
    private float lookX, lookY, lookZ;
    private boolean hasTarget;

    private static final float MAX_LOOK_TIME = 40; // 2 seconds

    public LookAtPlayerGoal(LivingEntity mob, float range) {
        this.mob = mob;
        this.range = range;
        this.hasTarget = false;
    }

    @Override
    public int getPriority() {
        return 8; // Very low priority - only when idle
    }

    @Override
    public boolean canUse() {
        if (mob.getWorld() == null)
            return false;

        Player player = mob.getWorld().getPlayer();
        if (player == null)
            return false;

        // Check if player is in range
        float dist = distanceToPlayer(player);
        if (dist > range)
            return false;

        // Random chance to start looking (not constant staring)
        if (Math.random() > 0.1f)
            return false;

        // Set look target
        lookX = player.getPosition().x;
        lookY = player.getPosition().y + 1.6f; // Eye level
        lookZ = player.getPosition().z;
        lookDuration = MAX_LOOK_TIME;
        hasTarget = true;

        return true;
    }

    @Override
    public boolean canContinue() {
        if (!hasTarget)
            return false;
        if (lookDuration <= 0)
            return false;

        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        if (player == null)
            return false;

        // Stop if player moved too far
        float dist = distanceToPlayer(player);
        return dist <= range * 1.5f;
    }

    @Override
    public void start() {
        // Already set in canUse
    }

    @Override
    public void tick() {
        lookDuration--;

        Player player = mob.getWorld() != null ? mob.getWorld().getPlayer() : null;
        if (player != null) {
            // Update look position (player might move)
            lookX = player.getPosition().x;
            lookY = player.getPosition().y + 1.6f;
            lookZ = player.getPosition().z;
        }

        // Look at player (just head rotation, not body)
        mob.lookAt(lookX, lookY, lookZ);
    }

    @Override
    public void stop() {
        hasTarget = false;
    }

    private float distanceToPlayer(Player player) {
        float dx = player.getPosition().x - mob.getX();
        float dy = player.getPosition().y - mob.getY();
        float dz = player.getPosition().z - mob.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean isExclusive() {
        return false; // Looking doesn't prevent other actions
    }
}
