package com.craftzero.entity.mob;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldSoundEvent;

/**
 * Chicken mob - passive, slow falling, drops feathers and chicken.
 */
public class Chicken extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.CHICKEN;

    // Egg laying
    private int eggTimer;
    private static final int MIN_EGG_INTERVAL = 6000; // 5 minutes
    private static final int EGG_INTERVAL_VARIANCE = 6000;

    public Chicken() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.CHICKEN;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();
        resetEggTimer();

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

        if (isBaby()) {
            return;
        }

        // Egg laying timer
        eggTimer--;
        if (eggTimer <= 0) {
            layEgg();
            resetEggTimer();
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        super.updatePhysics(deltaTime);
        if (!onGround && motionY < 0.0f) {
            motionY *= 0.6f;
        }
    }

    @Override
    protected float getGravityPerTick() {
        return 0.03f;
    }

    @Override
    protected boolean isFallDamageImmune() {
        return true;
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.CHICKEN_IDLE;
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.CHICKEN_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.CHICKEN_DEATH);
        super.onDeath();
    }

    private void layEgg() {
        if (world != null) {
            world.playSound(WorldSoundEvent.CHICKEN_PLOP, x, y, z, 1.0f, WorldSoundEvent.chickenPlopPitch(random));
            world.spawnDroppedItem(x, y + 0.5f, z, ItemType.EGG, 1);
        }
    }

    private void resetEggTimer() {
        eggTimer = MIN_EGG_INTERVAL + random.nextInt(EGG_INTERVAL_VARIANCE);
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.FEATHER, 0, 2);
        dropItem(isOnFire() ? ItemType.COOKED_CHICKEN : ItemType.RAW_CHICKEN, 1);
    }

    @Override
    protected boolean isBreedingItem(ItemType itemType) {
        return itemType == ItemType.WHEAT;
    }

    @Override
    protected boolean isBreedingCompatible(Mob mate) {
        return mate instanceof Chicken;
    }

    @Override
    protected Mob createBreedingChild(Mob mate) {
        return new Chicken();
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/chicken.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.CHICKEN;
    }

    public int getEggTimer() {
        return eggTimer;
    }

    public void setEggTimer(int eggTimer) {
        this.eggTimer = Math.max(0, eggTimer);
    }
}
