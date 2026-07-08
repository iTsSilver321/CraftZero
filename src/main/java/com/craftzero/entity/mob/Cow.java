package com.craftzero.entity.mob;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldSoundEvent;

/**
 * Cow mob - passive, drops beef and leather.
 */
public class Cow extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.COW;

    public Cow() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.COW;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.5f));
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.6f));
    }

    @Override
    public void dropLoot() {
        dropItems(isOnFire() ? ItemType.STEAK : ItemType.RAW_BEEF, 1, 3);
        dropItems(ItemType.LEATHER, 0, 2);
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.COW_IDLE;
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.COW_HURT);
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.COW_DEATH);
        super.onDeath();
    }

    @Override
    protected boolean isBreedingItem(ItemType itemType) {
        return itemType == ItemType.WHEAT;
    }

    @Override
    protected boolean isBreedingCompatible(Mob mate) {
        return mate instanceof Cow && mate.getClass() == getClass();
    }

    @Override
    protected Mob createBreedingChild(Mob mate) {
        return new Cow();
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/cow.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }
}
