package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;

/**
 * Chicken mob - passive, slow falling, drops feathers and chicken.
 */
public class Chicken extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.CHICKEN;

    // Slow falling
    private static final float CHICKEN_GRAVITY = -10.0f; // Much slower than normal

    // Egg laying
    private int eggTimer;
    private static final int EGG_INTERVAL = 6000; // 5 minutes

    public Chicken() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.CHICKEN;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();
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
        dropItems(ItemType.FEATHER, 0, 2);
        dropItems(isOnFire() ? ItemType.COOKED_CHICKEN : ItemType.RAW_CHICKEN, 1, 1);
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
