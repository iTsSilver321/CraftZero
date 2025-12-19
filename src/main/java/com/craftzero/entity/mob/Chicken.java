package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.world.BlockType;

/**
 * Chicken mob - passive, slow falling, drops feathers and chicken.
 */
public class Chicken extends Mob {

    private static final float WIDTH = 0.4f;
    private static final float HEIGHT = 0.7f;
    private static final float MAX_HEALTH = 4.0f;

    // Slow falling
    private static final float CHICKEN_GRAVITY = -10.0f; // Much slower than normal

    // Egg laying
    private int eggTimer;
    private static final int EGG_INTERVAL = 6000; // 5 minutes

    public Chicken() {
        super(WIDTH, HEIGHT, MAX_HEALTH);
        this.hostile = false;
        this.burnsInSunlight = false;
        this.moveSpeed = 0.12f;
        this.experienceValue = 1;
        this.eggTimer = random.nextInt(EGG_INTERVAL);

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.8f)); // Chickens run fast when scared
        ai.addGoal(7, new WanderGoal(this, ai, 6.0f, 0.8f));
    }

    @Override
    public void tick() {
        super.tick();

        if (dead)
            return;

        // Egg laying timer
        eggTimer++;
        if (eggTimer >= EGG_INTERVAL) {
            eggTimer = 0;
            layEgg();
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null)
            return;

        // Slow falling instead of normal gravity
        if (!onGround) {
            motionY += CHICKEN_GRAVITY * deltaTime;
            // Cap fall speed
            if (motionY < -2.0f) {
                motionY = -2.0f;
            }
        }

        // Apply air resistance
        motionX *= AIR_RESISTANCE;
        motionZ *= AIR_RESISTANCE;

        // Move with collision
        moveWithCollision(motionX * deltaTime, motionY * deltaTime, motionZ * deltaTime);

        // Ground friction
        if (onGround) {
            motionX *= GROUND_FRICTION;
            motionZ *= GROUND_FRICTION;
        }

        // Track distance walked
        float dx = x - prevX;
        float dz = z - prevZ;
        float horizontalDist = (float) Math.sqrt(dx * dx + dz * dz);
        distanceWalked += horizontalDist;

        updateInWater();
    }

    private void layEgg() {
        // TODO: Spawn egg item
        // For now, just a placeholder
    }

    @Override
    public void dropLoot() {
        // Drop 1-2 feathers
        dropItems(BlockType.DIRT, 0, 2); // Placeholder for FEATHER
        // Drop 1 raw chicken
        dropItems(BlockType.DIRT, 1, 1); // Placeholder for RAW_CHICKEN
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/chicken.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.CHICKEN;
    }
}
