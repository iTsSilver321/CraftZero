package com.craftzero.entity.ai;

import com.craftzero.entity.LivingEntity;

import java.util.Random;

/**
 * AI Goal: Panic and run when damaged.
 * Used by passive mobs to flee when hurt.
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
            pickFleeDirection();
            panicking = true;
            panicTime = PANIC_DURATION;
            return true;
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
            // Calculate target yaw and use new movement system
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            mob.setMoveDirection(targetYaw, panicSpeed);
        } else {
            // Reached destination, pick new one
            pickFleeDirection();
        }

        // If hit again, extend panic time
        if (mob.getHurtTime() > 0 && mob.getHurtTime() == 10) {
            panicTime = PANIC_DURATION;
            pickFleeDirection();
        }
    }

    @Override
    public void stop() {
        panicking = false;
        mob.stopMoving();
    }

    private void pickFleeDirection() {
        // Pick random direction away from damage source
        float angle;

        if (mob.getLastDamageSource() != null) {
            // Run away from damage source
            float dx = mob.getX() - mob.getLastDamageSource().getX();
            float dz = mob.getZ() - mob.getLastDamageSource().getZ();
            angle = (float) Math.atan2(dz, dx);
            // Add some randomness
            angle += (random.nextFloat() - 0.5f) * Math.PI * 0.5f;
        } else {
            // Random direction
            angle = random.nextFloat() * (float) Math.PI * 2;
        }

        fleeX = mob.getX() + (float) Math.cos(angle) * FLEE_RADIUS;
        fleeZ = mob.getZ() + (float) Math.sin(angle) * FLEE_RADIUS;
    }
}
