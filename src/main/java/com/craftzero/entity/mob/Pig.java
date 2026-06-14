package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;

/**
 * Pig mob - passive, drops porkchop.
 */
public class Pig extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.PIG;

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
        dropItems(isOnFire() ? ItemType.COOKED_PORKCHOP : ItemType.RAW_PORKCHOP, 1, 3);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/pig.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.QUADRUPED;
    }
}
