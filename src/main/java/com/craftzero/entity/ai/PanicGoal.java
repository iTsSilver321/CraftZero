package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;

import java.util.Random;

/**
 * AI Goal: Panic and run when damaged.
 * Used by passive mobs to flee when hurt.
 * Includes cliff-awareness to avoid running off edges.
 */
public class PanicGoal implements Goal {

    private final LivingEntity mob;
    private final MobAI ai;
    private final float panicSpeed;
    private final Random random;

    private float fleeX, fleeZ;
    private int panicTime;
    private boolean panicking;

    private static final int PANIC_DURATION = 100; // 5 seconds
    private static final float FLEE_RADIUS = 10.0f;
    private static final int MAX_FLEE_ATTEMPTS = 8;

    public PanicGoal(LivingEntity mob, MobAI ai, float panicSpeed) {
        this.mob = mob;
        this.ai = ai;
        this.panicSpeed = panicSpeed;
        this.random = new Random();
        this.panicking = false;
    }

    @Override
    public int getPriority() {
        return 1; // Highest priority - panic overrides everything
    }

    @Override
    public boolean canUse() {
        // Start panicking if just hurt
        if (mob.getHurtTime() > 0 && mob.getHurtTime() == 10) { // Just got hit
            if (pickSafeFleeDirection()) {
                panicking = true;
                panicTime = PANIC_DURATION;
                return true;
            }
        }
        return panicking;
    }

    @Override
    public boolean canContinue() {
        return panicTime > 0;
    }

    @Override
    public void start() {
        // Already set up in canUse
    }

    @Override
    public void tick() {
        panicTime--;

        // Move toward flee position
        float dx = fleeX - mob.getX();
        float dz = fleeZ - mob.getZ();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (dist > 1.0f) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));

            // Check for cliff ahead
            if (LineOfSightUtil.isCliffAhead(mob.getWorld(),
                    mob.getX(), mob.getY(), mob.getZ(), targetYaw, 1.5f)) {
                // Find safe direction
                float safeYaw = LineOfSightUtil.findSafeDirection(
                        mob.getWorld(), mob.getX(), mob.getY(), mob.getZ(), targetYaw);

                if (safeYaw != targetYaw) {
                    targetYaw = safeYaw;
                } else {
                    // No safe direction - pick new flee destination
                    pickSafeFleeDirection();
                    return;
                }
            }

            mob.setMoveDirection(targetYaw, panicSpeed);
        } else {
            // Reached destination, pick new one
            pickSafeFleeDirection();
        }

        // If hit again, extend panic time
        if (mob.getHurtTime() > 0 && mob.getHurtTime() == 10) {
            panicTime = PANIC_DURATION;
            pickSafeFleeDirection();
        }
    }

    @Override
    public void stop() {
        panicking = false;
        mob.stopMoving();
    }

    /**
     * Pick a safe flee direction that doesn't lead off a cliff.
     */
    private boolean pickSafeFleeDirection() {
        float baseAngle;

        if (mob.getLastDamageSource() != null) {
            // Run away from damage source
            float dx = mob.getX() - mob.getLastDamageSource().getX();
            float dz = mob.getZ() - mob.getLastDamageSource().getZ();
            baseAngle = (float) Math.atan2(dz, dx);
        } else {
            // Random direction
            baseAngle = random.nextFloat() * (float) Math.PI * 2;
        }

        // Try multiple angles to find a safe one
        for (int attempt = 0; attempt < MAX_FLEE_ATTEMPTS; attempt++) {
            float angle = baseAngle + (random.nextFloat() - 0.5f) * (float) Math.PI * 0.5f;
            // Each attempt, widen the search cone
            if (attempt > 2) {
                angle = random.nextFloat() * (float) Math.PI * 2;
            }

            float testX = mob.getX() + (float) Math.cos(angle) * FLEE_RADIUS;
            float testZ = mob.getZ() + (float) Math.sin(angle) * FLEE_RADIUS;

            // Check if destination is safe
            if (mob.getWorld() != null &&
                    LineOfSightUtil.isPositionSafe(mob.getWorld(), testX, mob.getY(), testZ, 3)) {

                // Check path is safe
                float testYaw = (float) Math.toDegrees(Math.atan2(
                        testX - mob.getX(), -(testZ - mob.getZ())));

                if (!LineOfSightUtil.isCliffAhead(mob.getWorld(),
                        mob.getX(), mob.getY(), mob.getZ(), testYaw, 2.0f)) {
                    fleeX = testX;
                    fleeZ = testZ;
                    return true;
                }
            }
        }

        // Fallback: just run in a random direction, better than staying still
        float angle = random.nextFloat() * (float) Math.PI * 2;
        fleeX = mob.getX() + (float) Math.cos(angle) * 4.0f;
        fleeZ = mob.getZ() + (float) Math.sin(angle) * 4.0f;
        return true;
    }
}
