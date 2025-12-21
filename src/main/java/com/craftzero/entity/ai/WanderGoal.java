package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;

import java.util.Random;

/**
 * AI Goal: Wander randomly within a radius.
 * Picks safe positions that won't lead off cliffs.
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
    private int stuckTime;
    private float lastX, lastZ; // For stuck detection

    private static final int MIN_COOLDOWN = 100; // 5 seconds
    private static final int MAX_COOLDOWN = 200; // 10 seconds
    private static final int MAX_TARGET_ATTEMPTS = 5; // How many times to try picking safe target

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
        if (ai.hasMoveTarget()) {
            return false;
        }

        // Check cooldown
        if (wanderCooldown > 0) {
            wanderCooldown--;
            return false;
        }

        // Random chance to start wandering
        if (random.nextFloat() < 0.02f) { // 2% chance per tick
            if (pickSafeTarget()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinue() {
        if (ai.hasMoveTarget()) {
            return false; // Higher priority goal took over
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
        stuckTime = 0;
        lastX = mob.getX();
        lastZ = mob.getZ();
    }

    @Override
    public void tick() {
        if (!hasTarget)
            return;

        // Calculate direction to target
        float dx = targetX - mob.getX();
        float dz = targetZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist <= 0.5f) {
            mob.stopMoving();
            return;
        }

        float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));

        // Check for cliff ahead
        if (LineOfSightUtil.isCliffAhead(mob.getWorld(),
                mob.getX(), mob.getY(), mob.getZ(), targetYaw, 1.5f)) {
            // Path leads to cliff - find safe direction or pick new target
            float safeYaw = LineOfSightUtil.findSafeDirection(
                    mob.getWorld(), mob.getX(), mob.getY(), mob.getZ(), targetYaw);

            if (safeYaw == targetYaw) {
                // No safe direction found - pick entirely new target
                if (!pickSafeTarget()) {
                    mob.stopMoving();
                    hasTarget = false;
                }
                return;
            }
            targetYaw = safeYaw;
        }

        // Check if stuck (not moving)
        float moveDist = (float) Math.sqrt(
                (mob.getX() - lastX) * (mob.getX() - lastX) +
                        (mob.getZ() - lastZ) * (mob.getZ() - lastZ));

        if (moveDist < 0.03f) {
            stuckTime++;
            if (stuckTime > 30) { // Stuck for 1.5 seconds
                // Pick new target
                if (!pickSafeTarget()) {
                    mob.stopMoving();
                    hasTarget = false;
                }
                stuckTime = 0;
                return;
            }
        } else {
            stuckTime = 0;
            lastX = mob.getX();
            lastZ = mob.getZ();
        }

        mob.setMoveDirection(targetYaw, speed);
    }

    @Override
    public void stop() {
        hasTarget = false;
        wanderCooldown = MIN_COOLDOWN + random.nextInt(MAX_COOLDOWN - MIN_COOLDOWN);
        mob.stopMoving();
    }

    /**
     * Pick a safe target position (not leading off a cliff).
     */
    private boolean pickSafeTarget() {
        for (int attempt = 0; attempt < MAX_TARGET_ATTEMPTS; attempt++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = random.nextFloat() * radius + 2.0f;

            float testX = mob.getX() + (float) Math.cos(angle) * distance;
            float testZ = mob.getZ() + (float) Math.sin(angle) * distance;

            // Check if destination is safe
            if (mob.getWorld() != null &&
                    LineOfSightUtil.isPositionSafe(mob.getWorld(), testX, mob.getY(), testZ, 3)) {

                // Also check path doesn't immediately lead off cliff
                float testYaw = (float) Math.toDegrees(Math.atan2(
                        testX - mob.getX(), -(testZ - mob.getZ())));

                if (!LineOfSightUtil.isCliffAhead(mob.getWorld(),
                        mob.getX(), mob.getY(), mob.getZ(), testYaw, 2.0f)) {
                    targetX = testX;
                    targetZ = testZ;
                    hasTarget = true;
                    return true;
                }
            }
        }

        return false; // Couldn't find safe target
    }
}
