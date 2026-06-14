package com.craftzero.entity.mob;

import com.craftzero.entity.ai.*;
import com.craftzero.inventory.ItemType;

/**
 * Skeleton mob - hostile, ranged attack, burns in sunlight.
 */
public class Skeleton extends Mob {

    private static final MobBalance.Spec SPEC = MobBalance.SKELETON;

    public Skeleton() {
        super(SPEC.width(), SPEC.height(), SPEC.maxHealth());
        this.definition = MobDefinition.SKELETON;
        this.hostile = SPEC.hostile();
        this.burnsInSunlight = SPEC.burnsInSunlight();
        this.moveSpeed = SPEC.moveSpeed();
        this.experienceValue = SPEC.experienceValue();

        setupAI();
    }

    private void setupAI() {
        // Skeletons prefer ranged combat - stay at distance
        ai.addGoal(2, new TargetNearestGoal(this, ai, 16.0f));
        ai.addGoal(3, new RangedAttackGoal(this, ai, 16.0f, 40));
        ai.addGoal(7, new WanderGoal(this, ai, 10.0f, 0.8f));
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.BONE, 0, 2);
        dropItems(ItemType.ARROW, 0, 2);
    }

    @Override
    public String getTexturePath() {
        return "/textures/mob/skeleton.png";
    }

    @Override
    public MobModelType getModelType() {
        return MobModelType.SKELETON;
    }
}
