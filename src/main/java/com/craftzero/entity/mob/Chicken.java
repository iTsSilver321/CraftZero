package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;

/**
 * Chicken mob - passive, slow falling, drops feathers and chicken.
 */
public class Chicken extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.CHICKEN;

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
        super.updatePhysics(deltaTime);
        if (!onGround && motionY < -0.6f) {
            motionY = -0.6f;
        }
    }

    @Override
    protected float getGravityPerTick() {
        return 0.03f;
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
