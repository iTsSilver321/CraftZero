package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.WorldSoundEvent;

/**
 * Pig mob - passive, drops porkchop.
 */
public class Pig extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.PIG;
    private boolean saddled;
    private boolean playerPassenger;

    public Pig() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.PIG;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        ai.addGoal(1, new PanicGoal(this, ai, 1.5f)); // Run when hurt
        ai.addGoal(7, new WanderGoal(this, ai, 8.0f, 0.6f));
    }

    @Override
    public void dropLoot() {
        dropItems(isOnFire() ? ItemType.COOKED_PORKCHOP : ItemType.RAW_PORKCHOP, 0, 2);
    }

    @Override
    protected String getAmbientSoundId() {
        return WorldSoundEvent.PIG_IDLE;
    }

    @Override
    protected void onHurt(float amount, Entity source) {
        super.onHurt(amount, source);
        playMobHurtSound(WorldSoundEvent.PIG_HURT);
    }

    @Override
    public boolean damage(float amount, DamageSource source) {
        boolean applied = super.damage(amount, source);
        if (applied && playerPassenger && source != null && source.type() == DamageSource.Type.FALL
                && world != null && world.getPlayer() != null) {
            world.getPlayer().getStats().getAchievements().recordPigFlew();
        }
        return applied;
    }

    @Override
    protected void onDeath() {
        playMobDeathSound(WorldSoundEvent.PIG_DEATH);
        super.onDeath();
    }

    @Override
    protected boolean isBreedingItem(ItemType itemType) {
        return itemType == ItemType.WHEAT;
    }

    @Override
    protected boolean isBreedingCompatible(Mob mate) {
        return mate instanceof Pig;
    }

    @Override
    protected Mob createBreedingChild(Mob mate) {
        return new Pig();
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/pig.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }

    public boolean isSaddled() {
        return saddled;
    }

    public void setSaddled(boolean saddled) {
        this.saddled = saddled;
    }

    public boolean saddle() {
        if (removed || saddled) {
            return false;
        }
        saddled = true;
        return true;
    }

    public boolean mountPlayer() {
        if (!saddled || playerPassenger || removed) {
            return false;
        }
        playerPassenger = true;
        return true;
    }

    public void dismountPlayer() {
        playerPassenger = false;
    }

    public boolean hasPlayerPassenger() {
        return playerPassenger;
    }

    @Override
    public void remove() {
        dismountPlayer();
        super.remove();
    }
}
