package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;

import java.util.Random;

/**
 * AI Goal: Wander randomly within a radius.
 * The mob picks a random position and walks toward it.
 */
public class WanderGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float radius;
    private final float speed;
    private final Random random;

    private float targetX, targetZ;
    private int wanderCooldown;
    private boolean hasTarget;
    private int stuckTime; // Track how long mob has been stuck at obstacle

    private static final int MIN_COOLDOWN = 100; // 5 seconds
    private static final int MAX_COOLDOWN = 200; // 10 seconds

    public WanderGoal(LivingEntity mob, MobAI ai, float radius, float speed) {
        this.mob = mob;
        this.ai = ai;
        this.radius = radius;
        this.speed = speed;
        this.random = new Random();
        this.wanderCooldown = 0;
        this.hasTarget = false;
    }

    @Override
    public int getPriority() {
        return 7; // Low priority - other goals override this
    }

    @Override
    public boolean canUse() {
        // Don't wander if we have a target to chase
        if (ai.hasTarget()) {
            return false;
        }

        // Check cooldown
        if (wanderCooldown > 0) {
            wanderCooldown--;
            return false;
        }

        // Random chance to start wandering
        if (random.nextFloat() < 0.02f) { // 2% chance per tick
            pickNewTarget();
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinue() {
        if (ai.hasTarget()) {
            return false;
        }

        if (!hasTarget) {
            return false;
        }

        // Check if we've reached the target
        float dx = targetX - mob.getX();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        return dist > 1.0f; // Continue until within 1 block
    }

    @Override
    public void start() {
        // Already picked target in canUse
    }

    @Override
    public void tick() {
        if (!hasTarget)
            return;

        // If mob is trapped or escaping, DON'T interfere with its movement
        if (mob.isTrapped() || mob.isEscaping()) {
            return; // Let LivingEntity handle movement
        }

        // Check if mob hit a wall - but only turn if it's NOT a jumpable obstacle
        // Let auto-jump handle 1-block high walls
        if (mob.isStuck() || (mob.isCollidedHorizontally() && mob.isOnGround())) {
            // Pick a completely new target direction immediately if stuck,
            // or after 15 ticks if just colliding (might be a 1-block jump)
            stuckTime++;

            if (mob.isStuck() || stuckTime > 15) {
                mob.clearTrapped(); // Clear trapped state for fresh start
                pickNewTarget();
                stuckTime = 0;
                return;
            }
        } else {
            stuckTime = 0;
        }

        // Move toward target using the NEW movement system
        float dx = targetX - mob.getX();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.5f) {
            // Calculate target yaw (direction to face)
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));

            // Set movement direction and speed - body will smoothly rotate
            // Speed is 0-1 multiplier (1.0 = full speed)
            mob.setMoveDirection(targetYaw, speed);
        } else {
            // Close to target - stop
            mob.stopMoving();
        }
    }

    @Override
    public void stop() {
        hasTarget = false;
        wanderCooldown = MIN_COOLDOWN + random.nextInt(MAX_COOLDOWN - MIN_COOLDOWN);
        mob.stopMoving();
    }

    private void pickNewTarget() {
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = random.nextFloat() * radius + 2.0f; // At least 2 blocks away

        targetX = mob.getX() + (float) Math.cos(angle) * distance;
        targetZ = mob.getZ() + (float) Math.sin(angle) * distance;
        hasTarget = true;
    }
}
